package tetris.view;

import java.nio.file.Files;
import java.nio.file.Path;
import tetris.ResourcePath;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class DeviceFramePane extends Pane {

    private static final Path DEFAULT_BACKGROUND_IMAGE = ResourcePath.of("images", "playfield-bg.png");

    private final Canvas playfieldCanvas;
    private final ImageView backgroundView;
    private final Rectangle flashOverlay;
    // PINCH 警告用。テーマフラッシュ（flashOverlay）と同時発生するため別レイヤーに分ける
    private final Rectangle dangerOverlay;
    private final Rectangle frame;

    public DeviceFramePane(double frameSize) {
        setPrefSize(frameSize, frameSize);
        setMinSize(frameSize, frameSize);
        setMaxSize(frameSize, frameSize);

        backgroundView = new ImageView();
        backgroundView.setFitWidth(frameSize);
        backgroundView.setFitHeight(frameSize);

        frame = new Rectangle(frameSize, frameSize);
        frame.setFill(Color.rgb(10, 10, 10, 0.6));
        frame.setStroke(Color.web("#556677"));
        frame.setStrokeWidth(2);

        playfieldCanvas = new Canvas();
        playfieldCanvas.setWidth(frameSize);
        playfieldCanvas.setHeight(frameSize);
        playfieldCanvas.setLayoutX(0);
        playfieldCanvas.setLayoutY(0);

        flashOverlay = new Rectangle(frameSize, frameSize, Color.WHITE);
        flashOverlay.setOpacity(0);
        flashOverlay.setMouseTransparent(true);

        dangerOverlay = new Rectangle(frameSize, frameSize, Color.rgb(255, 40, 40));
        dangerOverlay.setOpacity(0);
        dangerOverlay.setMouseTransparent(true);

        getChildren().addAll(backgroundView, frame, playfieldCanvas, flashOverlay, dangerOverlay);

        applyBackgroundImage(DEFAULT_BACKGROUND_IMAGE);
    }

    public Canvas getPlayfieldCanvas() {
        return playfieldCanvas;
    }

    public void loadBackgroundImage(Path imagePath) {
        applyBackgroundImage(imagePath);
    }

    private void applyBackgroundImage(Path imagePath) {
        if (imagePath == null || !Files.exists(imagePath)) {
            backgroundView.setImage(null);
            return;
        }

        Image image = new Image(imagePath.toUri().toString());
        backgroundView.setImage(image);
    }

    public void triggerFlash(int lines) {
        if (lines <= 0) return;
        double intensity = Math.min(1.0, lines * 0.25);
        flashOverlay.setFill(Color.WHITE);
        flashOverlay.setOpacity(intensity);
        FadeTransition ft = new FadeTransition(Duration.millis(300), flashOverlay);
        ft.setFromValue(intensity);
        ft.setToValue(0);
        ft.play();
    }

    /** 仮ゲームオーバー時などの警告用: 画面全体を赤くフラッシュさせる */
    public void triggerDangerFlash() {
        dangerOverlay.setOpacity(0.45);
        FadeTransition ft = new FadeTransition(Duration.millis(500), dangerOverlay);
        ft.setFromValue(0.45);
        ft.setToValue(0);
        ft.play();
    }

    /** プレイフィールド上に流れるスコア／警告ポップアップを表示する */
    public void spawnScorePopup(String text, Color color) {
        Label popup = new Label(text);
        popup.setStyle("-fx-font-size: 42px; -fx-font-weight: bold;"
            + " -fx-text-fill: " + toWebColor(color) + ";"
            + " -fx-font-family: 'Courier New';"
            + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.9), 6, 0.0, 2, 2);");
        popup.setLayoutX(getPrefWidth() / 2 - 120);
        popup.setLayoutY(getPrefHeight() * 0.35);
        popup.setMouseTransparent(true);
        getChildren().add(popup);

        TranslateTransition move = new TranslateTransition(Duration.millis(900), popup);
        move.setByY(-90);
        FadeTransition fade = new FadeTransition(Duration.millis(900), popup);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        ParallelTransition anim = new ParallelTransition(move, fade);
        anim.setOnFinished(e -> getChildren().remove(popup)); // リーク防止: 必ず remove
        anim.play();
    }

    private static String toWebColor(Color c) {
        return String.format("#%02x%02x%02x",
            (int) Math.round(c.getRed() * 255),
            (int) Math.round(c.getGreen() * 255),
            (int) Math.round(c.getBlue() * 255));
    }

    // ============================================================
    //  テーマ適用（ワールドローテートごとに呼ばれる）
    // ============================================================
    public void applyTheme(UiTheme theme) {
        // 枠線の色をテーマのアクセントカラーへ
        frame.setStroke(Color.web(theme.borderColor));

        // テーマカラーでフラッシュ演出
        flashOverlay.setFill(theme.flashColor);
        flashOverlay.setOpacity(0.35);
        FadeTransition ft = new FadeTransition(Duration.millis(400), flashOverlay);
        ft.setFromValue(0.35);
        ft.setToValue(0.0);
        ft.play();

        // ゲームボード全体の不透明度をテーマに合わせてフェード
        Timeline opacityAnim = new Timeline(
            new KeyFrame(Duration.ZERO,        new KeyValue(opacityProperty(), getOpacity())),
            new KeyFrame(Duration.millis(500),  new KeyValue(opacityProperty(), theme.boardOpacity))
        );
        opacityAnim.play();
    }

    public void triggerShake() {
        Timeline shake = new Timeline(
            new KeyFrame(Duration.ZERO,        new KeyValue(translateXProperty(), 0.0)),
            new KeyFrame(Duration.millis(40),  new KeyValue(translateXProperty(), 9.0)),
            new KeyFrame(Duration.millis(80),  new KeyValue(translateXProperty(), -9.0)),
            new KeyFrame(Duration.millis(120), new KeyValue(translateXProperty(), 7.0)),
            new KeyFrame(Duration.millis(160), new KeyValue(translateXProperty(), -7.0)),
            new KeyFrame(Duration.millis(200), new KeyValue(translateXProperty(), 4.0)),
            new KeyFrame(Duration.millis(240), new KeyValue(translateXProperty(), -4.0)),
            new KeyFrame(Duration.millis(290), new KeyValue(translateXProperty(), 0.0))
        );
        shake.play();
    }
}
