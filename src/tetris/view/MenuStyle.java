package tetris.view;

/**
 * タイトル・ポーズ・ゲームオーバーといったメニュー画面の文字スタイル。
 *
 * ゲーム中の UI は {@link KowloonPalette} の 5 色だけで組まれているのに、メニューだけが
 * 青系（#66bbff など）のままだと、同じゲームの画面に見えない。ここに集約して
 * 全画面を同じ配色・同じタイプスケールに載せる。
 *
 * 階層（{@link UiSkin}）で色が入れ替わるゲーム画面と違い、メニューは常にこの 1 セット。
 * 「盤面の外は変化しない」ことで、回転で世界が変わるのがゲーム画面だけだと分かる。
 */
public final class MenuStyle {

    private static final String FONT = "Courier New";

    private static String font(int px) {
        return "-fx-font-size: " + px + "px; -fx-font-family: '" + FONT + "';";
    }

    /** 画面の主題（TETRIS / PAUSED / GAME OVER）。色は用途ごとに渡す */
    public static String title(int px, String colorHex) {
        return font(px)
            + " -fx-font-weight: bold;"
            + " -fx-text-fill: " + colorHex + ";"
            + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.85), 12, 0.0, 2, 2);";
    }

    /** 「Press SPACE to Start」など、次の操作を促す行 */
    public static String prompt() {
        return font(26)
            + " -fx-text-fill: " + KowloonPalette.LIGHT_HEX + ";"
            + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 5, 0.0, 1, 1);";
    }

    /** 補助的な操作ヒント（一段弱く） */
    public static String hint() {
        return font(22)
            + " -fx-text-fill: " + KowloonPalette.rgba(KowloonPalette.LIGHT_HEX, 0.55) + ";";
    }

    /** 数値・記録の表示 */
    public static String value(int px, String colorHex) {
        return font(px) + " -fx-text-fill: " + colorHex + ";";
    }

    /** スコアなどを囲む枠（ゲーム中のパネルと同じ様式に揃える） */
    public static String box() {
        return "-fx-background-color: " + KowloonPalette.rgba(KowloonPalette.SHADOW_HEX, 0.72) + ";"
            + " -fx-padding: 30px 60px;"
            + " -fx-border-color: " + KowloonPalette.rgba(KowloonPalette.LIGHT_HEX, 0.35) + ";"
            + " -fx-border-width: 1px;";
    }

    private MenuStyle() {
    }
}
