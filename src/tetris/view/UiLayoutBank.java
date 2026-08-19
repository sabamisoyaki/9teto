package tetris.view;

import java.util.List;

import javafx.geometry.Point2D;

import tetris.ResourcePath;
import tetris.view.UiLayout.Box;
import tetris.view.UiLayout.Style;

/**
 * UI 配置の定義一覧。配置を増やす・調整するときはこのファイルの LAYOUTS に
 * 1 エントリ追加／編集するだけでよい。ゲーム中 F3 で順送りに切り替えられる。
 *
 * 全配置は共通の基準線に載せている（{@link UiMetrics}）:
 *   - 左右の外側エッジ  … MARGIN(40) / SCREEN_W - MARGIN(1880)
 *   - パネル群の上端／下端 … TOP(150) / BOTTOM(850)
 *   - パネル同士の間隔  … GUTTER(24)
 *   - 操作ヒント帯      … HINT_Y(874) から HINT_H(56)
 * 座標は基準線から算出した式で書き、生の px を並べない。ズレはここを読めば分かる。
 *
 * 1〜6 は「計器を並べる」様式（{@link Style#PANEL}）、7〜8 は構図ラフを起こした
 * 「1 枚絵に窓を開ける」様式（{@link Style#OVERLAY}）。F3 で並べて比較できる。
 */
public final class UiLayoutBank {

    private static final double L = UiMetrics.MARGIN;                       //   40 左端
    private static final double R = UiMetrics.SCREEN_W - UiMetrics.MARGIN;  // 1880 右端
    private static final double T = UiMetrics.TOP;                          //  150 上端
    private static final double B = UiMetrics.BOTTOM;                       //  850 下端
    private static final double G = UiMetrics.GUTTER;                       //   24 間隔
    private static final double F = UiMetrics.FIELD;                        //  700 盤面

    /**
     * プレビュー（NEXT / HOLD）1 枚の高さ。ミノ 2 段ぶんが入る最小限に留めている。
     * ここを増やすと、同じ列に積む HUD の高さがそのぶん削られてセリフが入らなくなる。
     */
    private static final double PREVIEW_H = 132;
    /** プレビュー 1 枚ぶんの縦の占有（本体 + 下の間隔） */
    private static final double PREVIEW_STEP = PREVIEW_H + G;
    /** キャラを縦一杯に置くときの標準幅 */
    private static final double CHAR_W = 440;
    /** 盤面を中央に置くときの左端 */
    private static final double CENTER_X = (UiMetrics.SCREEN_W - F) / 2;    //  610
    /** 中央配置のときに左右へ残る帯の幅 */
    private static final double RAIL_W = CENTER_X - G - L;                  //  546

    /** 画面幅いっぱいの操作ヒント帯（多くの配置で共用） */
    private static final Box FULL_HINT =
        new Box(L, UiMetrics.HINT_Y, R - L, UiMetrics.HINT_H);

    private static final List<UiLayout> LAYOUTS = List.of(

        // 1) CLASSIC: 盤面を左、中央に情報列（NEXT/HOLD/HUD）、右端にキャラ。
        //    情報列とキャラの上端・下端が盤面と揃うので、視線が横に流れても段差が出ない。
        classicColumns("CLASSIC", L, false),

        // 2) SOUTHPAW: CLASSIC の左右反転。利き目・利き手で盤面を右に置きたい人向け。
        classicColumns("SOUTHPAW", R - F, true),

        // 3) CENTER STAGE: 盤面を画面中央へ。左に情報列、右にキャラで挟む。
        //    盤面が常に視線の正面に来るぶん、周辺の情報は端まで離れる。
        new UiLayout("CENTER STAGE", Style.PANEL,
            new Point2D(CENTER_X, T),
            new Box(L, T + PREVIEW_STEP, RAIL_W, PREVIEW_H),                  // hold
            new Box(L, T, RAIL_W, PREVIEW_H),                                 // next
            new Box(R - RAIL_W, T, RAIL_W, B - T),                            // character
            new Box(L, T + PREVIEW_STEP * 2, RAIL_W, B - T - PREVIEW_STEP * 2),
            FULL_HINT, null),

        // 4) NEXT CLUSTER: NEXT/HOLD の直下にキャラを置き、盤面のすぐ隣へ情報を寄せる。
        //    視線移動を最小にする配置。HUD は右端の縦長カラムへ逃がす。
        nextCluster(),

        // 5) COCKPIT: 盤面を中央に据え、左右へ対称に計器を配る。
        //    HOLD↔NEXT / HUD↔キャラ が線対称になるので、画面が“機体”として読める。
        new UiLayout("COCKPIT", Style.PANEL,
            new Point2D(CENTER_X, T),
            new Box(L, T, RAIL_W, PREVIEW_H),                                 // hold（左上）
            new Box(R - RAIL_W, T, RAIL_W, PREVIEW_H),                        // next（右上）
            new Box(R - RAIL_W, T + PREVIEW_STEP, RAIL_W, B - T - PREVIEW_STEP),
            new Box(L, T + PREVIEW_STEP, RAIL_W, B - T - PREVIEW_STEP),       // hud（左下）
            FULL_HINT, null),

        // 6) THEATER: キャラを画面右半分いっぱいの縦長で主役に据える。
        //    盤面は左上、NEXT/HOLD は盤面の真下へ寝かせ、HUD は細い縦カラムに畳む。
        //    ヒント帯を置く余地が無いので、この配置だけ非表示（null）にしている。
        theater(),

        // 7-8) ROUGH A / B: 構図ラフ（style.jpg）の 2 コマをそのままモックの立ち絵にした案。
        //    tools/BuildRoughMocks.java が「ラフの赤枠 = ゲームの盤面矩形」になるよう
        //    拡大・平行移動して焼いているので、盤面の位置はラフと一致している。
        //    情報はラフの SCORE 枠にならって右下の小枠へ畳む。
        rough("ROUGH A (逆さ)", "mock-rough-a.png"),
        rough("ROUGH B (顔アップ)", "mock-rough-b.png")
    );

