package tetris.view;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * ワールド回転に連動して最前面へ一瞬だけ出る「近づいてくる」キャラ演出レイヤー。
 *
 * 常設の {@link CharacterPane} とは独立したオーバーレイで、拡大しても既存レイアウトの
 * サイズ計算に影響しない。通常時は不可視・マウス透過で、{@link #play(UiSkin)} の間だけ
 * 寄り差分（{@link UiSkin#approachImage}）を小さめ→手前へズームさせ、完了後は不可視へ戻して
 * 見た目を（スキン適用済みの）常設キャラへバトンタッチする。
 */
public final class CharacterApproachPane extends StackPane {

    private static final Duration DURATION = Duration.millis(FxParams.CHAR_APPROACH_MS);

    private final ImageView view;
    private ParallelTransition running;

    public CharacterApproachPane(double width, double height, double characterSize) {
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);
        setMouseTransparent(true);
        setVisible(false);

        view = new ImageView();
        view.setFitWidth(characterSize);
        view.setFitHeight(characterSize);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        getChildren().add(view);
        StackPane.setAlignment(view, Pos.CENTER);
    }

    /**
     * 次スキンの寄り差分でズームインを再生する。
     * 演出途中に次が来たら前回を即停止し、常に不可視の初期状態から始める。
     */
    public void play(UiSkin next) {
        if (running != null) {
            running.stop();
        }
        ImageAssets.setImage(view, next.approachImage, next.characterImage);
        if (view.getImage() == null) {
            setVisible(false);
            return;
        }

        // 初期状態: 小さめ・わずかに下（奥）・透明・傾きセット
        view.setScaleX(FxParams.CHAR_APPROACH_FROM_SCALE);
        view.setScaleY(FxParams.CHAR_APPROACH_FROM_SCALE);
        view.setTranslateY(FxParams.CHAR_APPROACH_RISE_PX);
        view.setOpacity(0);
        setVisible(true);

        ScaleTransition scale = new ScaleTransition(DURATION, view);
        scale.setToX(FxParams.CHAR_APPROACH_OVERSHOOT);
        scale.setToY(FxParams.CHAR_APPROACH_OVERSHOOT);
        scale.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition rise = new TranslateTransition(DURATION, view);
        rise.setToY(0);
        rise.setInterpolator(Interpolator.EASE_OUT);

        // 山なり補間: opacity = 0 + (1-0)*sin(pi*t) → 中盤 1、両端 0 のフェードイン→アウト
        FadeTransition fade = new FadeTransition(DURATION, view);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(new PeakInterpolator());

        // やや大きめの傾きから決めポーズの角度へ収束させ、寄りに「ひねり」を添える
        RotateTransition settle = new RotateTransition(DURATION, view);
        settle.setFromAngle(next.approachRotate * 1.8);
        settle.setToAngle(next.approachRotate);
        settle.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition pt = new ParallelTransition(scale, rise, fade, settle);
        pt.setOnFinished(e -> {
            setVisible(false);
            running = null;
        });
        running = pt;
        pt.play();
    }

    /**
     * 0→1→0 の山なりを描くフェード用インターポレータ。
     * 中盤で最大不透明度に達し、寄り切りに合わせて抜ける。
     */
    private static final class PeakInterpolator extends Interpolator {
        @Override
        protected double curve(double t) {
            // sin(pi*t): t=0,1 で 0、t=0.5 で 1
            return Math.sin(Math.PI * t);
        }
    }
}
