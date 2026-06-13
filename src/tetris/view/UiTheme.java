package tetris.view;

import javafx.scene.paint.Color;

public enum UiTheme {

    // Step 0: サイバーブルー（デフォルト）
    CYBER_BLUE(
        "#445566",
        "rgba(10, 10, 30, 0.80)",
        "#e0e0e0",
        "#aaddff",
        Color.rgb(80, 140, 220),
        1.0
    ),

    // Step 1: エンバーレッド
    EMBER_RED(
        "#664433",
        "rgba(30, 10, 8, 0.80)",
        "#f0d8c0",
        "#ffaa66",
        Color.rgb(220, 100, 60),
        0.82
    ),

    // Step 2: ネオングリーン
    NEON_GREEN(
        "#336655",
        "rgba(8, 28, 15, 0.80)",
        "#c8f0d8",
        "#66ffaa",
        Color.rgb(60, 200, 110),
        0.60
    ),

    // Step 3: バイオレット
    VIOLET(
        "#553366",
        "rgba(18, 8, 30, 0.80)",
        "#e8c8f0",
        "#cc88ff",
        Color.rgb(180, 80, 240),
        0.78
    );

    /** 枠線カラー (CSS hex) */
    public final String borderColor;
    /** 背景カラー (CSS rgba) */
    public final String bgColor;
    /** 通常テキストカラー (CSS hex) */
    public final String textColor;
    /** アクセントカラー (CSS hex) */
    public final String accentColor;
    /** フラッシュ演出用 JavaFX Color */
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
