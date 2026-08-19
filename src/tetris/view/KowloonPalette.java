package tetris.view;

import java.util.Locale;

import javafx.scene.paint.Color;

/**
 * 九龍城テーマの基準パレット（5色）。UI に出る色はすべてここから取る。
 *
 * 画面をごたつかせても汚く見えないための条件は「色数を増やさないこと」なので、
 * 各 View に新しい hex を直書きせず、必ずこの 5 色（＋ alpha 違い）で組む。
 * 階層ごとの差は「どの色を枠／アクセント／フラッシュに割り当てるか」で作る
 * （割り当て表は UiTheme）。
 */
public final class KowloonPalette {

    /** ベース: 暗い青緑（湿ったコンクリート） */
    public static final String BASE_HEX   = "#214743";
    /** 影: 濡れた煤黒 */
    public static final String SHADOW_HEX = "#171C1B";
    /** 光: 病的な蛍光灯色 */
    public static final String LIGHT_HEX  = "#B7C89A";
    /** 差し色: 錆びた赤橙 */
    public static final String RUST_HEX   = "#9A4B32";
    /** 看板: 褪せたネオン赤 */
    public static final String NEON_HEX   = "#C83F4D";

    public static final Color BASE   = Color.web(BASE_HEX);
    public static final Color SHADOW = Color.web(SHADOW_HEX);
    public static final Color LIGHT  = Color.web(LIGHT_HEX);
    public static final Color RUST   = Color.web(RUST_HEX);
    public static final Color NEON   = Color.web(NEON_HEX);

    /** パレット色に alpha を与えた CSS rgba 文字列（パネル背景用） */
    public static String rgba(String hex, double alpha) {
        Color c = Color.web(hex);
        return String.format(Locale.ROOT, "rgba(%d, %d, %d, %.2f)",
                to255(c.getRed()), to255(c.getGreen()), to255(c.getBlue()), alpha);
    }

    /** パレット色に alpha を与えた JavaFX Color（Canvas 描画用） */
    public static Color alpha(Color c, double alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    private static int to255(double v) {
        return (int) Math.round(v * 255);
    }

    private KowloonPalette() {
    }
}
