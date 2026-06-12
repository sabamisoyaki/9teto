package tetris.view;

import java.nio.file.Files;
import java.nio.file.Path;
import tetris.ResourcePath;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;
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
import javafx.scene.layout.StackPane;

public class HudPane extends StackPane {

    private static final Path DEFAULT_BACKGROUND_IMAGE = ResourcePath.of("images", "hud-bg.png");

    private final Label scoreLabel;
    private final Label linesLabel;
    private final Label levelLabel;
    private final Label dialogueLabel;
    private final VBox contentBox;

    private int lastDialogueScore = -1;
    private int lastDialogueLines = -1;

    public HudPane() {
        setPrefSize(480, 280);
        setMinSize(480, 280);
        setMaxSize(480, 280);
        setAlignment(Pos.TOP_LEFT);
        setStyle("-fx-background-color: rgba(10, 10, 10, 0.75); -fx-border-color: #445566; -fx-border-width: 1px; -fx-border-radius: 5px; -fx-background-radius: 5px;");

        contentBox = new VBox(15);
        contentBox.setAlignment(Pos.TOP_LEFT);
        contentBox.setPadding(new Insets(20, 30, 20, 30));

        scoreLabel = new Label("Score: 0");
        linesLabel = new Label("Lines: 0");
        levelLabel = new Label("Level: 1");
        dialogueLabel = new Label();

        scoreLabel.setStyle("-fx-font-size: 22px; -fx-text-fill: #e0e0e0; -fx-font-family: 'Courier New';");
        linesLabel.setStyle("-fx-font-size: 22px; -fx-text-fill: #e0e0e0; -fx-font-family: 'Courier New';");
        levelLabel.setStyle("-fx-font-size: 22px; -fx-text-fill: #e0e0e0; -fx-font-family: 'Courier New';");
        dialogueLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #cccccc; -fx-font-family: 'Courier New'; -fx-line-spacing: 5px;");
        dialogueLabel.setWrapText(true);
        dialogueLabel.setMaxWidth(420);

        contentBox.getChildren().addAll(scoreLabel, linesLabel, levelLabel, dialogueLabel);
        getChildren().add(contentBox);

        applyBackgroundImage(DEFAULT_BACKGROUND_IMAGE);
    }

    public void updateScore(int score) {
        scoreLabel.setText("Score: " + score);
    }

    public void updateLines(int lines) {
        linesLabel.setText("Lines: " + lines);
    }

    public void updateLevel(int level) {
        levelLabel.setText("Level: " + level);
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

    // ============================================================
    //  テーマ適用（ワールドローテートごとに呼ばれる）
    //  UiTheme の各フィールドを使って見た目を自由にカスタマイズ
    // ============================================================
    public void applyTheme(UiTheme theme) {
        // パネル全体のボーダー・背景
        setStyle(
            "-fx-background-color: " + theme.bgColor + "; " +
            "-fx-border-color: " + theme.borderColor + "; " +
            "-fx-border-width: 2px; " +
            "-fx-border-radius: 5px; " +
            "-fx-background-radius: 5px;");

        // スコア／ライン／レベルラベルの文字色
        String baseStyle = "-fx-font-size: 22px; -fx-font-family: 'Courier New';";
        scoreLabel.setStyle(baseStyle + " -fx-text-fill: " + theme.textColor + ";");
        linesLabel.setStyle(baseStyle + " -fx-text-fill: " + theme.textColor + ";");
        levelLabel.setStyle(baseStyle + " -fx-text-fill: " + theme.accentColor + "; -fx-font-weight: bold;");
        dialogueLabel.setStyle(
            "-fx-font-size: 18px; -fx-font-family: 'Courier New'; -fx-line-spacing: 5px;" +
            " -fx-text-fill: " + theme.textColor + ";");

        // ここに追加カスタマイズを書く
        // 例: backgroundImage を theme.bgImagePath で差し替えるなど
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

    public void showScorePopup(int addedScore) {
        Label popup = new Label("+" + addedScore);
        popup.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: #ffd700; -fx-font-family: 'Courier New'; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 5, 0.0, 1, 1);");

        getChildren().add(popup);
        StackPane.setAlignment(popup, Pos.TOP_RIGHT);
        StackPane.setMargin(popup, new Insets(20, 30, 0, 0));

        TranslateTransition tt = new TranslateTransition(Duration.seconds(1), popup);
        tt.setByY(-50);

        FadeTransition ft = new FadeTransition(Duration.seconds(1), popup);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);

        ParallelTransition pt = new ParallelTransition(tt, ft);
        pt.setOnFinished(e -> getChildren().remove(popup));
        pt.play();
    }

    public void showTSpinPopup(boolean mini) {
        showBigPopup(
            mini ? "T-SPIN MINI!" : "T-SPIN!",
            mini ? "#aaaaff" : "#ff44ff"
        );
    }

    public void showRenPopup(int combo) {
        showBigPopup(combo + " REN!", "#ffaa44");
    }

    private void showBigPopup(String text, String colorHex) {
        Label popup = new Label(text);
        popup.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: " + colorHex + "; " +
                       "-fx-font-family: 'Courier New'; " +
                       "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.9), 8, 0.0, 0, 0);");

        getChildren().add(popup);
        StackPane.setAlignment(popup, Pos.CENTER);

        ScaleTransition scale = new ScaleTransition(Duration.millis(200), popup);
        scale.setFromX(0.4);
        scale.setFromY(0.4);
        scale.setToX(1.0);
        scale.setToY(1.0);

        FadeTransition ft = new FadeTransition(Duration.millis(700), popup);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setDelay(Duration.millis(500));

        ParallelTransition pt = new ParallelTransition(scale, ft);
        pt.setOnFinished(e -> getChildren().remove(popup));
        pt.play();
    }
}
