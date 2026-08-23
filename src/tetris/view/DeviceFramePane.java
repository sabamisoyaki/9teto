package tetris.view;

import java.nio.file.Path;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;

import tetris.view.UiLayout.Style;

public class DeviceFramePane extends Pane {

    private final Canvas playfieldCanvas;
    private final ImageView backgroundView;
    private final Rectangle flashOverlay;
    // PINCH 警告用。テーマフラッシュ（flashOverlay）と同時発生するため別レイヤーに分ける
    private final Rectangle dangerOverlay;
    private final Rectangle frame;
    // 枠の四隅に置く鉤括弧。角だけを強調すると、細い枠線でも盤面の輪郭がはっきりする
    private final Polyline[] corners = new Polyline[4];

    /** 四隅の鉤括弧の腕の長さ */
    private static final double CORNER_ARM = 26;

    public DeviceFramePane(double frameSize) {
        setPrefSize(frameSize, frameSize);
        setMinSize(frameSize, frameSize);
        setMaxSize(frameSize, frameSize);

        backgroundView = new ImageView();
        backgroundView.setFitWidth(frameSize);
        backgroundView.setFitHeight(frameSize);

        frame = new Rectangle(frameSize, frameSize);
        frame.setFill(KowloonPalette.alpha(KowloonPalette.SHADOW, 0.6));
        frame.setStroke(KowloonPalette.RUST);
        frame.setStrokeWidth(2);

        playfieldCanvas = new Canvas();
        playfieldCanvas.setWidth(frameSize);
        playfieldCanvas.setHeight(frameSize);
        playfieldCanvas.setLayoutX(0);
        playfieldCanvas.setLayoutY(0);

        // 初期塗りは蛍光灯色。以降はスキンの flashColor で上書きされる
        flashOverlay = new Rectangle(frameSize, frameSize, KowloonPalette.LIGHT);
        flashOverlay.setOpacity(0);
        flashOverlay.setMouseTransparent(true);

        dangerOverlay = new Rectangle(frameSize, frameSize, KowloonPalette.NEON);
        dangerOverlay.setOpacity(0);
        dangerOverlay.setMouseTransparent(true);

        for (int i = 0; i < corners.length; i++) {
            corners[i] = new Polyline();
            corners[i].setStrokeWidth(3);
            corners[i].setMouseTransparent(true);
        }

        getChildren().addAll(backgroundView, frame, playfieldCanvas);
        getChildren().addAll(corners);
        getChildren().addAll(flashOverlay, dangerOverlay);

        alignFrame(0, 0, frameSize, frameSize);
        ImageAssets.setBackdrop(backgroundView, ImageAssets.PLAYFIELD_BG_DEFAULT, null, Backdrop.FIELD);
    }

    /**
     * 枠と四隅の鉤括弧を、実際に描かれる盤面の矩形へ揃える。
     *
     * Canvas は正方形でもセルサイズの切り捨てで盤面が一回り小さく、そのまま Canvas の
     * 外周に枠を引くと数 px ぶん盤面から浮く。Render が確定した後に一度だけ呼ぶこと。
     */
    public void alignFrame(Render renderer) {
        alignFrame(renderer.getBoardX(), renderer.getBoardY(),
                renderer.getBoardWidth(), renderer.getBoardHeight());
    }

    private void alignFrame(double x, double y, double w, double h) {
        frame.setX(x);
        frame.setY(y);
        frame.setWidth(w);
        frame.setHeight(h);

        // 左上・右上・右下・左下の順。各コーナーは「腕 → 角 → 腕」の 3 点で描く
        setCorner(corners[0], x, y,               1,  1);
        setCorner(corners[1], x + w, y,          -1,  1);
        setCorner(corners[2], x + w, y + h,      -1, -1);
        setCorner(corners[3], x, y + h,           1, -1);
    }

    private static void setCorner(Polyline line, double cx, double cy, double dx, double dy) {
        line.getPoints().setAll(
            cx, cy + dy * CORNER_ARM,
            cx, cy,
            cx + dx * CORNER_ARM, cy);
    }

    public Canvas getPlayfieldCanvas() {
        return playfieldCanvas;
    }

