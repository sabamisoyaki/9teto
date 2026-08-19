package tetris.view;

import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

import tetris.view.UiLayout.Box;
import tetris.view.UiLayout.Style;

public class GameView {

    private static final double WINDOW_WIDTH  = UiMetrics.SCREEN_W;
    private static final double WINDOW_HEIGHT = UiMetrics.SCREEN_H;
    private static final double PLAY_FIELD_SIZE = UiMetrics.FIELD;

    // パネルは固定座標で配置する（配置定義は UiLayoutBank が一元管理）
    private final Pane root;
    private final NextPane holdPane;
    private final NextPane nextPane;
    private final DeviceFramePane playFieldPane;
    private final CharacterPane characterPane;
    // 盤面の手前を横切るキャラの線。立ち絵と同じ絵を盤面の矩形だけに切り抜いて重ねる
    private final ImageView characterFrontView;
    private final Rectangle characterFrontClip;
    private final HudPane hudPane;
    private final KeyHintPane hintPane;
    private final CharacterApproachPane approachPane;

    /** 盤面の手前へ重ねる線の濃さ。上げると絵は活きるがミノが読みにくくなる */
    private static final double FRONT_LINE_OPACITY = 0.45;

    private int layoutIndex = UiLayoutBank.DEFAULT_LAYOUT_INDEX;
    // 配置を切り替えると様式が変わり見た目を入れ直す必要があるので、現在のスキンを覚えておく
    private UiSkin currentSkin = UiSkinBank.forStep(0);

    public GameView() {
        this.root = new Pane();
        root.setPrefSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        root.setMinSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        root.setMaxSize(WINDOW_WIDTH, WINDOW_HEIGHT);

        this.playFieldPane = new DeviceFramePane(PLAY_FIELD_SIZE);
        this.holdPane = new NextPane(420, 160, "HOLD");
        this.nextPane = new NextPane(420, 160, "NEXT");
        this.characterPane = new CharacterPane();
        this.hudPane = new HudPane();
        this.hintPane = new KeyHintPane(UiMetrics.SCREEN_W - UiMetrics.MARGIN * 2, UiMetrics.HINT_H);
        this.approachPane = new CharacterApproachPane(WINDOW_WIDTH, WINDOW_HEIGHT, PLAY_FIELD_SIZE);

        // 手前レイヤー = 立ち絵と同じ絵を、盤面の矩形にクリップして薄く重ねたもの。
        // ラフでは腕や脚が赤枠の手前を横切っているが、そのための別絵を用意しなくても
        // 「窓の上に線が乗っている」感じはこれで出せる。盤面の外は元の絵と二重になって
        // 線が濃くなるだけなので、必ず盤面矩形でクリップすること。
        this.characterFrontView = new ImageView();
        characterFrontView.setMouseTransparent(true);
        characterFrontView.setSmooth(true);
        characterFrontView.setOpacity(FRONT_LINE_OPACITY);
        this.characterFrontClip = new Rectangle();
        characterFrontView.setClip(characterFrontClip);

        // 追加順 = 重なり順。
        //   キャラ(奥) → 盤面 → キャラ(手前) → 情報 → 寄り演出
        // キャラ(手前)を盤面と情報の**間**に挟むのが要点。盤面には腕や脚が掛かって
        // よいが、スコアや NEXT が絵に隠れると読めなくなる。
        // キャラ(奥)を最背面へ置くのは OVERLAY 配置のため。PANEL 配置ではパネル同士が
        // 重ならないので、この順でも見た目は変わらない。
        root.getChildren().addAll(
                characterPane,
                playFieldPane,
                characterFrontView,
                holdPane, nextPane, hudPane, hintPane,
                approachPane);

        ImageAssets.addBackdropView(root, ImageAssets.BASE_LAYER, WINDOW_WIDTH, WINDOW_HEIGHT, Backdrop.FAR);
        applyLayout(UiLayoutBank.get(layoutIndex));

        // 初期スキンを演出なしで適用（ゲーム開始時のフラッシュ暴発を防ぐ）
        applySkin(UiSkinBank.forStep(0));
    }

