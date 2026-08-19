package tetris.view;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import tetris.model.GameConfig;

public class ConfigPane {

    private static final int WINDOW_WIDTH  = 1920;
    private static final int WINDOW_HEIGHT = 1080;

    private final StackPane root;

    public ConfigPane(
            GameConfig config,
            DoubleConsumer onBgmVolumeChange,
            DoubleConsumer onSeVolumeChange,
            Consumer<Boolean> onBgmToggle,
            Consumer<Boolean> onSeToggle) {

        root = new StackPane();

        ImageAssets.addBackdropView(root, ImageAssets.BASE_LAYER, WINDOW_WIDTH, WINDOW_HEIGHT, Backdrop.FAR);

        Rectangle overlay = new Rectangle(WINDOW_WIDTH, WINDOW_HEIGHT,
                KowloonPalette.alpha(KowloonPalette.SHADOW, 0.85));

        VBox content = new VBox(48);
        content.setAlignment(Pos.CENTER);

        Label title = new Label("CONFIG");
        title.setStyle(MenuStyle.title(72, KowloonPalette.LIGHT_HEX));

        VBox panel = new VBox(28);
        panel.setAlignment(Pos.CENTER_LEFT);
        panel.setPadding(new Insets(40, 60, 40, 60));
        panel.setMaxWidth(760);
        panel.setStyle(MenuStyle.box());

        panel.getChildren().addAll(
            makeSliderRow("BGM Volume", config.getBgmVolume(), v -> {
                config.setBgmVolume(v);
                onBgmVolumeChange.accept(v);
            }),
            makeSliderRow("SE  Volume", config.getSeVolume(), v -> {
                config.setSeVolume(v);
                onSeVolumeChange.accept(v);
            }),
            makeToggleRow("BGM", config.isBgmEnabled(), b -> {
                config.setBgmEnabled(b);
                onBgmToggle.accept(b);
            }),
            makeToggleRow("SE", config.isSeEnabled(), b -> {
                config.setSeEnabled(b);
                onSeToggle.accept(b);
            })
        );

        Label hint = new Label("ESC  Back");
        hint.setStyle(MenuStyle.hint());

        content.getChildren().addAll(title, panel, hint);
        root.getChildren().addAll(overlay, content);
    }

    private HBox makeSliderRow(String name, double initial, DoubleConsumer onChange) {
        Label label = new Label(name);
        label.setStyle(MenuStyle.value(22, KowloonPalette.LIGHT_HEX));
        label.setMinWidth(220);

        Slider slider = new Slider(0.0, 1.0, initial);
        slider.setPrefWidth(360);
        // Slider の track / thumb は子要素なので inline style から直接触れない。
        // modena が参照するルック色（-fx-base / -fx-control-inner-background）を
        // パレット色に差し替えて、既定の青いつまみを消す
        slider.setStyle("-fx-base: " + KowloonPalette.RUST_HEX + ";"
            + " -fx-control-inner-background: " + KowloonPalette.SHADOW_HEX + ";");

        Label pct = new Label(String.format("%.0f%%", initial * 100));
        pct.setStyle(MenuStyle.value(20, KowloonPalette.RUST_HEX));
        pct.setMinWidth(55);

        slider.valueProperty().addListener((obs, old, n) -> {
            pct.setText(String.format("%.0f%%", n.doubleValue() * 100));
            onChange.accept(n.doubleValue());
        });

        HBox row = new HBox(24, label, slider, pct);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox makeToggleRow(String name, boolean initial, Consumer<Boolean> onChange) {
        Label label = new Label(name);
        label.setStyle(MenuStyle.value(22, KowloonPalette.LIGHT_HEX));
        label.setMinWidth(220);

        ToggleButton btn = new ToggleButton(initial ? "ON" : "OFF");
        btn.setSelected(initial);
        btn.setStyle(btnStyle(initial));
        btn.setOnAction(e -> {
            boolean on = btn.isSelected();
            btn.setText(on ? "ON" : "OFF");
            btn.setStyle(btnStyle(on));
            onChange.accept(on);
        });

        HBox row = new HBox(24, label, btn);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /** ON / OFF トグル。ON はアクセント（錆）、OFF は沈める。色はパレットのみ */
    private String btnStyle(boolean on) {
        String base = "-fx-font-size: 20px; -fx-font-family: 'Courier New'; -fx-min-width: 90px;"
            + " -fx-border-width: 1px; -fx-cursor: hand;";
        return on
            ? base
              + " -fx-background-color: " + KowloonPalette.rgba(KowloonPalette.RUST_HEX, 0.55) + ";"
              + " -fx-text-fill: " + KowloonPalette.LIGHT_HEX + ";"
              + " -fx-border-color: " + KowloonPalette.RUST_HEX + ";"
            : base
              + " -fx-background-color: " + KowloonPalette.rgba(KowloonPalette.SHADOW_HEX, 0.85) + ";"
              + " -fx-text-fill: " + KowloonPalette.rgba(KowloonPalette.LIGHT_HEX, 0.45) + ";"
              + " -fx-border-color: " + KowloonPalette.rgba(KowloonPalette.LIGHT_HEX, 0.28) + ";";
    }

    public StackPane getRoot() { return root; }
}
