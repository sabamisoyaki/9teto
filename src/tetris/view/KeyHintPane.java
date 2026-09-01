package tetris.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import tetris.model.GameAction;
import tetris.model.KeyBindings;

/**
 * 画面下部の操作ヒント帯。キーキャップ風の枠とラベルを 1 セットにして横に並べる。
 *
 * 空きスペースを埋めるための飾りではなく、「このゲームで何が押せるか」を常時出しておく
 * ためのもの。配置定義（{@link UiLayout}）で位置・幅を持ち、置かない配置では非表示にできる。
 */
public final class KeyHintPane extends StackPane {

    /**
     * キー表記 → 動作 のペア。キー表記は割り当ての既定値で、
     * {@link #applyBindings} を呼ぶと実際の割り当てに差し替わる。
     */
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

    /**
     * 実際のキー割り当てを表示へ反映する。
     * ヒントが嘘をつくと、キーを変えた人が操作を探せなくなる。
     *
     * <p>MOVE と ROTATE は 2 つの操作をまとめて 1 チップに出す（左右・左右回転）。
     * PAUSE は割り当て対象外なので固定表記のまま。
     */
    public void applyBindings(KeyBindings binds) {
        for (int i = 0; i < HINTS.length; i++) {
            String text = switch (HINTS[i][1]) {
                case "MOVE"   -> binds.describe(GameAction.MOVE_LEFT)
                                 + " " + binds.describe(GameAction.MOVE_RIGHT);
                case "SOFT"   -> binds.describe(GameAction.SOFT_DROP);
                case "HARD"   -> binds.describe(GameAction.HARD_DROP);
                case "ROTATE" -> binds.describe(GameAction.ROTATE_LEFT)
                                 + " / " + binds.describe(GameAction.ROTATE_RIGHT);
                case "HOLD"   -> binds.describe(GameAction.HOLD);
                default       -> HINTS[i][0];
            };
            keyLabels[i].setText(text);
        }
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
