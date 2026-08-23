import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

import javax.imageio.ImageIO;

/**
 * 白背景の立ち絵から背景だけを抜いて、透過 PNG にする。アプリ本体には含まれない。
 *
 *   java tools\RemoveWhiteBackground.java &lt;入力&gt; &lt;出力&gt;
 *       [--size 1080] [--tolerance 236] [--enclosed 200]
 *
 * 例:
 *   java tools\RemoveWhiteBackground.java raw.png images\character\9F-CLINIC.png
 *
 * なぜ「しきい値で白を全部抜く」ではないのか:
 *   この絵柄のキャラは<b>白衣を着ている</b>。明るい画素をまとめて抜くと白衣や
 *   ハイライトに穴が開く。そこで画像の<b>縁から連結している白だけ</b>を塗りつぶす
 *   （フラッドフィル）。キャラの内側にある白は縁と繋がっていないので残る。
 *
 * やっていること:
 *   1. 四辺の画素を種にして、白っぽい画素だけを辿るフラッドフィルで背景を確定する
 *   2. <b>閉じた背景</b>を拾う。腰に当てた腕と胴の隙間のように、キャラに囲まれていて
 *      縁と繋がらない背景がある。ここは 1 の対象外なので白いまま残ってしまう。
 *      一定サイズ以上の「閉じた白」を背景に足す（{@code --enclosed} でサイズを指定）。
 *      白衣の中の小さなハイライトまで抜かないよう、しきい値で線を引く
 *   3. 背景に接する半端な明るさの画素（線画のアンチエイリアス）を半透明にし、
 *      混ざり込んだ白を差し引く（アンチエイリアスを残さないと輪郭がギザつき、
 *      白を引かないと縁に白いフチが残る）
 *   4. 中身の外接矩形で切り詰め、縦横比を保ったまま指定サイズの正方形へ収める
 *      （CharacterPane は preserveRatio + TOP_CENTER で焼くので上寄せにする）
 */
public class RemoveWhiteBackground {

    /** ここ以上に明るい画素は「背景の白」の候補。JPEG 由来のムラを見込んで 255 より下げる */
    private static final int DEFAULT_TOLERANCE = 236;

    /** 出力の一辺。ASSETS.md の立ち絵基準に合わせる */
    private static final int DEFAULT_SIZE = 1080;

    /** アンチエイリアスとして扱う下限。これより暗ければ線画本体なので不透明のまま残す */
    private static final int EDGE_FLOOR = 150;

    /** 切り詰めたあとに残す余白（出力サイズ基準の割合）。0 だと輪郭が縁に貼り付く */
    private static final double MARGIN_RATIO = 0.01;

    /**
     * 「閉じた白」をこのサイズ以上なら背景として抜く（px）。
     * 隙間の背景は数千 px 規模、白衣の中のハイライトは数十 px 規模なので間が空く。
     * 絵によっては合わないので {@code --enclosed} で調整すること。0 で無効。
     */
    private static final int DEFAULT_ENCLOSED_MIN = 200;

    // ---- --lineart 用。tools/BuildRoughMocks.java と同じ値にすること ----

