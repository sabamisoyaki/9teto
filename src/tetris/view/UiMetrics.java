package tetris.view;

/**
 * 画面レイアウトの基準寸法（余白・間隔・タイプスケール）。
 *
 * 「綺麗に見える」の実体は装飾の量ではなく **同じ数値が繰り返されていること** なので、
 * 余白・間隔・文字サイズをここに集約し、各 View に生の px を書かない。
 *
 * 縦の基準線: 全パネルの上端 {@link #TOP}／下端 {@link #BOTTOM} を揃える。
 * 横の基準線: 画面左右の {@link #MARGIN} と、パネル間の {@link #GUTTER} だけで割る。
 */
public final class UiMetrics {

    /** 論理解像度（ルートペインの固定サイズ） */
    public static final double SCREEN_W = 1920;
    public static final double SCREEN_H = 1080;

    /** 画面外周の余白。全パネルの外側エッジはこの線に揃える */
    public static final double MARGIN = 40;
    /** パネル同士の間隔 */
    public static final double GUTTER = 24;
    /** パネル内側の余白 */
    public static final double PAD = 18;

    /**
     * プレイフィールドの一辺（Render のセルサイズ算出元なので実行中は変更しない）。
     *
     * 700 は盤面 25 マスで割り切れる（セル 28px・センタリング余りゼロ）値であり、
     * 構図ラフの「赤枠＝画面短辺の 55〜65%」ともほぼ一致する。ここを大きくすると
     * キャラを大きく見せる余地がそのぶん減る。
     */
    public static final double FIELD = 700;

    /** 操作ヒント帯の高さ */
    public static final double HINT_H = 56;

    /**
     * 主要パネルの上端／下端。「盤面 + 間隔 + ヒント帯」のかたまりを画面の縦中央へ置く。
     * FIELD を変えても上下の余白が自動で釣り合う。
     */
    public static final double TOP = (SCREEN_H - (FIELD + GUTTER + HINT_H)) / 2; // 150
    public static final double BOTTOM = TOP + FIELD;                             // 850

    /** 操作ヒント帯の上端（プレイフィールド下端から GUTTER ぶん下） */
    public static final double HINT_Y = BOTTOM + GUTTER;                         // 874

    /** パネル見出し帯の高さ（テキスト行＋アクセント罫） */
    public static final double HEADER_H = 34;

    // ---- タイプスケール（この 6 段以外のサイズを使わない） ----
    public static final int FONT_HEADER = 19;
    public static final int FONT_SCORE  = 40;
    public static final int FONT_VALUE  = 28;
    public static final int FONT_LABEL  = 16;
    public static final int FONT_BODY   = 18;
    public static final int FONT_HINT   = 15;

    private UiMetrics() {
    }
}
