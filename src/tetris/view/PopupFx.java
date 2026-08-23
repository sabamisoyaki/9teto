package tetris.view;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * ポップアップ演出の共通部品。スタイル生成と
 * 「add → アニメーション → 必ず remove」のライフサイクルをここで一元保証する。
 * 表示位置（座標・alignment）は要件が呼び出し元ごとに違うため、呼び出し側が設定する。
 */
final class PopupFx {

    /** 共通スタイル（bold・Courier New・ドロップシャドウ）のポップアップ Label を生成する */
    static Label label(String text, int fontPx, String webColor) {
        Label popup = new Label(text);
        popup.setStyle("-fx-font-size: " + fontPx + "px; -fx-font-weight: bold;"
            + " -fx-text-fill: " + webColor + ";"
            + " -fx-font-family: 'Courier New';"
            + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.9), 6, 0.0, 2, 2);");
        popup.setMouseTransparent(true);
        return popup;
    }

    /** host に追加し、上昇＋フェードアウト後に必ず remove する（リーク防止） */
    static void rise(Pane host, Label popup, double riseByPx, Duration duration) {
        host.getChildren().add(popup);

        TranslateTransition move = new TranslateTransition(duration, popup);
        move.setByY(riseByPx);
        FadeTransition fade = new FadeTransition(duration, popup);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);

        ParallelTransition anim = new ParallelTransition(move, fade);
        anim.setOnFinished(e -> host.getChildren().remove(popup));
        anim.play();
    }

    /** host に追加し、スケールイン＋遅延フェードアウト後に必ず remove する */
    static void burst(Pane host, Label popup, Duration scaleIn, Duration fade, Duration fadeDelay) {
        host.getChildren().add(popup);

        ScaleTransition scale = new ScaleTransition(scaleIn, popup);
        scale.setFromX(0.4);
        scale.setFromY(0.4);
        scale.setToX(1.0);
        scale.setToY(1.0);

        FadeTransition ft = new FadeTransition(fade, popup);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setDelay(fadeDelay);

        ParallelTransition anim = new ParallelTransition(scale, ft);
        anim.setOnFinished(e -> host.getChildren().remove(popup));
        anim.play();
    }

    static String toWebColor(Color c) {
        return String.format("#%02x%02x%02x",
            (int) Math.round(c.getRed() * 255),
            (int) Math.round(c.getGreen() * 255),
            (int) Math.round(c.getBlue() * 255));
    }

    private PopupFx() {
    }
}
