package tetris.view;

import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * 複数の UI パネルを「横方向にパタンと畳む → スキン適用 → 開く」フリップ演出で
 * まとめて入れ替えるアニメーター。
 *
 * 適用処理（applyAtMidpoint）は必ず1回だけ、畳み切った瞬間に実行される。
 * 演出途中に次の入れ替えが要求された場合は、前回分を即座に完了させてから始める
 * （スキン適用がスキップされて見た目と状態がズレるのを防ぐ）。
 */
public final class UiSwapAnimator {

    private static final Duration FOLD_DURATION = Duration.millis(200);
    private static final Duration OPEN_DURATION = Duration.millis(240);

    private final Node[] targets;

    private ParallelTransition running;
    private Runnable pendingApply;

    public UiSwapAnimator(Node... targets) {
        this.targets = targets;
    }

    /** フリップ演出付きでスキン適用を実行する */
    public void play(Runnable applyAtMidpoint) {
        finishImmediately();
        pendingApply = applyAtMidpoint;

        ParallelTransition fold = buildScaleAll(FOLD_DURATION, 0.0, Interpolator.EASE_IN);
        fold.setOnFinished(e -> {
            runPendingApply();
            ParallelTransition open = buildScaleAll(OPEN_DURATION, 1.0, Interpolator.EASE_OUT);
            open.setOnFinished(e2 -> running = null);
            running = open;
            open.play();
        });
        running = fold;
        fold.play();
    }

    /** 実行中の演出を即完了させる（適用漏れなし・scaleX も復元） */
    public void finishImmediately() {
        if (running != null) {
            running.stop();
            running = null;
        }
        runPendingApply();
        for (Node n : targets) {
            n.setScaleX(1.0);
        }
    }

    private void runPendingApply() {
        if (pendingApply != null) {
            Runnable r = pendingApply;
            pendingApply = null;
            r.run();
        }
    }

    private ParallelTransition buildScaleAll(Duration d, double toX, Interpolator interp) {
        ParallelTransition pt = new ParallelTransition();
        for (Node n : targets) {
            ScaleTransition st = new ScaleTransition(d, n);
            st.setToX(toX);
            st.setInterpolator(interp);
            pt.getChildren().add(st);
        }
        return pt;
    }
}
