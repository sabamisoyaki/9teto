package tetris.view;

import java.nio.file.Files;
import java.nio.file.Path;
import tetris.ResourcePath;

import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

public class CharacterPane extends StackPane {

    private static final Path DEFAULT_CHARACTER_IMAGE = ResourcePath.of("images", "character.png");
    private static final Path ROTATE_CHARACTER_IMAGE_1 = ResourcePath.of("images", "character-rotate-1.png");
    private static final Path ROTATE_CHARACTER_IMAGE_2 = ResourcePath.of("images", "character-rotate-2.png");
    private static final Path ROTATE_CHARACTER_IMAGE_3 = ResourcePath.of("images", "character-rotate-3.png");
    private static final Path CHARACTER_BG_IMAGE = ResourcePath.of("images", "character-bg.png");
    private static final double PANE_WIDTH = 480;
    private static final double PANE_HEIGHT = 1080;
    private static final double CHARACTER_SIZE = 1080;

    private final ImageView characterView;

    public CharacterPane() {
        setPrefSize(PANE_WIDTH, PANE_HEIGHT);
        setMinSize(PANE_WIDTH, PANE_HEIGHT);
        setMaxSize(PANE_WIDTH, PANE_HEIGHT);
        setClip(new Rectangle(PANE_WIDTH, PANE_HEIGHT));

        characterView = new ImageView();
        characterView.setFitWidth(CHARACTER_SIZE);
        characterView.setFitHeight(CHARACTER_SIZE);
        characterView.setPreserveRatio(true);
        characterView.setSmooth(true);
        characterView.setTranslateX(300);

        getChildren().add(characterView);
        StackPane.setAlignment(characterView, Pos.BOTTOM_RIGHT);

        applyCharacterImage(DEFAULT_CHARACTER_IMAGE);
        applyBackgroundImage(CHARACTER_BG_IMAGE);
    }

    public ImageView getCharacterView() {
        return characterView;
    }

    public void setCharacterSize(double width, double height) {
        characterView.setFitWidth(width);
        characterView.setFitHeight(height);
    }

    public void updateCharacterForWorldRotateStep(int rotateStep) {
        Path imagePath = switch (rotateStep) {
            case 1 -> ROTATE_CHARACTER_IMAGE_1;
            case 2 -> ROTATE_CHARACTER_IMAGE_2;
            case 3 -> ROTATE_CHARACTER_IMAGE_3;
            default -> DEFAULT_CHARACTER_IMAGE;
        };

        if (!Files.exists(imagePath)) {
            imagePath = DEFAULT_CHARACTER_IMAGE;
        }

        applyCharacterImage(imagePath);
    }

    public void loadCharacterImage(Path imagePath) {
        applyCharacterImage(imagePath);
    }

    private void applyCharacterImage(Path imagePath) {
        if (imagePath == null || !Files.exists(imagePath)) {
            characterView.setImage(null);
            return;
        }

        Image image = new Image(imagePath.toUri().toString());
        characterView.setImage(image);
    }

    private void applyBackgroundImage(Path imagePath) {
        if (imagePath == null || !Files.exists(imagePath)) {
            return;
        }

        Image image = new Image(imagePath.toUri().toString());
        BackgroundSize size = new BackgroundSize(100, 100, true, true, false, true);
        BackgroundImage backgroundImage = new BackgroundImage(
                image,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                size);
        setBackground(new Background(backgroundImage));
    }
}