    /** 線画の色（KowloonPalette.LIGHT / 病的な蛍光灯色） */
    private static final int LINE_RGB = 0xB7C89A;
    /** 紙の白と見なす明るさ。これ以上は完全に透明 */
    private static final int PAPER_LUM = 250;
    /** 完全に不透明にする明るさ。これ以下は線の芯 */
    private static final int INK_LUM = 25;
    /** 全体の濃さ。1.0 だと主張しすぎる */
    private static final double LINE_OPACITY = 0.85;

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("usage: java tools\\RemoveWhiteBackground.java <入力> <出力> "
                    + "[--size 1080] [--tolerance 236] [--enclosed 200]");
            return;
        }
        File in = new File(args[0]);
        File out = new File(args[1]);
        int size = intArg(args, "--size", DEFAULT_SIZE);
        int tolerance = intArg(args, "--tolerance", DEFAULT_TOLERANCE);
        int enclosedMin = intArg(args, "--enclosed", DEFAULT_ENCLOSED_MIN);

        // --canvas 1920x1080 で任意の画布へ。既定は --size の正方形（立ち絵用）
        int canvasW = size, canvasH = size;
        String canvas = strArg(args, "--canvas");
        if (canvas != null) {
            String[] wh = canvas.toLowerCase().split("x");
            if (wh.length != 2) {
                System.out.println("[error] --canvas は 1920x1080 の形式で指定する: " + canvas);
                return;
            }
            canvasW = Integer.parseInt(wh[0]);
            canvasH = Integer.parseInt(wh[1]);
        }
        boolean cover = hasFlag(args, "--cover");

        if (!in.isFile()) {
            System.out.println("[error] 入力が見つからない: " + in.getAbsolutePath());
            return;
        }

        BufferedImage src = ImageIO.read(in);
        if (src == null) {
            System.out.println("[error] 画像として読めない: " + in.getAbsolutePath());
            return;
        }
        System.out.printf("[in ] %s  %dx%d%n", in.getName(), src.getWidth(), src.getHeight());

        BufferedImage cut = hasFlag(args, "--lineart")
                ? toLineArt(src)
                : cutBackground(src, tolerance, enclosedMin);
        BufferedImage fitted = fitToCanvas(cut, canvasW, canvasH, cover);

        File parent = out.getParentFile();
        if (parent != null) parent.mkdirs();
        ImageIO.write(fitted, "png", out);
        System.out.printf("[out] %s  %dx%d%n", out.getPath(), fitted.getWidth(), fitted.getHeight());
    }

    // ==================================================
    //   1) 縁から連結した白だけを抜く
    // ==================================================

    private static BufferedImage cutBackground(BufferedImage src, int tolerance, int enclosedMin) {
        int w = src.getWidth();
        int h = src.getHeight();
        boolean[] bg = new boolean[w * h];

        Deque<int[]> queue = new ArrayDeque<>();
        for (int x = 0; x < w; x++) {
            seed(src, bg, queue, x, 0, tolerance);
            seed(src, bg, queue, x, h - 1, tolerance);
        }
        for (int y = 0; y < h; y++) {
            seed(src, bg, queue, 0, y, tolerance);
            seed(src, bg, queue, w - 1, y, tolerance);
        }

        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};
        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            for (int i = 0; i < 4; i++) {
                seed(src, bg, queue, p[0] + dx[i], p[1] + dy[i], tolerance);
            }
        }

        if (enclosedMin > 0) {
            addEnclosedBackground(src, bg, tolerance, enclosedMin);
        }

        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int removed = 0;
        int feathered = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int idx = y * w + x;
                if (bg[idx]) {
                    dst.setRGB(x, y, 0);
                    removed++;
                    continue;
                }
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                int v = Math.max(r, Math.max(g, b));

                // 背景に隣接していて、かつ半端に明るい = 線画のアンチエイリアス。
                // 内側の白（白衣）は背景に隣接しないのでここへ来ない
                if (v > EDGE_FLOOR && v < 255 && touchesBackground(bg, w, h, x, y)) {
                    double alpha = (255.0 - v) / (255.0 - EDGE_FLOOR);
                    alpha = Math.max(0.0, Math.min(1.0, alpha));
                    if (alpha <= 0.0) {
                        dst.setRGB(x, y, 0);
                        removed++;
                    } else {
                        // 白と混ざったぶんを差し引く（縁の白フチ落とし）
                        dst.setRGB(x, y, argb(
                                (int) Math.round(255 * alpha),
                                unmix(r, alpha), unmix(g, alpha), unmix(b, alpha)));
                        feathered++;
                    }
                    continue;
                }
                dst.setRGB(x, y, 0xFF000000 | (rgb & 0x00FFFFFF));
            }
        }
        System.out.printf("[cut] 背景 %,d px を除去 / 輪郭 %,d px を半透明化 (%.1f%% が背景)%n",
                removed, feathered, 100.0 * removed / (w * h));
        return dst;
    }

    /**
     * キャラに囲まれていて画像の縁と繋がらない白（腰に当てた腕と胴の隙間など）を
     * 背景に足す。白衣の中の小さなハイライトを巻き込まないよう、
     * {@code minPixels} 以上の塊だけを対象にする。
     */
    private static void addEnclosedBackground(
            BufferedImage src, boolean[] bg, int tolerance, int minPixels) {
        int w = src.getWidth(), h = src.getHeight();
        boolean[] seen = new boolean[w * h];
        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};
        int filled = 0, regions = 0;

        for (int y0 = 0; y0 < h; y0++) {
            for (int x0 = 0; x0 < w; x0++) {
                int start = y0 * w + x0;
                if (bg[start] || seen[start] || !isWhite(src, x0, y0, tolerance)) continue;

                // 1 塊を数え上げてから、大きければまとめて背景にする
                Deque<Integer> q = new ArrayDeque<>();
                java.util.List<Integer> members = new java.util.ArrayList<>();
                seen[start] = true;
                q.add(start);
                while (!q.isEmpty()) {
                    int p = q.poll();
                    members.add(p);
                    int px = p % w, py = p / w;
                    for (int i = 0; i < 4; i++) {
                        int nx = px + dx[i], ny = py + dy[i];
                        if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                        int ni = ny * w + nx;
                        if (seen[ni] || bg[ni] || !isWhite(src, nx, ny, tolerance)) continue;
                        seen[ni] = true;
                        q.add(ni);
                    }
                }
                if (members.size() < minPixels) continue;
                for (int p : members) bg[p] = true;
                filled += members.size();
                regions++;
            }
        }
        System.out.printf("[gap] 閉じた背景 %d 箇所 / %,d px を追加で除去（%d px 未満は残す）%n",
                regions, filled, minPixels);
    }

    private static boolean isWhite(BufferedImage src, int x, int y, int tolerance) {
        int rgb = src.getRGB(x, y);
        return ((rgb >> 16) & 0xFF) >= tolerance
                && ((rgb >> 8) & 0xFF) >= tolerance
                && (rgb & 0xFF) >= tolerance;
    }

    private static void seed(BufferedImage src, boolean[] bg, Deque<int[]> queue,
                             int x, int y, int tolerance) {
        int w = src.getWidth(), h = src.getHeight();
        if (x < 0 || y < 0 || x >= w || y >= h) return;
        int idx = y * w + x;
        if (bg[idx]) return;
        int rgb = src.getRGB(x, y);
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        if (r < tolerance || g < tolerance || b < tolerance) return;
        bg[idx] = true;
        queue.add(new int[]{x, y});
    }

    private static boolean touchesBackground(boolean[] bg, int w, int h, int x, int y) {
        for (int oy = -1; oy <= 1; oy++) {
            for (int ox = -1; ox <= 1; ox++) {
                int nx = x + ox, ny = y + oy;
                if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                if (bg[ny * w + nx]) return true;
            }
        }
        return false;
    }

    /** 観測色 = alpha*本来の色 + (1-alpha)*白 を解いて本来の色に戻す */
    private static int unmix(int observed, double alpha) {
        double v = (observed - 255.0 * (1.0 - alpha)) / alpha;
        return (int) Math.round(Math.max(0, Math.min(255, v)));
    }

    private static int argb(int a, int r, int g, int b) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // ==================================================
    //   2) 中身で切り詰めて正方形へ収める
    // ==================================================

    /**
     * 中身の外接矩形で切り詰めてから画布へ収める。
     *
     * @param cover true なら画布を埋めるまで拡大してはみ出しを切る（全画面モック用）。
     *              false なら画布に収まるまで縮めて上寄せ・中央そろえ（立ち絵用）
     */
    private static BufferedImage fitToCanvas(BufferedImage src, int cw0, int ch0, boolean cover) {
        int w = src.getWidth(), h = src.getHeight();
        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (((src.getRGB(x, y) >>> 24) & 0xFF) == 0) continue;
                if (x < minX) minX = x;
                if (y < minY) minY = y;
                if (x > maxX) maxX = x;
                if (y > maxY) maxY = y;
            }
        }
        if (maxX < 0) {
            System.out.println("[warn] 中身が空。全部背景と判定された。--tolerance を上げて試すこと");
            return src;
        }
        int cw = maxX - minX + 1, ch = maxY - minY + 1;
        System.out.printf("[fit] 中身の外接矩形 %dx%d (元 %dx%d)%n", cw, ch, w, h);

        BufferedImage trimmed = src.getSubimage(minX, minY, cw, ch);

        int margin = cover ? 0 : (int) Math.round(Math.min(cw0, ch0) * MARGIN_RATIO);
        double boxW = cw0 - margin * 2.0, boxH = ch0 - margin * 2.0;
        double scale = cover
                ? Math.max(boxW / cw, boxH / ch)   // 画布を埋める。はみ出しは切る
                : Math.min(boxW / cw, boxH / ch);  // 画布に収める
        int dw = (int) Math.round(cw * scale), dh = (int) Math.round(ch * scale);

        BufferedImage dst = new BufferedImage(cw0, ch0, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // cover は中央そろえ。fit は CharacterPane の上端中央クリップに合わせて上寄せ
        int dx = (cw0 - dw) / 2;
        int dy = cover ? (ch0 - dh) / 2 : margin;
        g.drawImage(trimmed, dx, dy, dw, dh, null);
        g.dispose();
        System.out.printf("[fit] %s %dx%d へ %s%n",
                cover ? "cover" : "fit", cw0, ch0, dw + "x" + dh);
        return dst;
    }

    private static String strArg(String[] args, String key) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(key)) return args[i + 1];
        }
        return null;
    }

    /**
     * 下描き向け。紙の白地を透明に、線を蛍光灯色にする。
     *
     * <p>線画は<b>内側が白い</b>ので、フラッドフィル方式（{@link #cutBackground}）に
     * かけると体の内側まで背景と見なされて抜ける。かといって残すと紙が白いまま
     * 板のように見える。線の濃さをそのまま不透明度にすれば、下描きは下描きのまま
     * 暗い画面に馴染む。{@code tools/BuildRoughMocks.java} の構図ラフと同じ扱い。
     */
    private static BufferedImage toLineArt(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int inked = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                int lum = (r * 30 + g * 59 + b * 11) / 100;
                double t = Math.max(0, Math.min(1,
                        (lum - INK_LUM) / (double) (PAPER_LUM - INK_LUM)));
                int alpha = (int) (255.0 * (1.0 - t) * LINE_OPACITY);
                if (alpha <= 2) continue;
                out.setRGB(x, y, (alpha << 24) | LINE_RGB);
                inked++;
            }
        }
        System.out.printf("[line] 線画 %,d px を残した（紙は透明・色は #%06X）%n", inked, LINE_RGB);
        return out;
    }

    private static boolean hasFlag(String[] args, String key) {
        for (String a : args) {
            if (a.equals(key)) return true;
        }
        return false;
    }

    private static int intArg(String[] args, String key, int def) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(key)) {
                try { return Integer.parseInt(args[i + 1]); }
                catch (NumberFormatException ignored) { return def; }
            }
        }
        return def;
    }

    private RemoveWhiteBackground() {
    }
}
