package tetris.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

/**
 * 画面下部の操作ヒント帯。キーキャップ風の枠とラベルを 1 セットにして横に並べる。
 *
 * 空きスペースを埋めるための飾りではなく、「このゲームで何が押せるか」を常時出しておく
 * ためのもの。配置定義（{@link UiLayout}）で位置・幅を持ち、置かない配置では非表示にできる。
 */
public final class KeyHintPane extends StackPane {

    /** キー表記 → 動作 のペア。ここを直せば表示が変わる */
    private static final String[][] HINTS = {
        {"← →", "MOVE"},
        {"↓",        "SOFT"},
        {"SPACE",         "HARD"},
        {"Z / X",         "ROTATE"},
        {"H",             "HOLD"},
        {"P",             "PAUSE"},
    };

    private final HBox row;
    private final Label[] keyLabels;
    private final Label[] actionLabels;

    public KeyHintPane(double width, double height) {
        setAlignment(Pos.CENTER);

        row = new HBox(26);
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(0, UiMetrics.PAD, 0, UiMetrics.PAD));

        keyLabels = new Label[HINTS.length];
        actionLabels = new Label[HINTS.length];
        for (int i = 0; i < HINTS.length; i++) {
            keyLabels[i] = new Label(HINTS[i][0]);
            keyLabels[i].setPadding(new Insets(2, 10, 2, 10));
            actionLabels[i] = new Label(HINTS[i][1]);

            HBox chip = new HBox(8, keyLabels[i], actionLabels[i]);
            chip.setAlignment(Pos.CENTER);
            row.getChildren().add(chip);
        }

        getChildren().add(row);
        setPaneSize(width, height);
        applySkin(UiSkinBank.forStep(0));
    }

    public void setPaneSize(double width, double height) {
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);
    }

    public void applySkin(UiSkin skin) {
        setStyle(skin.panelStyle());

        String keyStyle = skin.fontStyle(UiMetrics.FONT_HINT)
            + " -fx-font-weight: bold;"
            + " -fx-text-fill: " + skin.theme.textColor + ";"
            + " -fx-border-color: " + KowloonPalette.rgba(skin.theme.accentColor, 0.75) + ";"
            + " -fx-border-width: 1px;"
            + " -fx-background-color: " + KowloonPalette.rgba(KowloonPalette.SHADOW_HEX, 0.55) + ";";
        String actionStyle = skin.fontStyle(UiMetrics.FONT_HINT)
            + " -fx-text-fill: " + KowloonPalette.rgba(KowloonPalette.LIGHT_HEX, 0.55) + ";";

        for (int i = 0; i < keyLabels.length; i++) {
            keyLabels[i].setStyle(keyStyle);
            actionLabels[i].setStyle(actionStyle);
        }
    }
}
