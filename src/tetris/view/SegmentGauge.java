package tetris.view;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;

/**
 * 「あと何回」を四角の並びで見せるゲージ（ラベル + マス目）。
 *
 * 数字や ○● の文字で出すとフォント依存で欠けるうえ、読むのに視線を止める必要がある。
 * 面積で出せば盤面から目を離さずに残量が分かるので、回転までのライン数と
 * 仮ゲームオーバーの残機はどちらもこの部品で表示する。
 */
public final class SegmentGauge extends HBox {

    private static final double CELL = 18;
    private static final double CELL_GAP = 6;

    private final Label label;
    private final HBox cellBox;

    private String onColor = KowloonPalette.LIGHT_HEX;
    private int filled;

    public SegmentGauge(String labelText) {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(0);

        label = new Label(labelText);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        cellBox = new HBox(CELL_GAP);
        cellBox.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(label, spacer, cellBox);
    }

    /**
     * 目盛りの総数と点灯数をまとめて設定する。総数はゲーム側の定数（回転間隔・残機）を
     * そのまま渡す想定で、View 側に同じ数値を持たない（＝定数を変えても表示がズレない）。
     */
    public void setValue(int filledCount, int max) {
        if (cellBox.getChildren().size() != max) {
            cellBox.getChildren().clear();
            for (int i = 0; i < max; i++) {
                Rectangle cell = new Rectangle(CELL, CELL);
                cell.setStrokeWidth(1);
                cellBox.getChildren().add(cell);
            }
        }
        this.filled = Math.max(0, Math.min(max, filledCount));
        repaint();
    }

    /**
     * 点灯色を指定してスキンを適用する。
     * 点灯色だけは用途で変えたい（残量＝蛍光灯色／危険＝ネオン赤）ため引数で受ける。
     */
    public void applySkin(UiSkin skin, String onColorHex) {
        this.onColor = onColorHex;
        label.setStyle(skin.fontStyle(UiMetrics.FONT_LABEL)
            + " -fx-text-fill: " + KowloonPalette.rgba(KowloonPalette.LIGHT_HEX, 0.65) + ";");
        repaint();
    }

    private void repaint() {
        for (int i = 0; i < cellBox.getChildren().size(); i++) {
            Rectangle cell = (Rectangle) cellBox.getChildren().get(i);
            boolean on = i < filled;
            cell.setFill(on
                ? javafx.scene.paint.Color.web(onColor)
                : KowloonPalette.alpha(KowloonPalette.SHADOW, 0.55));
            cell.setStroke(on
                ? javafx.scene.paint.Color.web(onColor)
                : KowloonPalette.alpha(KowloonPalette.LIGHT, 0.28));
        }
    }
}
