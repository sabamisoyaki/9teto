package tetris.view;

import javafx.geometry.Point2D;

/**
 * UI パネルの配置（各パネルの左上座標）を定義する。
 * 1920×1080 のルート上に絶対座標で配置するため、ここに座標を書くだけで
 * パネルの位置を自由に組み替えられる。配置一覧は UiLayoutBank が一元管理する。
 *
 * 各パネルのサイズは固定:
 *   playfield 840×840 / hold・next 各 420×168 / character 480×1080 / hud 480×400
 */
public final class UiLayout {

    /** 配置名（デバッグ表示用） */
    public final String name;
    public final Point2D playfield;
    public final Point2D hold;
    public final Point2D next;
    public final Point2D character;
    public final Point2D hud;

    public UiLayout(
            String name,
            Point2D playfield,
            Point2D hold,
            Point2D next,
            Point2D character,
            Point2D hud) {
        this.name = name;
        this.playfield = playfield;
        this.hold = hold;
        this.next = next;
        this.character = character;
        this.hud = hud;
    }
}
