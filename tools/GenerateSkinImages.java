import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;

/**
 * 背景・スキン画像の生成ツール。アプリ本体には含まれない
 * （build.bat / pom.xml は src\ 以下のみコンパイルする）。
 *
 * 実行方法（リポジトリルートで。JDK のみで動く・JavaFX 不要・再実行冪等）:
 *   java tools\GenerateSkinImages.java
 *
 * ## 方針: 背景は「絵」ではなく「壁」
 *
 * 以前は用意された1枚絵（サイバーパンクの端末や路地のイラスト）を加工して
 * 背景にしていたが、元絵が独自の看板・文字・別UIを持っているため、
 * どれだけぼかしても前景の情報（スコア・NEXT・盤面）と競合して画面が汚れた。
 *
 * そこで背景は**パレット5色だけを使って手続き的に描く**。九龍城の密度は
 * 乱雑さではなく「同じ増築ユニットの反復」なので、窓・室外機・配管・汚れ筋を
 * 一定の文法で敷き詰めるだけで、読めない情報を持たないまま密度が出る。
 *
 * 生成物（images/ へ出力。コミットする）:
 *   skin-<フロア>-hud-bg.png  (480x400) / -next-bg.png (420x168)
 *   skin-<フロア>-playfield-bg.png (840x840)
 *     … 増築ユニットの壁。playfield は盤面の視認優先で一番コントラストが低い
 *   skin-<フロア>-character.png / -approach.png (1080x1080)
 *     … フロア色の透過シルエット（本番立ち絵ができたら差し替える）
 *   bg-kowloon-base-layer.png (1920x1080) … 全画面共通の背景
 *   bg-kowloon-character-panel.png (440x560) … キャラパネルの背景
 *
 * 調整は下の SKINS 表（3色）と各 draw* の呼び出し引数だけを触る。
 * 使う色は必ず PALETTE 5 色から選ぶこと。
 */
public class GenerateSkinImages {

    // ---- 九龍城パレット（src/tetris/view/KowloonPalette.java と同じ5色） ----
    private static final Color BASE   = new Color(0x21, 0x47, 0x43); // 暗い青緑
    private static final Color SHADOW = new Color(0x17, 0x1C, 0x1B); // 濡れた煤黒
    private static final Color LIGHT  = new Color(0xB7, 0xC8, 0x9A); // 病的な蛍光灯色
    private static final Color RUST   = new Color(0x9A, 0x4B, 0x32); // 錆びた赤橙
    private static final Color NEON   = new Color(0xC8, 0x3F, 0x4D); // 褪せたネオン赤

    /**
     * フロア表: ファイル名 / 表示名 / 壁の地色 / 増築部材の色 / 窓明かりの色。
     * 3色の組み替えだけでフロアを描き分け、色数は増やさない。
     */
    private static final Skin[] SKINS = {
        // 1F 電気街: コンクリートの壁、錆びた部材、蛍光灯の窓
        new Skin("kowloon-arcade",  "1F ARCADE",  BASE,   RUST,  LIGHT),
        // 5F 市場: 煤けた壁一面が錆に覆われ、窓だけが蛍光灯
        new Skin("kowloon-market",  "5F MARKET",  SHADOW, RUST,  LIGHT),
        // 9F 診療所: 蛍光灯に洗われた明るい階。部材まで白く浮く
        new Skin("kowloon-clinic",  "9F CLINIC",  BASE,   LIGHT, LIGHT),
        // RF 屋上: ほぼ闇。看板のネオンだけが灯る
        new Skin("kowloon-rooftop", "RF ROOFTOP", SHADOW, BASE,  NEON),
    };

    private static final File IMAGES_DIR = new File("images");
    private static final int CHARACTER_SIZE = 1080;

