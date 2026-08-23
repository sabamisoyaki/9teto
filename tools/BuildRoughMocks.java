import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * 構図ラフ（docs/improvements/improvements/style.jpg）から 2 コマを切り出し、
 * OVERLAY 配置用のモック立ち絵にする。アプリ本体には含まれない。
 *
 *   java tools\BuildRoughMocks.java           （リポジトリルートで。冪等）
 *   java tools\BuildRoughMocks.java --raw     切り出した生のコマも書き出す
 *                                             （コマの切り出し位置を調整するとき用）
 *
 * やっていること:
 *   1. 指定したコマを切り出す
 *   2. コマの中の**赤いベタ塗り（＝プレイ領域）を検出**し、その矩形が
 *      ゲームの盤面矩形にぴったり重なるよう拡大・平行移動して 1920×1080 へ焼く
 *      → ラフの構図がそのまま画面の構図になる（盤面の位置を手で合わせなくてよい）
 *   3. 紙の白地を落として線画だけを残す（線の濃さ → 透明度、色は蛍光灯色）。
 *      赤ベタ（プレイ領域のマーク）は抜くが、赤で描かれた線は線画として残す
 *
 * 出力: images/mock/mock-rough-a.png / images/mock/mock-rough-b.png
 */
public class BuildRoughMocks {

    private static final File SOURCE = new File("docs/improvements/improvements/style.jpg");
    private static final File IMAGES_DIR = new File("images");
    /** 生成物の出力先。手で置くアセットと混ざらないようサブフォルダへ分けている */
    private static final File OUT_DIR = new File(IMAGES_DIR, "mock");

    /** 出力サイズ = 論理解像度 */
    private static final int OUT_W = 1920;
    private static final int OUT_H = 1080;

    /** 線画の色（KowloonPalette.LIGHT / 病的な蛍光灯色） */
    private static final int LINE_RGB = 0xB7C89A;

    /** 紙の白と見なす明るさ。これ以上は完全に透明にする */
    private static final int PAPER_LUM = 250;
    /** 完全に不透明にする明るさ。これ以下は線の芯 */
    private static final int INK_LUM = 25;
    /** 全体の濃さ。1.0 だとラフが主張しすぎて盤面と competing になる */
    private static final double LINE_OPACITY = 0.85;

    /**
     * 赤ベタと赤の描線を分ける明るさ。ラフでは同じ赤でも、プレイ領域のベタ塗りは
     * 薄く（明るさ 160 前後に集中）、腕や髪を描いた線は濃い（〜150）。
     * ここを境にベタだけ落として線は残す。両方まとめて抜くと構図の真ん中が空になる。
     */
    private static final int RED_FILL_LUM = 150;

    /**
     * 切り出すコマと、そのコマの赤枠を画面上のどこへ置くか。
     *
     * @param name    出力ファイル名
     * @param sx/sy/sw/sh  style.jpg 上のコマ矩形
     * @param fieldX/fieldY/fieldSize  赤枠を重ねる先（＝ゲームの盤面矩形）
     */
    private record Panel(String name, int sx, int sy, int sw, int sh,
                         int fieldX, int fieldY, int fieldSize) {
    }

    private static final Panel[] PANELS = {
        // 逆さ／ダイナミック（SCORE 枠を右下に置いたコマ）
        new Panel("mock-rough-a", 60, 545, 600, 465, 610, 150, 700),
        // 顔アップ（赤枠中央・上下を横線が貫くコマ）
        new Panel("mock-rough-b", 70, 1400, 690, 660, 610, 150, 700),
    };

