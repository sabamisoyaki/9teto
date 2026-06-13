package tetris.view;

import java.nio.file.Files;
import java.nio.file.Path;
import tetris.ResourcePath;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
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
    //  スキン適用（ワールドローテートごとに呼ばれる）
    //  演出は持たず見た目の切替のみ。回転演出は playWorldRotateTransition が担当
    // ============================================================
    public void applySkin(UiSkin skin) {
        frame.setStroke(Color.web(skin.theme.borderColor));

        if (skin.playfieldBgImage != null) {
            applyBackgroundImage(skin.playfieldBgImage);
        }

        // ゲームボード全体の不透明度をスキンに合わせてフェード
        Timeline opacityAnim = new Timeline(
            new KeyFrame(Duration.ZERO,        new KeyValue(opacityProperty(), getOpacity())),
            new KeyFrame(Duration.millis(500),  new KeyValue(opacityProperty(), skin.theme.boardOpacity))
        );
        opacityAnim.play();
    }

    // ============================================================
    //  ワールド回転トランジション
    //  呼び出し時点の Canvas（＝回転前の盤面が残っている）をスナップショットして
    //  重ね、実際の盤面回転と同じ 90°CW に回しながら新盤面へクロスフェードする。
    //  必ず「このフレームの drawAll より前」に呼ぶこと。
    // ============================================================
    private ParallelTransition rotateTransition = null;
    private ImageView rotateSnapshotView = null;

    public void playWorldRotateTransition(UiSkin newSkin) {
        finishRotateTransition();

        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        WritableImage snap = playfieldCanvas.snapshot(sp, null);

        ImageView old = new ImageView(snap);
        old.setMouseTransparent(true);
        // フラッシュ・警告オーバーレイより下、Canvas より上に挿入
        getChildren().add(getChildren().indexOf(flashOverlay), old);
        playfieldCanvas.setOpacity(0.0);

        Duration d = Duration.millis(450);

        RotateTransition rot = new RotateTransition(d, old);
        rot.setByAngle(90); // Board.rotateClockwise と同じ右回転
        rot.setInterpolator(Interpolator.EASE_BOTH);

        FadeTransition oldFade = new FadeTransition(d.multiply(0.6), old);
        oldFade.setFromValue(1.0);
        oldFade.setToValue(0.0);
        oldFade.setDelay(d.multiply(0.4));

        FadeTransition canvasFade = new FadeTransition(d, playfieldCanvas);
        canvasFade.setFromValue(0.0);
        canvasFade.setToValue(1.0);

        // 新スキンのテーマカラーでフラッシュ
        flashOverlay.setFill(newSkin.theme.flashColor);
        FadeTransition flash = new FadeTransition(Duration.millis(400), flashOverlay);
        flash.setFromValue(0.35);
        flash.setToValue(0.0);

        ParallelTransition all = new ParallelTransition(rot, oldFade, canvasFade, flash);
        all.setOnFinished(e -> finishRotateTransition());
        rotateTransition = all;
        rotateSnapshotView = old;
        all.play();
    }

    /** 実行中の回転トランジションを即座に終了状態へ戻す（連続回転時の多重防止） */
    private void finishRotateTransition() {
        if (rotateTransition == null) return;
        rotateTransition.stop();
        rotateTransition = null;
        if (rotateSnapshotView != null) {
            getChildren().remove(rotateSnapshotView);
            rotateSnapshotView = null;
        }
        playfieldCanvas.setOpacity(1.0);
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
