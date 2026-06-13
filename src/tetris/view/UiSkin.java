package tetris.view;

import java.nio.file.Path;

/**
 * ワールド回転ステップごとの UI 一式（配色・フォント・枠形状・各パネルの背景画像・キャラ絵）。
 * 「どのステップでどの見た目になるか」は UiSkinBank が一元管理する。
 *
 * 画像 Path は null または非存在を許容し、各 Pane 側でフォールバックする。
 */
public final class UiSkin {

    /** スキン名（デバッグ表示用） */
    public final String name;
    /** 配色パレット */
    public final UiTheme theme;
    /** ラベル類のフォントファミリー */
    public final String fontFamily;
    /** パネル角丸 (px) */
    public final int borderRadius;
    /** パネル枠線幅 (px) */
    public final int borderWidth;
    /** HUD パネル背景画像（null可） */
    public final Path hudBgImage;
    /** NEXT / HOLD パネル背景画像（null可） */
    public final Path nextBgImage;
    /** プレイフィールド背景画像（null可） */
    public final Path playfieldBgImage;
    /** キャラクター立ち絵（null可 → デフォルト） */
    public final Path characterImage;

    public UiSkin(
            String name,
            UiTheme theme,
            String fontFamily,
            int borderRadius,
            int borderWidth,
            Path hudBgImage,
            Path nextBgImage,
            Path playfieldBgImage,
            Path characterImage) {
        this.name = name;
        this.theme = theme;
        this.fontFamily = fontFamily;
        this.borderRadius = borderRadius;
        this.borderWidth = borderWidth;
        this.hudBgImage = hudBgImage;
        this.nextBgImage = nextBgImage;
        this.playfieldBgImage = playfieldBgImage;
        this.characterImage = characterImage;
    }

    /** パネル共通のベーススタイル（背景色・枠線）を生成する */
    public String panelStyle() {
        return "-fx-background-color: " + theme.bgColor + ";"
            + " -fx-border-color: " + theme.borderColor + ";"
            + " -fx-border-width: " + borderWidth + "px;"
            + " -fx-border-radius: " + borderRadius + "px;"
            + " -fx-background-radius: " + borderRadius + "px;";
    }

    /** ラベル共通のフォント指定を生成する */
    public String fontStyle(int sizePx) {
        return "-fx-font-size: " + sizePx + "px; -fx-font-family: '" + fontFamily + "';";
    }
}
