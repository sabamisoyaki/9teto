package tetris.view;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import tetris.ResourcePath;
import tetris.model.GameConfig;

public class ConfigPane {

    private static final Path BACKGROUND_IMAGE = ResourcePath.of("images", "base-layer-1920x1080.png");
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

        if (Files.exists(BACKGROUND_IMAGE)) {
            Image image = new Image(BACKGROUND_IMAGE.toUri().toString());
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(WINDOW_WIDTH);
            imageView.setFitHeight(WINDOW_HEIGHT);
            imageView.setEffect(new GaussianBlur(5));
            root.getChildren().add(imageView);
        } else {
            root.setStyle("-fx-background-color: black;");
        }

        Rectangle overlay = new Rectangle(WINDOW_WIDTH, WINDOW_HEIGHT, Color.rgb(10, 15, 20, 0.85));

        VBox content = new VBox(48);
        content.setAlignment(Pos.CENTER);

        Label title = new Label("CONFIG");
        title.setStyle(
            "-fx-font-size: 72px; -fx-font-weight: bold; -fx-text-fill: #f0f0f0;" +
            " -fx-font-family: 'Courier New';" +
            " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 10, 0.0, 2, 2);");

        VBox panel = new VBox(28);
        panel.setAlignment(Pos.CENTER_LEFT);
        panel.setPadding(new Insets(40, 60, 40, 60));
        panel.setMaxWidth(760);
        panel.setStyle(
            "-fx-background-color: rgba(0,0,0,0.6);" +
            " -fx-border-color: #445566; -fx-border-width: 1px;" +
            " -fx-border-radius: 5px; -fx-background-radius: 5px;");

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
        hint.setStyle(
            "-fx-font-size: 18px; -fx-text-fill: #6688aa; -fx-font-family: 'Courier New';");

        content.getChildren().addAll(title, panel, hint);
        root.getChildren().addAll(overlay, content);
    }

    private HBox makeSliderRow(String name, double initial, DoubleConsumer onChange) {
        Label label = new Label(name);
        label.setStyle(
            "-fx-font-size: 22px; -fx-text-fill: #e0e0e0; -fx-font-family: 'Courier New';");
        label.setMinWidth(220);

        Slider slider = new Slider(0.0, 1.0, initial);
        slider.setPrefWidth(360);

        Label pct = new Label(String.format("%.0f%%", initial * 100));
        pct.setStyle(
            "-fx-font-size: 20px; -fx-text-fill: #aaddff; -fx-font-family: 'Courier New';");
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
        label.setStyle(
            "-fx-font-size: 22px; -fx-text-fill: #e0e0e0; -fx-font-family: 'Courier New';");
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

    private String btnStyle(boolean on) {
        return on
            ? "-fx-font-size: 20px; -fx-font-family: 'Courier New'; -fx-min-width: 90px;" +
              " -fx-background-color: #224488; -fx-text-fill: #aaddff;" +
              " -fx-border-color: #4488cc; -fx-border-width: 1px; -fx-cursor: hand;"
            : "-fx-font-size: 20px; -fx-font-family: 'Courier New'; -fx-min-width: 90px;" +
              " -fx-background-color: #1a1a1a; -fx-text-fill: #556677;" +
              " -fx-border-color: #334455; -fx-border-width: 1px; -fx-cursor: hand;";
    }

    public StackPane getRoot() { return root; }
}
