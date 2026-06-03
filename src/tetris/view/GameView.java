package tetris.view;

import java.nio.file.Files;
import java.nio.file.Path;
import tetris.ResourcePath;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class GameView {

    private static final Path DEFAULT_BACKGROUND_IMAGE = ResourcePath.of("images", "base-layer-1920x1080.png");

    private static final double WINDOW_WIDTH  = 1920;
    private static final double WINDOW_HEIGHT = 1080;
    private static final double LEFT_WIDTH    = 960;
    private static final double SIDE_WIDTH    = 960;
    private static final double PLAY_FIELD_SIZE = 840;
    private static final double NEXT_HEIGHT     = 168; // 7:1 vertical split (840:168)
    private static final double PREVIEW_WIDTH   = PLAY_FIELD_SIZE / 2;

    private final BorderPane root;
    private final NextPane holdPane;
    private final NextPane nextPane;
    private final DeviceFramePane playFieldPane;
    private final CharacterPane characterPane;
    private final HudPane hudPane;

    public GameView() {
        this.root = new BorderPane();

        this.playFieldPane = new DeviceFramePane(PLAY_FIELD_SIZE);
        this.holdPane = new NextPane(PREVIEW_WIDTH, NEXT_HEIGHT, "HOLD (H)");
        this.nextPane = new NextPane(PREVIEW_WIDTH, NEXT_HEIGHT, "NEXT");
        this.characterPane = new CharacterPane();
        this.hudPane = new HudPane();

        HBox previewRow = new HBox(holdPane, nextPane);
        previewRow.setAlignment(Pos.TOP_LEFT);

        VBox leftColumn = new VBox(playFieldPane, previewRow);
        leftColumn.setAlignment(Pos.TOP_LEFT);
        leftColumn.setPadding(new Insets(20, 0, 0, 20));
        leftColumn.setPrefSize(LEFT_WIDTH, WINDOW_HEIGHT);
        leftColumn.setMinSize(LEFT_WIDTH, WINDOW_HEIGHT);
        leftColumn.setMaxSize(LEFT_WIDTH, WINDOW_HEIGHT);

        HBox sideArea = new HBox(characterPane, hudPane);
        sideArea.setAlignment(Pos.TOP_LEFT);
        sideArea.setPrefSize(SIDE_WIDTH, WINDOW_HEIGHT);
        sideArea.setMinSize(SIDE_WIDTH, WINDOW_HEIGHT);
        sideArea.setMaxSize(SIDE_WIDTH, WINDOW_HEIGHT);

        root.setLeft(leftColumn);
        root.setRight(sideArea);
        loadBaseLayerImage(DEFAULT_BACKGROUND_IMAGE);
    }

    private void loadBaseLayerImage(Path imagePath) {
        if (imagePath == null || !Files.exists(imagePath)) {
            root.setStyle("-fx-background-color: black;");
            return;
        }
        Image image = new Image(imagePath.toUri().toString());
        BackgroundSize backgroundSize = new BackgroundSize(
                WINDOW_WIDTH, WINDOW_HEIGHT, false, false, false, false);
        BackgroundImage backgroundImage = new BackgroundImage(
                image,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                backgroundSize);
        root.setBackground(new Background(backgroundImage));
    }

    public BorderPane getRoot()                  { return root; }
    public NextPane getHoldPane()                { return holdPane; }
    public NextPane getNextPane()                { return nextPane; }
    public DeviceFramePane getPlayFieldPane()    { return playFieldPane; }
    public CharacterPane getCharacterPane()      { return characterPane; }
    public HudPane getHudPane()                  { return hudPane; }
}
