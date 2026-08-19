package tetris.view;

import java.nio.file.Path;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import tetris.model.Tetromino;
import tetris.view.UiLayout.Style;

/**
 * NEXT / HOLD のプレビューパネル。見出し帯（{@link PanelHeader}）＋ミノ表示枠の 2 段構成。
 *
 * ミノは盤面と同じセルサイズでは小さすぎて形が読めないので、枠の大きさから毎回
 * セルサイズを決めて描く（{@link #draw}）。どの配置でも枠いっぱいに大きく出る。
 */
public class NextPane extends StackPane {

    private final Canvas previewCanvas;
    private final PanelBackground backgroundView;
    private final PanelHeader header;
    private final Label compactLabel;

    private Style style = Style.PANEL;
    private UiSkin skin = UiSkinBank.forStep(0);

    public NextPane(double width, double height) {
        this(width, height, "NEXT");
    }

    public NextPane(double width, double height, String title) {
        setAlignment(Pos.TOP_LEFT);

        backgroundView = new PanelBackground();

        previewCanvas = new Canvas();

        header = new PanelHeader(title);
        header.setPadding(new Insets(UiMetrics.PAD, UiMetrics.PAD, 0, UiMetrics.PAD));

        // OVERLAY 用の見出し。帯を持たず、枠の左上に小さな文字を置くだけ
        compactLabel = new Label(title);
        compactLabel.setVisible(false);
        StackPane.setAlignment(compactLabel, Pos.TOP_LEFT);
        StackPane.setMargin(compactLabel, new Insets(6, 0, 0, 8));

        getChildren().addAll(backgroundView, previewCanvas, header, compactLabel);

        setPaneSize(width, height);
        // 初期見た目もスキン定義から取る（パレット外の色をここに持たない）
        applySkin(UiSkinBank.forStep(0));
    }

    /**
     * 配置定義の様式を適用する。OVERLAY では見出し帯と壁テクスチャを落とし、
     * 枠だけ細く残して「立ち絵の上に置かれた小窓」に寄せる。
     */
    public void applyStyle(Style newStyle) {
        this.style = newStyle;
        boolean compact = newStyle == Style.OVERLAY;

        header.setVisible(!compact);
        header.setManaged(!compact);
        compactLabel.setVisible(compact);
        backgroundView.setVisible(!compact);

        applySkin(skin);
    }

    /** 配置定義に合わせてパネルの大きさを変える */
    public void setPaneSize(double width, double height) {
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);
        backgroundView.setBox(width, height);
        previewCanvas.setWidth(width);
        previewCanvas.setHeight(height);
        header.setPrefWidth(width);
    }

    /**
     * 見出し帯を除いた表示枠にミノを描く。
     *
     * @param grayed HOLD が使用済みのときに true。灰色＋見出しタグで「今は使えない」を示す
     */
    public void draw(Render renderer, Tetromino piece, boolean grayed) {
        header.setTag(grayed ? "LOCKED" : "");

        double top = style == Style.OVERLAY
                ? 26                                       // 小さな見出し文字ぶんだけ空ける
                : UiMetrics.PAD + UiMetrics.HEADER_H;
        renderer.drawPreview(previewCanvas.getGraphicsContext2D(), piece,
                UiMetrics.PAD, top,
                getWidth() - UiMetrics.PAD * 2,
                getHeight() - top - UiMetrics.PAD,
                grayed);
    }

    // ============================================================
    //  スキン適用（ワールドローテートごとに呼ばれる）
    //  見た目の定義は UiSkinBank に集約されている
    // ============================================================
    public void applySkin(UiSkin newSkin) {
        this.skin = newSkin;
        setStyle(style == Style.OVERLAY ? skin.compactPanelStyle() : skin.panelStyle());
        header.applySkin(skin);
        compactLabel.setStyle(skin.fontStyle(UiMetrics.FONT_HINT)
            + " -fx-font-weight: bold;"
            + " -fx-text-fill: " + skin.theme.accentColor + ";"
            + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.9), 4, 0.0, 1, 1);");
        backgroundView.apply(skin.nextBgImage, ImageAssets.NEXT_BG_DEFAULT, Backdrop.PANEL);
    }

    public Canvas getNextCanvas() {
        return previewCanvas;
    }

    public void loadBackgroundImage(Path imagePath) {
        backgroundView.apply(imagePath, null, Backdrop.PANEL);
    }
}
