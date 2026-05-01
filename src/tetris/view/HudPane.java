package tetris.view;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.VBox;

public class HudPane extends VBox {

    private static final Path DEFAULT_BACKGROUND_IMAGE = Paths.get("images", "hud-bg.png");

    private final Label scoreLabel;
    private final Label linesLabel;
    private final Label dialogueLabel;

    private int lastDialogueScore = -1;
    private int lastDialogueLines = -1;

    public HudPane() {
        setPrefSize(480, 280);
        setMinSize(480, 280);
        setMaxSize(480, 280);
        setAlignment(Pos.TOP_LEFT);
        setSpacing(20);
        setPadding(new Insets(20, 30, 20, 30));
        setStyle("-fx-background-color: rgba(10, 10, 10, 0.75); -fx-border-color: #445566; -fx-border-width: 1px; -fx-border-radius: 5px; -fx-background-radius: 5px;");

        scoreLabel = new Label("Score: 0");
        linesLabel = new Label("Lines: 0");
        dialogueLabel = new Label();

        scoreLabel.setStyle("-fx-font-size: 22px; -fx-text-fill: #e0e0e0; -fx-font-family: 'Courier New';");
        linesLabel.setStyle("-fx-font-size: 22px; -fx-text-fill: #e0e0e0; -fx-font-family: 'Courier New';");
        dialogueLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #cccccc; -fx-font-family: 'Courier New'; -fx-line-spacing: 5px;");
        dialogueLabel.setWrapText(true);
        dialogueLabel.setMaxWidth(420);

        getChildren().addAll(scoreLabel, linesLabel, dialogueLabel);

        applyBackgroundImage(DEFAULT_BACKGROUND_IMAGE);
    }

    public void updateScore(int score) {
        scoreLabel.setText("Score: " + score);
    }

    public void updateLines(int lines) {
        linesLabel.setText("Lines: " + lines);
    }

    public void updateDialogue(int score, int lines) {
        if (score == lastDialogueScore && lines == lastDialogueLines) return;
        lastDialogueScore = score;
        lastDialogueLines = lines;

        String text = """
                現在の点数は %d 点。まだ足りないわね。
                累計ラインは %d。次はどうする？
                次のミノはこれよ。下にも表示してるけど。
                焦らず積んでいきましょ。""".formatted(score, lines);
        dialogueLabel.setText(text);
    }

    private void applyBackgroundImage(Path imagePath) {
        if (imagePath == null || !Files.exists(imagePath)) {
            setBackground(null);
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
