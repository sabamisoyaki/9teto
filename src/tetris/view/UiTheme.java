package tetris.view;

import javafx.scene.paint.Color;

/**
 * ワールド回転ステップに対応する配色。九龍城の「階層」を1ステップ＝1フロアと読み替え、
 * 全フロアを {@link KowloonPalette} の 5 色だけで組む。
 *
 * フロア差は色を足して作らず、**役割の割り当てを入れ替えて**作る:
 *
 * <pre>
 *   フロア        枠(border)  背景(bg)       アクセント  フラッシュ  盤面不透明度
 *   1F ARCADE     錆          ベース 0.82    ネオン      蛍光灯      0.95
 *   5F MARKET     ネオン      影     0.86    錆          ネオン      0.90
 *   9F CLINIC     蛍光灯      ベース 0.70    ネオン      蛍光灯      1.00
 *   RF ROOFTOP    ベース      影     0.92    ネオン      錆          0.84
 * </pre>
 *
 * textColor だけは全フロア共通で蛍光灯色に固定する。装飾がどれだけ変わっても
 * 情報レイヤー（スコア・NEXT・警告）の見え方は変えない、という規律のため。
 */
public enum UiTheme {

    // Step 0: 1F 電気街 — 錆びた鉄骨の枠にコンクリートの壁
    KOWLOON_ARCADE(
        KowloonPalette.RUST_HEX,
        KowloonPalette.rgba(KowloonPalette.BASE_HEX, 0.82),
        KowloonPalette.LIGHT_HEX,
        KowloonPalette.NEON_HEX,
        KowloonPalette.LIGHT,
        0.95
    ),

    // Step 1: 5F 市場 — 看板だけが明るく、奥は煤で潰れている
    KOWLOON_MARKET(
        KowloonPalette.NEON_HEX,
        KowloonPalette.rgba(KowloonPalette.SHADOW_HEX, 0.86),
        KowloonPalette.LIGHT_HEX,
        KowloonPalette.RUST_HEX,
        KowloonPalette.NEON,
        0.90
    ),

    // Step 2: 9F 診療所 — 蛍光灯に洗われて一番明るい（＝一番落ち着かない）階
    KOWLOON_CLINIC(
        KowloonPalette.LIGHT_HEX,
        KowloonPalette.rgba(KowloonPalette.BASE_HEX, 0.70),
        KowloonPalette.LIGHT_HEX,
        KowloonPalette.NEON_HEX,
        KowloonPalette.LIGHT,
        1.00
    ),

    // Step 3: RF 屋上 — 光源が看板しかない最も暗い階
    KOWLOON_ROOFTOP(
        KowloonPalette.BASE_HEX,
        KowloonPalette.rgba(KowloonPalette.SHADOW_HEX, 0.92),
        KowloonPalette.LIGHT_HEX,
        KowloonPalette.NEON_HEX,
        KowloonPalette.RUST,
        0.84
    );

    /** 枠線カラー (CSS hex) */
    public final String borderColor;
    /** 背景カラー (CSS rgba) */
    public final String bgColor;
    /** 通常テキストカラー (CSS hex)。全フロア共通 */
    public final String textColor;
    /** アクセントカラー (CSS hex) */
    public final String accentColor;
    /** フラッシュ演出用 JavaFX Color（回転時のクロスフェードで焚く） */
    public final Color flashColor;
    /** ゲームボード全体の不透明度 (0.0〜1.0) */
    public final double boardOpacity;

    UiTheme(String border, String bg, String text, String accent, Color flash, double boardOpacity) {
        this.borderColor   = border;
        this.bgColor       = bg;
        this.textColor     = text;
        this.accentColor   = accent;
        this.flashColor    = flash;
        this.boardOpacity  = boardOpacity;
    }

    // ステップ→見た目の対応は UiSkinBank が一元管理する（forStep はそちらへ移管）
}