    public Region getRoot()                      { return root; }
    public NextPane getHoldPane()                { return holdPane; }
    public NextPane getNextPane()                { return nextPane; }
    public DeviceFramePane getPlayFieldPane()    { return playFieldPane; }
    public CharacterPane getCharacterPane()      { return characterPane; }
    public HudPane getHudPane()                  { return hudPane; }
    public KeyHintPane getHintPane()             { return hintPane; }
    public CharacterApproachPane getApproachPane() { return approachPane; }

    /** 回転連動「近づいてくる」演出を最前面レイヤーで再生する */
    public void playApproach(UiSkin skin) {
        approachPane.play(skin);
    }

    /** 各パネルを配置定義どおりの様式・座標・サイズへ移す */
    public void applyLayout(UiLayout layout) {
        // 様式を先に入れる（compact 化でパネルの中身が組み変わるため）
        playFieldPane.applyStyle(layout.style);
        holdPane.applyStyle(layout.style);
        nextPane.applyStyle(layout.style);
        hudPane.applyStyle(layout.style);
        characterPane.applyStyle(layout.style, layout.characterArt);

        playFieldPane.setLayoutX(layout.playfield.getX());
        playFieldPane.setLayoutY(layout.playfield.getY());

        place(holdPane, layout.hold);
        holdPane.setPaneSize(layout.hold.w(), layout.hold.h());
        place(nextPane, layout.next);
        nextPane.setPaneSize(layout.next.w(), layout.next.h());
        place(characterPane, layout.character);
        characterPane.setPaneSize(layout.character.w(), layout.character.h());
        place(hudPane, layout.hud);
        hudPane.setPaneSize(layout.hud.w(), layout.hud.h());

        // 手前レイヤーはキャラ本体と完全に重ね、見せる範囲だけを盤面の矩形へ絞る
        characterFrontView.setLayoutX(layout.character.x());
        characterFrontView.setLayoutY(layout.character.y());
        characterFrontView.setFitWidth(layout.character.w());
        characterFrontView.setFitHeight(layout.character.h());
        characterFrontClip.setX(layout.playfield.getX() - layout.character.x());
        characterFrontClip.setY(layout.playfield.getY() - layout.character.y());
        characterFrontClip.setWidth(UiMetrics.FIELD);
        characterFrontClip.setHeight(UiMetrics.FIELD);
        characterFrontView.setVisible(layout.style == Style.OVERLAY && layout.characterArt != null);
        ImageAssets.setImage(characterFrontView, layout.characterArt, null);

        // ヒント帯は置き場所の無い配置（THEATER / OVERLAY）では null。その場合は隠す
        hintPane.setVisible(layout.hint != null);
        if (layout.hint != null) {
            place(hintPane, layout.hint);
            hintPane.setPaneSize(layout.hint.w(), layout.hint.h());
        }
    }

    private static void place(Region pane, Box box) {
        pane.setLayoutX(box.x());
        pane.setLayoutY(box.y());
    }

    /** 次の配置定義へ切り替えて、その名前を返す（プレビュー用） */
    public String cycleLayout() {
        layoutIndex = (layoutIndex + 1) % UiLayoutBank.count();
        UiLayout layout = UiLayoutBank.get(layoutIndex);
        applyLayout(layout);
        applySkin(currentSkin); // 様式が変わるので見た目も入れ直す（フロアは維持）
        return layout.name;
    }

    /**
     * 全 UI へ一括でスキンを適用する（演出なしの即時切替）。
     * ワールドローテート時のアニメーションは UiSwapAnimator / DeviceFramePane 側が担当する。
     */
    public void applySkin(UiSkin skin) {
        this.currentSkin = skin;
        hudPane.applySkin(skin);
        playFieldPane.applySkin(skin);
        holdPane.applySkin(skin);
        nextPane.applySkin(skin);
        characterPane.applySkin(skin);
        hintPane.applySkin(skin);
    }

    /** プレイフィールドの枠を実際の盤面矩形へ揃える（Render 確定後に一度だけ呼ぶ） */
    public void alignPlayFieldFrame(Render renderer) {
        playFieldPane.alignFrame(renderer);
    }
}
