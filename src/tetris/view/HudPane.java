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
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;

public class HudPane extends StackPane {

    private static final Path DEFAULT_BACKGROUND_IMAGE = ResourcePath.of("images", "hud-bg.png");
    private static final double PANE_WIDTH = 480;
    private static final double PANE_HEIGHT = 400;

    private final Label scoreLabel;
    private final Label linesLabel;
    private final Label levelLabel;
    private final Label rotateLabel;
    private final Label dangerLabel;
    private final Label dialogueLabel;
    private final VBox contentBox;
    // 背景画像は ImageView で持つ。setBackground だと inline style（-fx-background-color）に
    // 上書きされて画像が表示されないため、子ノードとして重ねる（NextPane と同方式）
    private final ImageView backgroundView;

    private UiSkin skin = UiSkinBank.forStep(0);
    private int lastRotateRemaining = Integer.MIN_VALUE;
    private int lastDangerStreak = Integer.MIN_VALUE;

    public HudPane() {
        setPrefSize(PANE_WIDTH, PANE_HEIGHT);
        setMinSize(PANE_WIDTH, PANE_HEIGHT);
        setMaxSize(PANE_WIDTH, PANE_HEIGHT);
        setAlignment(Pos.TOP_LEFT);

        backgroundView = new ImageView();
        backgroundView.setFitWidth(PANE_WIDTH);
        backgroundView.setFitHeight(PANE_HEIGHT);
        backgroundView.setMouseTransparent(true);

        contentBox = new VBox(15);
        contentBox.setAlignment(Pos.TOP_LEFT);
        contentBox.setPadding(new Insets(20, 30, 20, 30));

        scoreLabel = new Label("Score: 0");
        linesLabel = new Label("Lines: 0");
        levelLabel = new Label("Level: 1");
        rotateLabel = new Label("Spin in: 3 lines");
        dangerLabel = new Label("Danger: ○○○○");
        dialogueLabel = new Label();
        dialogueLabel.setWrapText(true);
        dialogueLabel.setMaxWidth(420);

        contentBox.getChildren().addAll(scoreLabel, linesLabel, levelLabel, rotateLabel, dangerLabel, dialogueLabel);
        getChildren().addAll(backgroundView, contentBox);

        applySkin(skin);
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

    /** ワールド回転までの残りライン数を表示。残り1ラインで警告色になる */
    public void updateRotateCountdown(int remaining) {
        if (remaining == lastRotateRemaining) return;
        lastRotateRemaining = remaining;

        rotateLabel.setText("Spin in: " + remaining + (remaining == 1 ? " line" : " lines"));
        applyRotateLabelStyle();
    }

    private void applyRotateLabelStyle() {
        boolean warning = lastRotateRemaining != Integer.MIN_VALUE && lastRotateRemaining <= 1;
        rotateLabel.setStyle(skin.fontStyle(22)
            + (warning ? " -fx-text-fill: #ff6666; -fx-font-weight: bold;"
                       : " -fx-text-fill: #ffcc66;"));
    }

    /** 仮ゲームオーバーの残ライフを ●○ で常時表示する */
    public void updateDangerGauge(int streak, int max) {
        if (streak == lastDangerStreak) return;
        lastDangerStreak = streak;

        StringBuilder sb = new StringBuilder("Danger: ");
        for (int i = 0; i < max; i++) sb.append(i < streak ? "●" : "○");
        dangerLabel.setText(sb.toString());
    }

    /** キャラのセリフを表示する。軽いフェードインで「喋った感」を出す */
    public void showDialogue(String text) {
        if (text == null || text.isEmpty()) return;
        dialogueLabel.setText(text);
        FadeTransition ft = new FadeTransition(Duration.millis(200), dialogueLabel);
        ft.setFromValue(0.3);
        ft.setToValue(1.0);
        ft.play();
    }

    // ============================================================
    //  スキン適用（ワールドローテートごとに呼ばれる）
    //  見た目の定義は UiSkinBank に集約されている
    // ============================================================
    public void applySkin(UiSkin newSkin) {
        this.skin = newSkin;

        setStyle(skin.panelStyle());

        scoreLabel.setStyle(skin.fontStyle(22) + " -fx-text-fill: " + skin.theme.textColor + ";");
        linesLabel.setStyle(skin.fontStyle(22) + " -fx-text-fill: " + skin.theme.textColor + ";");
        levelLabel.setStyle(skin.fontStyle(22) + " -fx-text-fill: " + skin.theme.accentColor + "; -fx-font-weight: bold;");
        dangerLabel.setStyle(skin.fontStyle(22) + " -fx-text-fill: #ff8888;");
        dialogueLabel.setStyle(skin.fontStyle(18) + " -fx-line-spacing: 5px;"
            + " -fx-text-fill: " + skin.theme.textColor + ";");
        applyRotateLabelStyle();

        Path imagePath = skin.hudBgImage != null ? skin.hudBgImage : DEFAULT_BACKGROUND_IMAGE;
        if (imagePath != null && Files.exists(imagePath)) {
            backgroundView.setImage(new Image(imagePath.toUri().toString()));
        } else {
            backgroundView.setImage(null);
        }
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