    public static void main(String[] args) throws IOException {
        if (!IMAGES_DIR.isDirectory()) {
            System.err.println("[ERROR] images/ が見つかりません。リポジトリルートで実行してください。");
            System.exit(1);
        }

        int count = 0;
        for (Skin skin : SKINS) {
            long seed = skin.name.hashCode(); // フロアごとに固定 → 再実行冪等

            // HUD / NEXT: 上にスコアや NEXT ミノが乗るので、壁は見えるだけの強さに留める。
            // フロア名は焼き込まない。パネル見出し（PanelHeader）が右端にフロア名を出すので
            // 二重表示になるうえ、配置ごとにパネルの縦横比が変わると文字だけが伸びて汚い。
            // サイズは正方形寄りの大きめに取る。PanelBackground が中央クロップで
            // 枠を覆うため、縦長・横長どちらの配置でも同じ密度の壁に見える。
            BufferedImage hud = drawWall(640, 640, skin, seed + 1, 48, 0.36, 0.18);
            count += write(hud, "skin-" + skin.name + "-hud-bg.png");

            BufferedImage next = drawWall(640, 400, skin, seed + 2, 44, 0.36, 0.18);
            count += write(next, "skin-" + skin.name + "-next-bg.png");

            // プレイフィールド: ミノとグリッドが主役。壁は一番暗く・一番低コントラスト
            BufferedImage field = drawWall(840, 840, skin, seed + 3, 70, 0.22, 0.45);
            stampName(field, skin.label, skin.accent, true);
            count += write(field, "skin-" + skin.name + "-playfield-bg.png");

            count += write(drawCharacterSilhouette(skin), "skin-" + skin.name + "-character.png");
            count += write(drawApproachSilhouette(skin), "skin-" + skin.name + "-approach.png");

        }

        // 全画面背景・キャラパネル背景は 1F ARCADE の語彙を共通の下地として使う
        Skin baseSkin = SKINS[0];
        // 全画面はモジュールを大きく取る。細かいと方眼紙に見えて建物にならない
        count += write(drawWall(1920, 1080, baseSkin, 9001, 132, 0.34, 0.52),
                "bg-kowloon-base-layer.png");
        count += write(drawWall(440, 560, baseSkin, 9002, 46, 0.40, 0.30),
                "bg-kowloon-character-panel.png");

        System.out.println("Done: " + count + " files -> " + IMAGES_DIR.getPath() + File.separator);
    }

    // ============================================================
    //  増築ユニットの壁
    // ============================================================

