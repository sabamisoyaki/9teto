package tetris.view;

import java.util.EnumMap;
import java.util.Map;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import tetris.model.GameAction;
import tetris.model.KeyBindings;

/**
 * キー割り当ての変更画面。
 *
 * <p>操作を ↑↓ で選び、ENTER で「待機中」にしてから割り当てたいキーを押す。
 * この 2 段構えにしているのは、選択の移動と割り当てを同じキーで兼ねられないため
 * （↑ を割り当てようとした瞬間にカーソルが動いてしまう）。
 */
public final class KeyConfigPane {

    private static final int WINDOW_WIDTH  = 1920;
    private static final int WINDOW_HEIGHT = 1080;

    private final StackPane root;
    private final KeyBindings binds;
    private final Map<GameAction, Label> valueLabels = new EnumMap<>(GameAction.class);
    private final Map<GameAction, Label> nameLabels = new EnumMap<>(GameAction.class);
    private final Label status;

    private int cursor = 0;
    /** true = 次に押されたキーを割り当てる */
    private boolean capturing = false;

    public KeyConfigPane(KeyBindings binds) {
        this.binds = binds;

        root = new StackPane();
        ImageAssets.addBackdropView(root, ImageAssets.BASE_LAYER,
                WINDOW_WIDTH, WINDOW_HEIGHT, Backdrop.FAR);
        Rectangle veil = new Rectangle(WINDOW_WIDTH, WINDOW_HEIGHT,
                KowloonPalette.alpha(KowloonPalette.SHADOW, 0.85));

        Label title = new Label("KEY CONFIG");
        title.setStyle(MenuStyle.title(64, KowloonPalette.LIGHT_HEX));

        VBox panel = new VBox(18);
        panel.setAlignment(Pos.CENTER_LEFT);
        panel.setPadding(new Insets(36, 56, 36, 56));
        panel.setMaxWidth(880);
        panel.setMaxHeight(Region.USE_PREF_SIZE);
        panel.setStyle(MenuStyle.box());

        for (GameAction action : GameAction.values()) {
            Label name = new Label(action.label);
            Label value = new Label();
            nameLabels.put(action, name);
            valueLabels.put(action, value);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox line = new HBox(name, spacer, value);
            line.setAlignment(Pos.CENTER_LEFT);
            panel.getChildren().add(line);
        }

        status = new Label();
        status.setStyle(MenuStyle.hint());

        Label hint = new Label("↑↓  ·  選択      ENTER  ·  割り当て      "
                + "DELETE  ·  外す      BACKSPACE  ·  既定へ戻す      ESC  ·  戻る");
        hint.setStyle(MenuStyle.hint());

        VBox content = new VBox(32, title, panel, status, hint);
        content.setAlignment(Pos.CENTER);
        root.getChildren().addAll(veil, content);

        refresh();
    }

    public StackPane getRoot() {
        return root;
    }

    /**
     * キー入力を処理する。
     *
     * @return true なら画面を閉じる（ESC）。割り当て待機中の ESC は取り消しに使うので閉じない
     */
    public boolean handle(KeyCode code) {
        if (capturing) {
            if (code == KeyCode.ESCAPE) {
                capturing = false;
                status.setText("取り消しました");
            } else if (!binds.assign(current(), code)) {
                status.setText(KeyBindings.displayName(code)
                        + " は他の用途で使うので割り当てられません");
            } else {
                capturing = false;
                status.setText(current().label + " に "
                        + KeyBindings.displayName(code) + " を割り当てました");
            }
            refresh();
            return false;
        }

        if (code == KeyCode.ESCAPE) {
            return true;
        } else if (code == KeyCode.UP) {
            cursor = (cursor - 1 + GameAction.values().length) % GameAction.values().length;
        } else if (code == KeyCode.DOWN) {
            cursor = (cursor + 1) % GameAction.values().length;
        } else if (code == KeyCode.ENTER) {
            capturing = true;
            status.setText("割り当てたいキーを押してください（ESC で取り消し）");
        } else if (code == KeyCode.DELETE) {
            binds.clear(current());
            status.setText(current().label + " の割り当てを外しました");
        } else if (code == KeyCode.BACK_SPACE) {
            binds.resetToDefaults();
            status.setText("すべて既定の配置へ戻しました");
        }
        refresh();
        return false;
    }

    private GameAction current() {
        return GameAction.values()[cursor];
    }

    private void refresh() {
        GameAction[] actions = GameAction.values();
        for (int i = 0; i < actions.length; i++) {
            GameAction action = actions[i];
            boolean selected = i == cursor;
            String mark = selected ? "▶  " : "     ";
            nameLabels.get(action).setText(mark + action.label);
            nameLabels.get(action).setStyle(MenuStyle.value(28, selected
                    ? KowloonPalette.LIGHT_HEX
                    : KowloonPalette.rgba(KowloonPalette.LIGHT_HEX, 0.55)));

            String text = (selected && capturing) ? "＿" : binds.describe(action);
            // 未割り当ては操作できなくなるので、選んでいなくても目立つ色で出す
            String color = binds.isUnbound(action)
                    ? KowloonPalette.NEON_HEX
                    : selected ? KowloonPalette.LIGHT_HEX
                    : KowloonPalette.rgba(KowloonPalette.LIGHT_HEX, 0.55);
            valueLabels.get(action).setText(text);
            valueLabels.get(action).setStyle(MenuStyle.value(28, color));
        }
    }
}
