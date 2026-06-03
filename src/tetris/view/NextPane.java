package tetris.view;

import java.nio.file.Files;
import java.nio.file.Path;

import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import tetris.ResourcePath;

public class NextPane extends StackPane {

    private static final Path DEFAULT_BACKGROUND_IMAGE = ResourcePath.of("images", "next-bg.png");

    private final Canvas nextCanvas;
    private final int nextCellSize;
    private final ImageView backgroundView;

    public NextPane(double width, double height) {
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

        getChildren().addAll(backgroundView, nextCanvas);

        loadBackgroundImage(DEFAULT_BACKGROUND_IMAGE);
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