    /**
     * 「盤面 / 情報列 / キャラ列」の 3 カラム構成を組む。CLASSIC と SOUTHPAW は
     * 左右が入れ替わるだけの同じ構図なので、座標をコピーせずここで生成する。
     *
     * @param fieldX  盤面の左端
     * @param mirror  true でキャラを左端・情報列を盤面側に寄せる（SOUTHPAW）
     */
    private static UiLayout classicColumns(String name, double fieldX, boolean mirror) {
        double charX = mirror ? L : R - CHAR_W;
        double infoX = mirror ? L + CHAR_W + G : L + F + G;
        double infoW = mirror ? fieldX - G - infoX : charX - G - infoX;

        return new UiLayout(name, Style.PANEL,
            new Point2D(fieldX, T),
            new Box(infoX, T + PREVIEW_STEP, infoW, PREVIEW_H),
            new Box(infoX, T, infoW, PREVIEW_H),
            new Box(charX, T, CHAR_W, B - T),
            new Box(infoX, T + PREVIEW_STEP * 2, infoW, B - T - PREVIEW_STEP * 2),
            FULL_HINT, null);
    }

    /** 盤面の右に残る領域を 2 カラムへ割り、左カラムへ NEXT/HOLD/キャラ、右カラムへ HUD */
    private static UiLayout nextCluster() {
        double colX = L + F + G;
        double colW = (R - colX - G) / 2;

        return new UiLayout("NEXT CLUSTER", Style.PANEL,
            new Point2D(L, T),
            new Box(colX, T + PREVIEW_STEP, colW, PREVIEW_H),                 // hold
            new Box(colX, T, colW, PREVIEW_H),                                // next
            new Box(colX, T + PREVIEW_STEP * 2, colW, B - T - PREVIEW_STEP * 2),
            new Box(colX + colW + G, T, colW, B - T),                         // hud
            FULL_HINT, null);
    }

    /** キャラを右半分の縦長に据え、盤面を左上、プレビューをその真下へ寝かせる */
    private static UiLayout theater() {
        double charW = 612;
        double top = L;
        double previewY = top + F + G;
        double previewH = UiMetrics.SCREEN_H - L - previewY;
        double previewW = (F - G) / 2;
        double hudX = L + F + G;

        return new UiLayout("THEATER", Style.PANEL,
            new Point2D(L, top),
            new Box(L + previewW + G, previewY, previewW, previewH),          // hold
            new Box(L, previewY, previewW, previewH),                         // next
            new Box(R - charW, top, charW, UiMetrics.SCREEN_H - top * 2),     // character
            new Box(hudX, top, R - charW - G - hudX, UiMetrics.SCREEN_H - top * 2),
            null, null);
    }

    /**
     * 構図ラフから起こしたモック配置。
     *
     * 盤面は BuildRoughMocks が絵を焼くときに使った矩形（画面中央 × TOP）と同じ位置に置く。
     * ここを動かすなら tools/BuildRoughMocks.java の fieldX/fieldY も合わせること。
     * SCORE の小枠はラフに描かれた枠の位置（右下）に合わせている。
     */
    private static UiLayout rough(String name, String artFile) {
        double previewW = 200;
        double previewH = 150;
        double previewX = CENTER_X - G - previewW;                            //  386
        double hudW = 460;
        double hudH = 320;

        return new UiLayout(name, Style.OVERLAY,
            new Point2D(CENTER_X, T),
            new Box(previewX, T + previewH + G, previewW, previewH),          // hold
            new Box(previewX, T, previewW, previewH),                         // next
            new Box(0, 0, UiMetrics.SCREEN_W, UiMetrics.SCREEN_H),            // character（全画面）
            new Box(R - hudW, UiMetrics.SCREEN_H - L - hudH, hudW, hudH),     // hud（右下の小枠）
            null,
            ResourcePath.of("images", artFile));
    }

    /** 起動直後に表示する配置（LAYOUTS の並び順に対応） */
    public static final int DEFAULT_LAYOUT_INDEX = 6; // ROUGH A

    public static UiLayout get(int index) {
        return LAYOUTS.get(Math.floorMod(index, LAYOUTS.size()));
    }

    public static int count() {
        return LAYOUTS.size();
    }

    private UiLayoutBank() {
    }
}