    /**
     * 九龍城の壁を描く。同じユニット（窓＋室外機）を格子状に反復し、
     * 縦の配管と汚れ筋を重ねる。文字や看板は一切描かないので、
     * どれだけ密度を上げても「読めない情報」は増えない。
     *
     * @param module   ユニット1個の辺の長さ(px)。小さいほど密になる
     * @param contrast 部材・窓の見え方(0..1)。前景が乗る面ほど下げる
     * @param darken   仕上げに煤黒へ寄せる量(0..1)
     */
    private static BufferedImage drawWall(
            int w, int h, Skin skin, long seed, int module, double contrast, double darken) {

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Random rnd = new Random(seed);

        Color wall = skin.wall;

        // 下地: 上がわずかに明るい縦グラデーション（上階からの光の回り込み）
        g.setPaint(new GradientPaint(
                0, 0, lerp(wall, skin.part, 0.18 * contrast),
                0, h, lerp(wall, SHADOW, 0.35)));
        g.fillRect(0, 0, w, h);

        int cols = w / module + 2;
        int rows = h / module + 2;

        // 増築ユニットの反復
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                // 段ごとに横へずらす（きれいに揃わせない = 増築のガタつき）
                int x = c * module - (r % 2) * module / 3;
                int y = r * module;
                drawUnit(g, rnd, x, y, module, skin, wall, contrast);
            }
        }

        // 床スラブの水平線
        g.setStroke(new BasicStroke(1f));
        for (int y = module; y < h; y += module) {
            g.setColor(alpha(SHADOW, (int) (70 * contrast)));
            g.drawLine(0, y, w, y);
            g.setColor(alpha(skin.part, (int) (34 * contrast)));
            g.drawLine(0, y + 1, w, y + 1);
        }

        // 縦の配管: 数本だけ通し、壁の反復を断ち切る
        int pipes = Math.max(1, w / (module * 3));
        for (int i = 0; i < pipes; i++) {
            int px = rnd.nextInt(Math.max(1, w));
            int pw = 3 + rnd.nextInt(4);
            g.setColor(alpha(SHADOW, (int) (110 * contrast)));
            g.fillRect(px + 1, 0, pw, h);
            g.setColor(alpha(skin.part, (int) (80 * contrast)));
            g.fillRect(px, 0, pw - 1, h);
            // 留め金
            for (int y = module / 2; y < h; y += module * 2) {
                g.fillRect(px - 2, y, pw + 3, 4);
            }
        }

        // 汚れの縦筋: 窓の下から垂れる
        for (int i = 0; i < cols * 2; i++) {
            int sx = rnd.nextInt(Math.max(1, w));
            int sy = rnd.nextInt(Math.max(1, h));
            int sh = module + rnd.nextInt(module * 3);
            g.setPaint(new GradientPaint(
                    0, sy, alpha(SHADOW, (int) (85 * contrast)),
                    0, sy + sh, alpha(SHADOW, 0)));
            g.fillRect(sx, sy, 2 + rnd.nextInt(3), sh);
        }

        // ビネット: 四隅を落として、パネルの中央に視線を残す
        g.setPaint(new RadialGradientPaint(
                new Point2D.Float(w / 2f, h / 2f),
                Math.max(w, h) * 0.72f,
                new float[] {0.35f, 1.0f},
                new Color[] {alpha(SHADOW, 0), alpha(SHADOW, 165)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g.fillRect(0, 0, w, h);

        // 仕上げの減光
        if (darken > 0) {
            g.setColor(alpha(SHADOW, (int) (255 * darken)));
            g.fillRect(0, 0, w, h);
        }

        g.dispose();
        return img;
    }

    /** 増築ユニット1個: 枠 + 窓 + 室外機 */
    private static void drawUnit(
            Graphics2D g, Random rnd, int x, int y, int m, Skin skin, Color wall, double contrast) {

        // ユニットの外枠（鉄骨）
        g.setColor(alpha(skin.part, (int) (58 * contrast)));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRect(x, y, m - 1, m - 1);

        // 窓: ユニットの上半分
        int pad = Math.max(3, m / 7);
        int ww = m - pad * 2;
        int wh = m / 2 - pad / 2;
        boolean lit = rnd.nextDouble() < 0.16;
        g.setColor(lit
                ? alpha(lerp(wall, skin.accent, 0.75), (int) (200 * contrast))
                : alpha(SHADOW, (int) (120 * contrast)));
        g.fillRect(x + pad, y + pad, ww, wh);
        // サッシの桟
        g.setColor(alpha(SHADOW, (int) (150 * contrast)));
        g.drawLine(x + pad + ww / 2, y + pad, x + pad + ww / 2, y + pad + wh);

        // 室外機: 3割のユニットにだけ付く
        if (rnd.nextDouble() < 0.32) {
            int uw = m / 3;
            int uh = m / 5;
            int ux = x + m - pad - uw;
            int uy = y + m - pad - uh;
            g.setColor(alpha(lerp(wall, skin.part, 0.55), (int) (190 * contrast)));
            g.fillRect(ux, uy, uw, uh);
            g.setColor(alpha(SHADOW, (int) (140 * contrast)));
            g.drawRect(ux, uy, uw, uh);
            for (int i = 1; i < 4; i++) {
                int ly = uy + uh * i / 4;
                g.drawLine(ux + 2, ly, ux + uw - 2, ly);
            }
        }
    }

    // ============================================================
    //  キャラプレースホルダ
    // ============================================================

    /**
     * フロア名を焼き込む（F2 でのフロア切替確認が一目で分かるように）。
     * 現在の呼び出しはプレイフィールドのみ。HUD / NEXT は PanelHeader 側でフロア名を出す。
     *
     * @param topLeft true=左上 / false=右下
     */
    private static void stampName(BufferedImage img, String label, Color textColor, boolean topLeft) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));

        FontMetrics fm = g.getFontMetrics();
        int x = topLeft ? 12 : img.getWidth() - fm.stringWidth(label) - 12;
        int y = topLeft ? 28 : img.getHeight() - 12;

        g.setColor(new Color(0, 0, 0, 170));
        g.drawString(label, x + 1, y + 1);
        g.setColor(alpha(textColor, 120));
        g.drawString(label, x, y);
        g.dispose();
    }

    /** フロア色の人型シルエット（透過背景）を合成する */
    private static BufferedImage drawCharacterSilhouette(Skin skin) {
        int w = CHARACTER_SIZE;
        int h = CHARACTER_SIZE;
        int cx = w / 2;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 胴体: 肩から裾に向かって広がるカーブ形状
        Path2D torso = new Path2D.Double();
        torso.moveTo(cx - 190, 470);
        torso.curveTo(cx - 260, 560, cx - 300, 800, cx - 320, h);
        torso.lineTo(cx + 320, h);
        torso.curveTo(cx + 300, 800, cx + 260, 560, cx + 190, 470);
        torso.closePath();
        Ellipse2D head = new Ellipse2D.Double(cx - 140, 130, 280, 280);

        g.setPaint(new GradientPaint(0, 130, skin.wall, 0, h, lerp(skin.wall, SHADOW, 0.6)));
        g.fill(torso);
        g.fillRect(cx - 55, 380, 110, 110); // 首
        g.fill(head);

        // 輪郭線: 暗色パネルに重ねても形が分かるように
        g.setStroke(new BasicStroke(5f));
        g.setColor(alpha(skin.accent, 130));
        g.draw(torso);
        g.draw(head);

        // フロア名を胸元へ。プレースホルダの識別用なので、UI の見出しより弱く小さく置く
        // （大きく焼くと立ち絵ではなく看板に見え、配置のプレビューが正しく評価できない）
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 30));
        drawCentered(g, skin.label, w, 790, alpha(skin.accent, 110));

        g.dispose();
        return img;
    }

    /**
     * 寄り演出用の差分プレースホルダ。常設シルエットより上半身へズームした
     * バスト構図（頭が大きく・肩幅広く・裾は画面外へ抜ける）で、"APPROACH" を焼き込む。
     */
    private static BufferedImage drawApproachSilhouette(Skin skin) {
        int w = CHARACTER_SIZE;
        int h = CHARACTER_SIZE;
        int cx = w / 2;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 肩〜胸: 画面下端まで大きく広がり、裾は枠外へ抜ける（寄っている印象）
        Path2D bust = new Path2D.Double();
        bust.moveTo(cx - 300, 640);
        bust.curveTo(cx - 460, 780, cx - 520, 980, cx - 540, h);
        bust.lineTo(cx + 540, h);
        bust.curveTo(cx + 520, 980, cx + 460, 780, cx + 300, 640);
        bust.closePath();
        // 頭部: 常設より一回り大きく、上寄り
        Ellipse2D head = new Ellipse2D.Double(cx - 210, 90, 420, 420);

        g.setPaint(new GradientPaint(0, 90, skin.wall, 0, h, lerp(skin.wall, SHADOW, 0.6)));
        g.fill(bust);
        g.fillRect(cx - 85, 470, 170, 180); // 首
        g.fill(head);

        g.setStroke(new BasicStroke(6f));
        g.setColor(alpha(skin.accent, 130));
        g.draw(bust);
        g.draw(head);

        // ラベル: フロア名 + APPROACH
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 72));
        drawCentered(g, skin.label, w, 940, skin.accent);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 40));
        drawCentered(g, "APPROACH", w, 1010, skin.accent);

        g.dispose();
        return img;
    }

    /** 水平中央寄せで、黒縁つきの文字を描く */
    private static void drawCentered(Graphics2D g, String text, int w, int baselineY, Color color) {
        FontMetrics fm = g.getFontMetrics();
        int tx = (w - fm.stringWidth(text)) / 2;
        g.setColor(new Color(0, 0, 0, 160));
        g.drawString(text, tx + 3, baselineY + 3);
        g.setColor(color);
        g.drawString(text, tx, baselineY);
    }

    // ============================================================
    //  色ユーティリティ
    // ============================================================

    /** 2色を t(0..1) で線形補間する */
    private static Color lerp(Color from, Color to, double t) {
        double k = Math.max(0.0, Math.min(1.0, t));
        return new Color(
            (int) Math.round(from.getRed()   + (to.getRed()   - from.getRed())   * k),
            (int) Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * k),
            (int) Math.round(from.getBlue()  + (to.getBlue()  - from.getBlue())  * k));
    }

    /** 色に alpha(0..255) を与える */
    private static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    private static int write(BufferedImage img, String filename) throws IOException {
        File dest = new File(IMAGES_DIR, filename);
        ImageIO.write(img, "png", dest);
        System.out.println("  " + dest.getPath());
        return 1;
    }

    /** フロア表の1行: 壁の地色 / 増築部材の色 / 窓明かり・アクセントの色 */
    private record Skin(String name, String label, Color wall, Color part, Color accent) {
    }
}