    /**
     * 配置定義の様式を適用する。OVERLAY では枠線と四隅の鉤括弧を消す。
     * 構図ラフの盤面は枠を持たない「絵に開いた窓」で、装飾を足すと
     * 立ち絵の上に別のパネルが乗ったように見えてしまうため。
     */
    public void applyStyle(Style newStyle) {
        boolean bare = newStyle == Style.OVERLAY;
        frame.setVisible(!bare);
        for (Polyline corner : corners) {
            corner.setVisible(!bare);
        }
    }

    public void loadBackgroundImage(Path imagePath) {
        ImageAssets.setBackdrop(backgroundView, imagePath, null, Backdrop.FIELD);
    }

    /** 仮ゲームオーバー時などの警告用: 画面全体を赤くフラッシュさせる */
    public void triggerDangerFlash() {
        dangerOverlay.setOpacity(FxParams.DANGER_FLASH_OPACITY);
        FadeTransition ft = new FadeTransition(Duration.millis(FxParams.DANGER_FLASH_MS), dangerOverlay);
        ft.setFromValue(FxParams.DANGER_FLASH_OPACITY);
        ft.setToValue(0);
        ft.play();
    }

    /** プレイフィールド上に流れるスコア／警告ポップアップを表示する */
    public void spawnScorePopup(String text, Color color) {
        Label popup = PopupFx.label(text, 42, PopupFx.toWebColor(color));
        popup.setLayoutX(getPrefWidth() / 2 - 120);
        popup.setLayoutY(getPrefHeight() * 0.35);
        PopupFx.rise(this, popup,
                -FxParams.PLAYFIELD_POPUP_RISE_PX, Duration.millis(FxParams.PLAYFIELD_POPUP_MS));
    }

    // ============================================================
    //  スキン適用（ワールドローテートごとに呼ばれる）
    //  演出は持たず見た目の切替のみ。回転演出は playWorldRotateTransition が担当
    // ============================================================
    public void applySkin(UiSkin skin) {
        frame.setStroke(KowloonPalette.alpha(Color.web(skin.theme.borderColor), 0.85));
        for (Polyline corner : corners) {
            corner.setStroke(Color.web(skin.theme.accentColor));
        }

        if (skin.playfieldBgImage != null) {
            ImageAssets.setBackdrop(backgroundView, skin.playfieldBgImage,
                    ImageAssets.PLAYFIELD_BG_DEFAULT, Backdrop.FIELD);
        }

        // ゲームボード全体の不透明度をスキンに合わせてフェード
        Timeline opacityAnim = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(opacityProperty(), getOpacity())),
            new KeyFrame(Duration.millis(FxParams.SKIN_OPACITY_FADE_MS),
                new KeyValue(opacityProperty(), skin.theme.boardOpacity))
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

        Duration d = Duration.millis(FxParams.WORLD_ROTATE_BOARD_MS);

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
        FadeTransition flash = new FadeTransition(Duration.millis(FxParams.WORLD_ROTATE_FLASH_MS), flashOverlay);
        flash.setFromValue(FxParams.WORLD_ROTATE_FLASH_PEAK);
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
        double a = FxParams.SHAKE_AMPLITUDE_PX;
        Timeline shake = new Timeline(
            new KeyFrame(Duration.ZERO,        new KeyValue(translateXProperty(), 0.0)),
            new KeyFrame(Duration.millis(40),  new KeyValue(translateXProperty(), a)),
            new KeyFrame(Duration.millis(80),  new KeyValue(translateXProperty(), -a)),
            new KeyFrame(Duration.millis(120), new KeyValue(translateXProperty(), a * 0.75)),
            new KeyFrame(Duration.millis(160), new KeyValue(translateXProperty(), -a * 0.75)),
            new KeyFrame(Duration.millis(200), new KeyValue(translateXProperty(), a * 0.45)),
            new KeyFrame(Duration.millis(240), new KeyValue(translateXProperty(), -a * 0.45)),
            new KeyFrame(Duration.millis(FxParams.SHAKE_MS), new KeyValue(translateXProperty(), 0.0))
        );
        shake.play();
    }
}