    public static void main(String[] args) throws IOException {
        boolean writeRaw = args.length > 0 && "--raw".equals(args[0]);
        if (!SOURCE.exists()) {
            System.out.println("Not found: " + SOURCE.getPath());
            return;
        }
        OUT_DIR.mkdirs();
        BufferedImage sheet = ImageIO.read(SOURCE);
        System.out.println("source: " + sheet.getWidth() + "x" + sheet.getHeight());

        for (Panel p : PANELS) {
            BufferedImage crop = sheet.getSubimage(
                    clamp(p.sx, 0, sheet.getWidth() - 1),
                    clamp(p.sy, 0, sheet.getHeight() - 1),
                    clamp(p.sw, 1, sheet.getWidth() - p.sx),
                    clamp(p.sh, 1, sheet.getHeight() - p.sy));

            if (writeRaw) {
                write(crop, p.name + "-raw.png");
            }

            int[] red = findRedBox(crop);
            if (red == null) {
                System.out.println("  [" + p.name + "] 赤枠が見つかりません。切り出し位置を確認してください");
                continue;
            }
            System.out.println("  [" + p.name + "] red box = x" + red[0] + " y" + red[1]
                    + " w" + red[2] + " h" + red[3]);

            write(compose(crop, red, p), p.name + ".png");
        }
    }

    /**
     * 赤枠が盤面矩形へ重なるよう拡大・平行移動して 1920×1080 に焼く。
     * 拡大率は赤枠の長辺基準（縦横比のずれで盤面がはみ出さないよう大きい方に合わせる）。
     */
    private static BufferedImage compose(BufferedImage crop, int[] red, Panel p) {
        double scale = Math.max(p.fieldSize / (double) red[2], p.fieldSize / (double) red[3]);

        BufferedImage out = new BufferedImage(OUT_W, OUT_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // 赤枠の中心を盤面の中心へ合わせる
        double redCx = (red[0] + red[2] / 2.0) * scale;
        double redCy = (red[1] + red[3] / 2.0) * scale;
        double fieldCx = p.fieldX + p.fieldSize / 2.0;
        double fieldCy = p.fieldY + p.fieldSize / 2.0;

        AffineTransform at = new AffineTransform();
        at.translate(fieldCx - redCx, fieldCy - redCy);
        at.scale(scale, scale);
        g.drawImage(toLineArt(crop), at, null);
        g.dispose();
        return out;
    }

    /**
     * 紙の白地を透明に、線を蛍光灯色にする。
     *
     * 赤ベタ（プレイ領域のマーク）は盤面が上に乗るので落とす。ただし落とすのは
     * ベタだけで、同じ赤でも濃い方は腕や髪を描いている線なので線画として残す
     * （{@link #RED_FILL_LUM}）。赤を一律で抜くと構図の真ん中が空白になる。
     * 赤の検出（{@link #findRedBox}）は盤面との位置合わせに使うので別途残してある。
     */
    private static BufferedImage toLineArt(BufferedImage src) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF, gg = (rgb >> 8) & 0xFF, b = rgb & 0xFF;

                int lum = (r * 30 + gg * 59 + b * 11) / 100;
                if (isRed(r, gg, b) && lum >= RED_FILL_LUM) {
                    continue; // 赤ベタ。盤面が乗るので抜く
                }
                double t = clampD((lum - INK_LUM) / (double) (PAPER_LUM - INK_LUM), 0, 1);
                int alpha = (int) (255.0 * (1.0 - t) * LINE_OPACITY);
                if (alpha <= 2) continue;
                out.setRGB(x, y, (alpha << 24) | LINE_RGB);
            }
        }
        return out;
    }

    /** 赤ベタの外接矩形（x, y, w, h）。見つからなければ null */
    private static int[] findRedBox(BufferedImage img) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = -1, maxY = -1;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y);
                if (!isRed((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF)) continue;
                if (x < minX) minX = x;
                if (y < minY) minY = y;
                if (x > maxX) maxX = x;
                if (y > maxY) maxY = y;
            }
        }
        if (maxX < 0) return null;
        return new int[] { minX, minY, maxX - minX + 1, maxY - minY + 1 };
    }

    /** ラフの赤ベタ判定。鉛筆の線やアンチエイリアスを拾わない程度に厳しくする */
    private static boolean isRed(int r, int g, int b) {
        return r > 120 && r - g > 45 && r - b > 45;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static double clampD(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static int write(BufferedImage img, String filename) throws IOException {
        File out = new File(OUT_DIR, filename);
        ImageIO.write(img, "png", out);
        System.out.println("  " + out.getPath());
        return 1;
    }
}
