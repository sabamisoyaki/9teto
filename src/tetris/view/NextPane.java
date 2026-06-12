package tetris.view;

import java.nio.file.Files;
import java.nio.file.Path;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import tetris.ResourcePath;

public class NextPane extends StackPane {

    private static final Path DEFAULT_BACKGROUND_IMAGE = ResourcePath.of("images", "next-bg.png");

    private final Canvas nextCanvas;
    private final int nextCellSize;
    private final ImageView backgroundView;
    private final Label titleLabel;

    public NextPane(double width, double height) {
        this(width, height, "NEXT");
    }

    public NextPane(double width, double height, String title) {
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: rgba(10, 10, 10, 0.75); -fx-border-color: #445566; -fx-border-width: 1px; -fx-border-radius: 5px; -fx-background-radius: 5px;");

        backgroundView = new ImageView();
        backgroundView.setFitWidth(width);
        backgroundView.setFitHeight(height);

        nextCanvas = new Canvas();
        nextCanvas.setWidth(width);
        nextCanvas.setHeight(height);
        nextCellSize = (int) Math.min(nextCanvas.getWidth() / 4, nextCanvas.getHeight() / 4);

        titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #dcecff; -fx-font-family: 'Courier New'; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.9), 4, 0.0, 1, 1);");
        StackPane.setAlignment(titleLabel, Pos.TOP_LEFT);
        StackPane.setMargin(titleLabel, new Insets(8, 0, 0, 12));

        getChildren().addAll(backgroundView, nextCanvas, titleLabel);

        loadBackgroundImage(DEFAULT_BACKGROUND_IMAGE);
    }

    // ============================================================
    //  テーマ適用（ワールドローテートごとに呼ばれる）
    // ============================================================
    public void applyTheme(UiTheme theme) {
        setStyle(
            "-fx-background-color: " + theme.bgColor + "; " +
            "-fx-border-color: " + theme.borderColor + "; " +
            "-fx-border-width: 2px; " +
            "-fx-border-radius: 5px; " +
            "-fx-background-radius: 5px;");

        titleLabel.setStyle(
            "-fx-font-size: 20px; -fx-font-weight: bold; -fx-font-family: 'Courier New';" +
            " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.9), 4, 0.0, 1, 1);" +
            " -fx-text-fill: " + theme.accentColor + ";");

        // ここに追加カスタマイズを書く
    }

    public Canvas getNextCanvas() {
        return nextCanvas;
    }

    public int getNextCellSize() {
        return nextCellSize;
    }

    public void loadBackgroundImage(Path imagePath) {
        if (imagePath == null || !Files.exists(imagePath)) {
            backgroundView.setImage(null);
            return;
        }

        Image image = new Image(imagePath.toUri().toString());
        backgroundView.setImage(image);
    }
}
