package tetris.view;

import java.nio.file.Path;
import java.util.List;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import tetris.ResourcePath;
import tetris.model.ScenarioPage;
import tetris.model.ScenarioRoute;

/**
 * アドベンチャーパートの画面。立ち絵とテキスト窓を出して、1 ページずつ送る。
 *
 * <p>骨格は {@link EndCreditPane} と同じ（1920×1080 の StackPane ＋ 背景 ＋ 暗幕 ＋ 中身）。
 * 違うのは中身と、アニメーションではなく<b>入力で進む</b>こと。
 *
 * <p>{@link CharacterPane} は流用しない。あちらはパネル用のクリップとスキン連動を
 * 抱えていて、全画面で使うと邪魔になる。ここは ImageView 1 枚で足りる。
 *
 * <p>オープニングにもエンディングにも回想にもこれ 1 つを使う。違いは
 * 「どのルートを渡すか」と「終わったあとどこへ行くか」だけ。
 */
public class AdventurePane {

    private static final double WIDTH = 1920;
    private static final double HEIGHT = 1080;

    /** 立ち絵の高さ。画面の 8 割。顔が上に来るよう下端を画面外へ逃がす */
    private static final double CHARACTER_HEIGHT = HEIGHT * 0.82;

    /** テキスト窓の左右余白と高さ。UiMetrics.MARGIN(40) の倍尺で画面の広さに合わせる */
    private static final double BOX_MARGIN = 80;
    private static final double BOX_HEIGHT = 260;

    /** ページ差し替えのフェード。長いと送りが重く感じる */
    private static final Duration PAGE_FADE = Duration.millis(140);

    private final StackPane root;
    private final ImageView characterView;
    private final Label speakerLabel;
    private final Label textLabel;
    private final VBox textBox;

    private final List<ScenarioPage> pages;
    private int index = -1;
    private String shownBackground = null;
    private boolean backgroundApplied = false;

    public AdventurePane(ScenarioRoute route) {
        this.pages = route.resolvedPages();

        root = new StackPane();
        root.setPrefSize(WIDTH, HEIGHT);
        root.setStyle("-fx-background-color: " + KowloonPalette.SHADOW_HEX + ";");

        // 暗幕。立ち絵と文字を読ませるため、背景は必ず沈める
        Rectangle veil = new Rectangle(WIDTH, HEIGHT);
        veil.setFill(KowloonPalette.alpha(KowloonPalette.SHADOW, 0.55));

        characterView = new ImageView();
        characterView.setPreserveRatio(true);
        characterView.setSmooth(true);
        characterView.setMouseTransparent(true);
        characterView.setFitHeight(CHARACTER_HEIGHT);
        StackPane.setAlignment(characterView, Pos.BOTTOM_CENTER);

        speakerLabel = new Label();
        speakerLabel.setStyle(MenuStyle.value(28, KowloonPalette.NEON_HEX));

        textLabel = new Label();
        textLabel.setWrapText(true);
        textLabel.setStyle(MenuStyle.value(30, KowloonPalette.LIGHT_HEX));

        Label hint = new Label("SPACE  ·  送る        ESC  ·  スキップ");
        hint.setStyle(MenuStyle.hint());

        HBox hintRow = new HBox(hint);
        hintRow.setAlignment(Pos.CENTER_RIGHT);

        textBox = new VBox(12, speakerLabel, textLabel, hintRow);
        // 窓は不透明にする。MenuStyle.box() は 0.72 だが、あちらは背後に立ち絵が
        // 来ない画面（タイトル・ゲームオーバー）向け。ここは立ち絵が窓の裏に立つうえ
        // 線画が明るいので、わずかでも透かすと線が本文に重なって読みにくい
        textBox.setStyle(
                "-fx-background-color: " + KowloonPalette.SHADOW_HEX + ";"
                + " -fx-border-color: " + KowloonPalette.rgba(KowloonPalette.LIGHT_HEX, 0.35) + ";"
                + " -fx-border-width: 1px;");
        textBox.setPadding(new Insets(24, 40, 20, 40));
        textBox.setPrefHeight(BOX_HEIGHT);
        textBox.setMinHeight(BOX_HEIGHT);
        textBox.setMaxHeight(BOX_HEIGHT);
        textBox.setMaxWidth(WIDTH - BOX_MARGIN * 2);
        StackPane.setAlignment(textBox, Pos.BOTTOM_CENTER);
        StackPane.setMargin(textBox, new Insets(0, BOX_MARGIN, BOX_MARGIN, BOX_MARGIN));

        // 本文が伸びてもヒント行は下端に貼り付ける
        VBox.setVgrow(textLabel, Priority.ALWAYS);

        root.getChildren().addAll(veil, characterView, textBox);
    }

    public StackPane getRoot() {
        return root;
    }

    public boolean isEmpty() {
        return pages.isEmpty();
    }

    /**
     * 次のページへ進める。
     *
     * @return まだページが残っていれば true。最後まで送り切ったら false
     */
    public boolean advance() {
        if (index + 1 >= pages.size()) {
            return false;
        }
        index++;
        show(pages.get(index));
        return true;
    }

    private void show(ScenarioPage page) {
        applyBackground(page.background());

        Path art = characterPath(page.character());
        characterView.setImage(art == null ? null : ImageAssets.loadOrNull(art));
        characterView.setVisible(characterView.getImage() != null);

        speakerLabel.setText(page.hasSpeaker() ? page.speaker() : "");
        speakerLabel.setVisible(page.hasSpeaker());
        speakerLabel.setManaged(page.hasSpeaker());
        textLabel.setText(page.text());

        fadeIn(textBox);
        if (characterView.isVisible()) {
            fadeIn(characterView);
        }
    }

    /** 背景は変わったときだけ敷き直す。毎ページ作り直すと点滅する */
    private void applyBackground(String name) {
        if (backgroundApplied && (name == null || name.equals(shownBackground))) {
            return;
        }
        backgroundApplied = true;
        shownBackground = name;

        // 既に敷いてある背景（index 0）を外してから入れ直す
        if (!root.getChildren().isEmpty()
                && root.getChildren().get(0) instanceof ImageView) {
            root.getChildren().remove(0);
        }
        Path path = backgroundPath(name);
        ImageAssets.addBackdropView(root, path, WIDTH, HEIGHT, Backdrop.FAR);
    }

    /** 立ち絵は名前だけを JSON に書く。置き場が変わってもシナリオを直さなくていい */
    private static Path characterPath(String name) {
        if (name == null || name.isBlank()) return null;
        return ResourcePath.of("images", "character", name + ".png");
    }

    private static Path backgroundPath(String name) {
        if (name == null || name.isBlank()) return ImageAssets.BASE_LAYER;
        Path skin = ResourcePath.of("images", "skin", name);
        if (java.nio.file.Files.exists(skin)) return skin;

        // START_BG など画面専用の背景は images/ 直下に置く既存契約なので、
        // skin/ に無い場合はこちらも探す。どちらにも無ければ従来どおり共通背景へ戻す。
        Path shared = ResourcePath.of("images", name);
        return java.nio.file.Files.exists(shared) ? shared : ImageAssets.BASE_LAYER;
    }

    private static void fadeIn(Node node) {
        FadeTransition f = new FadeTransition(PAGE_FADE, node);
        f.setFromValue(0.0);
        f.setToValue(1.0);
        f.play();
    }
}
