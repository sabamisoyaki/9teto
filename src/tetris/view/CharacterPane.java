package tetris.view;

import java.nio.file.Path;

import javafx.geometry.Pos;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

import tetris.view.UiLayout.Style;

/**
 * キャラクターの立ち絵レイヤー。配置の様式（{@link Style}）で二役をこなす。
 *
 * <pre>
 *   PANEL   … 他のパネルと同じ枠・背景を着せた矩形。立ち絵は枠に対して
 *             {@link #CHARACTER_OVERSCAN} 倍で焼き込み、上端基準でクリップする
 *             （＝顔が必ず入る）。立ち絵だけ枠なしで浮くと「画面に貼られた別の絵」
 *             に見えてしまうため。
 *   OVERLAY … 枠・背景・クリップをすべて外した全画面レイヤー。構図ラフの
 *             「立ち絵の上に盤面を窓状に重ねる」案では **どこにキャラが立っているかは
 *             絵の側が決める**（画面比の透過 PNG）ので、こちらは器に徹する。
 * </pre>
 */
public class CharacterPane extends StackPane {

    /**
     * PANEL 時の、枠に対する立ち絵の拡大率。枠の長辺に合わせる（＝縦横どちらにも隙間を
     * 作らない）値を基準にし、端に地の色が覗かないぶんだけ足す。これ以上大きくすると、
     * 縦長の配置で顔だけが画面を占めて「何のパネルか分からない絵」になる。
     */
    private static final double CHARACTER_OVERSCAN = 1.06;

    private final ImageView characterView;
    // 背景は Region の Background ではなく子ノードで持つ。
    // Background にはエフェクトを掛けられず、Backdrop を通せないため
    private final PanelBackground backgroundView;
    private final Rectangle clip;

    private Style style = Style.PANEL;
    private UiSkin skin = UiSkinBank.forStep(0);
    // 配置が自前の絵を持つ場合はスキンより優先する（OVERLAY のモック用）
    private Path overrideArt;
    private double paneWidth = 440;
    private double paneHeight = 700;

    public CharacterPane() {
        backgroundView = new PanelBackground();

        characterView = new ImageView();
        characterView.setPreserveRatio(true);
        characterView.setSmooth(true);
        characterView.setMouseTransparent(true);

        clip = new Rectangle();
        setClip(clip);

        // 追加順 = 重なり順。背景 → 立ち絵
        getChildren().addAll(backgroundView, characterView);
        StackPane.setAlignment(characterView, Pos.TOP_CENTER);

        setPaneSize(paneWidth, paneHeight);
        backgroundView.apply(ImageAssets.CHARACTER_CLOSEUP_BG, null, Backdrop.CHARACTER);
        applySkin(skin);
    }

    /**
     * 配置定義の様式を適用する。
     * {@code setStyle} は {@link javafx.scene.layout.Region} のスタイル文字列用に
     * 埋まっているので、こちらは applyStyle という名前にしている。
     */
    public void applyStyle(Style newStyle, Path art) {
        this.style = newStyle;
        this.overrideArt = art;
        boolean overlay = newStyle == Style.OVERLAY;

        setClip(overlay ? null : clip);
        backgroundView.setVisible(!overlay);
        StackPane.setAlignment(characterView, overlay ? Pos.CENTER : Pos.TOP_CENTER);

        applySkin(skin);
        setPaneSize(paneWidth, paneHeight);
    }

    /** 配置定義に合わせてレイヤーの大きさを変える。立ち絵とクリップも追従させる */
    public void setPaneSize(double width, double height) {
        this.paneWidth = width;
        this.paneHeight = height;

        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);

        clip.setWidth(width);
        clip.setHeight(height);
        backgroundView.setBox(width, height);

        if (style == Style.OVERLAY) {
            // 絵は画面比で描かれている前提。等倍で敷くだけにして構図は絵に任せる
            characterView.setFitWidth(width);
            characterView.setFitHeight(height);
        } else {
            double size = Math.max(width, height) * CHARACTER_OVERSCAN;
            characterView.setFitWidth(size);
            characterView.setFitHeight(size);
        }
    }

    public ImageView getCharacterView() {
        return characterView;
    }

    /**
     * スキンに対応するキャラ絵へ差し替える（ステップ→画像の対応は UiSkinBank が持つ）。
     * OVERLAY では配置が持つ絵を使い、無ければ通常の立ち絵へフォールバックする。
     */
    public void applySkin(UiSkin newSkin) {
        this.skin = newSkin;

        if (style == Style.OVERLAY) {
            setStyle(""); // 枠も背景色も持たない透明なレイヤー
            ImageAssets.setImage(characterView, overrideArt, skin.characterImage);
        } else {
            setStyle(skin.panelStyle());
            ImageAssets.setImage(characterView, skin.characterImage, ImageAssets.CHARACTER_DEFAULT);
        }
    }

    public void loadCharacterImage(Path imagePath) {
        ImageAssets.setImage(characterView, imagePath, null);
    }
}
