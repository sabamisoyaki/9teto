package tetris.view;

import java.util.List;

import javafx.geometry.Point2D;

/**
 * UI 配置の定義一覧。配置を増やす・調整するときはこのファイルの LAYOUTS に
 * 1 エントリ追加／編集するだけでよい。
 *
 * 座標は 1920×1080 ルート上の各パネル左上。パネルサイズは UiLayout の説明を参照。
 * ゲーム中 F3 キーで配置を順送りに切り替えられる（プレビュー用）。
 */
public final class UiLayoutBank {

    private static final List<UiLayout> LAYOUTS = List.of(
        // 1) CLASSIC: 左にプレイフィールド＋下にプレビュー、右にキャラ＋HUD（従来配置）
        new UiLayout("CLASSIC",
            new Point2D(20, 20),     // playfield
            new Point2D(20, 860),    // hold
            new Point2D(440, 860),   // next
            new Point2D(960, 0),     // character
            new Point2D(1440, 0)),   // hud

        // 2) SOUTHPAW: CLASSIC を左右反転。プレイフィールドを右、キャラ＋HUDを左へ
        new UiLayout("SOUTHPAW",
            new Point2D(1060, 20),   // playfield
            new Point2D(1480, 860),  // hold
            new Point2D(1060, 860),  // next
            new Point2D(480, 0),     // character
            new Point2D(0, 0)),      // hud

        // 3) CENTER STAGE: プレイフィールドを中央、左にHUD＋プレビュー、右にキャラ
        new UiLayout("CENTER STAGE",
            new Point2D(540, 120),   // playfield（中央寄せ）
            new Point2D(20, 560),    // hold
            new Point2D(20, 740),    // next
            new Point2D(1440, 0),    // character
            new Point2D(20, 120)),   // hud

        // 4) SIDEBAR: 左にプレイフィールド、右端にキャラ、中央の隙間にHUD＋プレビューを縦積み
        new UiLayout("SIDEBAR",
            new Point2D(40, 120),    // playfield
            new Point2D(910, 740),   // hold
            new Point2D(910, 560),   // next
            new Point2D(1440, 0),    // character
            new Point2D(910, 120)),  // hud

        // 5) NEXT_CLUSTER: NEXT/HOLD の直下にバストアップのキャラを置く
        new UiLayout("NEXT_CLUSTER",
            new Point2D(200, 120),   // playfield
            new Point2D(1120, 250),  // hold
            new Point2D(1120, 60),   // next
            new Point2D(1150, 430),  // character
            new Point2D(1560, 60))   // hud
    );

    public static UiLayout get(int index) {
        return LAYOUTS.get(Math.floorMod(index, LAYOUTS.size()));
    }

    public static int count() {
        return LAYOUTS.size();
    }

    private UiLayoutBank() {
    }
}
