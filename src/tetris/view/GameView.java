package tetris.view;

import java.nio.file.Files;
import java.nio.file.Path;
import tetris.ResourcePath;

import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

public class GameView {

    private static final Path DEFAULT_BACKGROUND_IMAGE = ResourcePath.of("images", "base-layer-1920x1080.png");

    private static final double WINDOW_WIDTH  = 1920;
    private static final double WINDOW_HEIGHT = 1080;
    private static final double PLAY_FIELD_SIZE = 840;
    private static final double NEXT_HEIGHT     = 168; // 7:1 vertical split (840:168)
    private static final double PREVIEW_WIDTH   = PLAY_FIELD_SIZE / 2;

    // パネルは固定座標で配置する（配置定義は UiLayoutBank が一元管理）
    private final Pane root;
    private final NextPane holdPane;
    private final NextPane nextPane;
    private final DeviceFramePane playFieldPane;
    private final CharacterPane characterPane;
    private final HudPane hudPane;

    // 起動直後から NEXT 隣接クラスタを表示する。
    private int layoutIndex = 4;

    public GameView() {
        this.root = new Pane();
        root.setPrefSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        root.setMinSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        root.setMaxSize(WINDOW_WIDTH, WINDOW_HEIGHT);

        this.playFieldPane = new DeviceFramePane(PLAY_FIELD_SIZE);
        this.holdPane = new NextPane(PREVIEW_WIDTH, NEXT_HEIGHT, "HOLD (H)");
        this.nextPane = new NextPane(PREVIEW_WIDTH, NEXT_HEIGHT, "NEXT");
        this.characterPane = new CharacterPane();
        this.hudPane = new HudPane();

        // 追加順 = 重なり順。キャラの立ち絵を最前面にする
        root.getChildren().addAll(playFieldPane, holdPane, nextPane, hudPane, characterPane);

        loadBaseLayerImage(DEFAULT_BACKGROUND_IMAGE);
        applyLayout(UiLayoutBank.get(layoutIndex));

        // 初期スキンを演出なしで適用（ゲーム開始時のフラッシュ暴発を防ぐ）
        applySkin(UiSkinBank.forStep(0));
    }

    private void loadBaseLayerImage(Path imagePath) {
        if (imagePath == null || !Files.exists(imagePath)) {
            root.setStyle("-fx-background-color: black;");
            return;
        }
        Image image = new Image(imagePath.toUri().toString());
        BackgroundSize backgroundSize = new BackgroundSize(
                WINDOW_WIDTH, WINDOW_HEIGHT, false, false, false, false);
        BackgroundImage backgroundImage = new BackgroundImage(
                image,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                backgroundSize);
        root.setBackground(new Background(backgroundImage));
    }

    public Region getRoot()                      { return root; }
    public NextPane getHoldPane()                { return holdPane; }
    public NextPane getNextPane()                { return nextPane; }
    public DeviceFramePane getPlayFieldPane()    { return playFieldPane; }
    public CharacterPane getCharacterPane()      { return characterPane; }
    public HudPane getHudPane()                  { return hudPane; }

    /** 各パネルを配置定義どおりの座標へ移動する */
    public void applyLayout(UiLayout layout) {
        place(playFieldPane, layout.playfield);
        place(holdPane,      layout.hold);
        place(nextPane,      layout.next);
        place(characterPane, layout.character);
        place(hudPane,       layout.hud);
    }

    private static void place(Region pane, Point2D pos) {
        pane.setLayoutX(pos.getX());
        pane.setLayoutY(pos.getY());
    }

    /** 次の配置定義へ切り替えて、その名前を返す（プレビュー用） */
    public String cycleLayout() {
        layoutIndex = (layoutIndex + 1) % UiLayoutBank.count();
        UiLayout layout = UiLayoutBank.get(layoutIndex);
        applyLayout(layout);
        return layout.name;
    }

    /**
     * 全 UI へ一括でスキンを適用する（演出なしの即時切替）。
     * ワールドローテート時のアニメーションは UiSwapAnimator / DeviceFramePane 側が担当する。
     */
    public void applySkin(UiSkin skin) {
        hudPane.applySkin(skin);
        playFieldPane.applySkin(skin);
        holdPane.applySkin(skin);
        nextPane.applySkin(skin);
        characterPane.applySkin(skin);
    }
}
