package tetris.view;

import java.text.NumberFormat;
import java.util.Locale;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import tetris.view.UiLayout.Style;

/**
 * スコア・進行状況・セリフをまとめる情報パネル。
 *
 * 情報の並べ方は「読む頻度」で決めている:
 *   SCORE            … 一番大きく単独の行で（ゲームの結果そのもの）
 *   LINES / LEVEL    … 2 列 1 行に畳む（並べて読む値）
 *   SPIN IN / DANGER … ゲージ（{@link SegmentGauge}）。数字を読ませず面積で伝える
 *   セリフ            … 最下段に固定。左のアクセント罫で「情報ではなく声」だと分ける
 *
 * 値は右揃えの等幅で出す。桁が増減しても数字の位置が動かず、盤面から目を離さずに読める。
 */
public class HudPane extends StackPane {

    private static final NumberFormat SCORE_FORMAT = NumberFormat.getIntegerInstance(Locale.US);

    private final Label scoreCaption;
    private final Label scoreValue;
    private final Label bestCaption;
    private final Label bestValue;
    private final Label linesCaption;
    private final Label linesValue;
    private final Label levelCaption;
    private final Label levelValue;
    private final Label dialogueLabel;
    private final Region dialogueMark;
    private final SegmentGauge rotateGauge;
    private final SegmentGauge dangerGauge;
    private final PanelHeader header;
    private final Region[] rules;
    private final VBox contentBox;
    // 短い HUD で中身がはみ出しても、隣のパネルへ文字が流れ出さないようにする
    private final Rectangle clip;
    // 背景画像は ImageView で持つ。setBackground だと inline style（-fx-background-color）に
    // 上書きされて画像が表示されないため、子ノードとして重ねる（NextPane と同方式）
    private final PanelBackground backgroundView;

    private final VBox scoreBox;
    private final HBox statsRow;
    private final VBox gaugeBox;
    private final HBox dialogueBox;

    private UiSkin skin = UiSkinBank.forStep(0);
    private Style style = Style.PANEL;
    private int lastRotateRemaining = Integer.MIN_VALUE;
    private int lastDangerStreak = Integer.MIN_VALUE;

    public HudPane() {
        setAlignment(Pos.TOP_LEFT);

        backgroundView = new PanelBackground();

        header = new PanelHeader("STATUS");

        scoreCaption = new Label("SCORE");
        scoreValue = new Label("0");
        scoreValue.setMaxWidth(Double.MAX_VALUE);
        scoreValue.setAlignment(Pos.CENTER_RIGHT);
        bestCaption = new Label("BEST");
        bestValue = new Label("0");
        bestValue.setMaxWidth(Double.MAX_VALUE);
        bestValue.setAlignment(Pos.CENTER_RIGHT);
        HBox bestRow = new HBox(bestCaption, bestValue);
        HBox.setHgrow(bestValue, Priority.ALWAYS);
        scoreBox = new VBox(-2, scoreCaption, scoreValue, bestRow);

        linesCaption = new Label("LINES");
        linesValue = new Label("0");
        levelCaption = new Label("LEVEL");
        levelValue = new Label("1");
        statsRow = new HBox(
                statCell(linesCaption, linesValue), spacer(), statCell(levelCaption, levelValue));
        statsRow.setAlignment(Pos.CENTER_LEFT);

        rotateGauge = new SegmentGauge("SPIN IN");
        dangerGauge = new SegmentGauge("DANGER");
        gaugeBox = new VBox(10, rotateGauge, dangerGauge);

        dialogueLabel = new Label();
        dialogueLabel.setWrapText(true);
        dialogueMark = new Region();
        dialogueMark.setPrefWidth(3);
        dialogueMark.setMinWidth(3);
        dialogueBox = new HBox(12, dialogueMark, dialogueLabel);
        HBox.setHgrow(dialogueLabel, Priority.ALWAYS);
        dialogueBox.setMinHeight(54);

        // セリフは常に最下段へ貼り付ける。パネルが縦長の配置でも位置が変わらないように
        Region bottomSpacer = new Region();
        VBox.setVgrow(bottomSpacer, Priority.ALWAYS);

        rules = new Region[] { rule(), rule(), rule() };

        // 区画の間にも伸びる余白を挟む。縦長の配置で「下だけ大穴」にならず、
        // 余った高さが区画の間へ均等に配られる
        contentBox = new VBox(8,
                header,
                scoreBox, rules[0], gap(),
                statsRow, rules[1], gap(),
                gaugeBox,
                bottomSpacer,
                rules[2], dialogueBox);
        contentBox.setAlignment(Pos.TOP_LEFT);
        contentBox.setPadding(new Insets(UiMetrics.PAD));

        getChildren().addAll(backgroundView, contentBox);
        clip = new Rectangle();
        setClip(clip);

        setPaneSize(512, 432);
        applySkin(skin);
    }

    /** 見出し + 値を縦に組んだ 1 区画（LINES / LEVEL 用） */
    private static VBox statCell(Label caption, Label value) {
        VBox cell = new VBox(-2, caption, value);
        cell.setAlignment(Pos.TOP_LEFT);
        return cell;
    }

    private static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    /**
     * 区画の間に入る可変余白。上限を設けているのは、縦長の配置で余りを全部ここに配ると
     * 読み取り行どうしが離れすぎて「1 枚のパネル」に見えなくなるため。
     * 上限を超えたぶんは最下段（セリフの上）へ寄る。
     */
    private static Region gap() {
        Region r = new Region();
        r.setMaxHeight(26);
        VBox.setVgrow(r, Priority.ALWAYS);
        return r;
    }

    private static Region rule() {
        Region r = new Region();
        r.setPrefHeight(1);
        r.setMinHeight(1);
        r.setMaxHeight(1);
        return r;
    }

    /**
     * 配置定義の様式を適用する。
     *
     * OVERLAY は構図ラフの「右下の小枠」向け。見出し帯・背景テクスチャ・区切り罫・
     * セリフを落として、細い枠の中に数字とゲージだけを残す。立ち絵の上に浮く枠なので、
     * パネル様式のまま置くと絵の上に計器盤を貼ったように見えてしまう。
     */
    public void applyStyle(Style newStyle) {
        this.style = newStyle;
        boolean compact = newStyle == Style.OVERLAY;

        header.setVisible(!compact);
        header.setManaged(!compact);
        dialogueBox.setVisible(!compact);
        dialogueBox.setManaged(!compact);
        for (Region r : rules) {
            r.setVisible(!compact);
            r.setManaged(!compact);
        }
        contentBox.setSpacing(compact ? 6 : 8);
        contentBox.setPadding(new Insets(compact ? 14 : UiMetrics.PAD));

        applySkin(skin);
    }

    /** OVERLAY 時にセリフをどこへ出すかは外側（GameView）が決める */
    public boolean isCompact() {
        return style == Style.OVERLAY;
    }

    /** 配置定義に合わせてパネルの大きさを変える（中身は VBox が追従する） */
    public void setPaneSize(double width, double height) {
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);
        backgroundView.setBox(width, height);
        contentBox.setPrefSize(width, height);
        clip.setWidth(width);
        clip.setHeight(height);
        dialogueLabel.setMaxWidth(width - UiMetrics.PAD * 2 - 20);
    }

    public void updateScore(int score) {
        scoreValue.setText(SCORE_FORMAT.format(score));
    }

    /** ハイスコア表示。ゲーム開始時に一度だけ設定する */
    public void setBestScore(int best) {
        bestValue.setText(SCORE_FORMAT.format(best));
    }

    public void updateLines(int lines) {
        linesValue.setText(String.valueOf(lines));
    }

    public void updateLevel(int level) {
        levelValue.setText(String.valueOf(level));
    }

    /** ワールド回転までの残りライン数。ゲージは「消化済み」ぶんを点灯させる */
    public void updateRotateCountdown(int remaining, int interval) {
        if (remaining == lastRotateRemaining) return;
        lastRotateRemaining = remaining;
        rotateGauge.setValue(interval - remaining, interval);
    }

    /** 仮ゲームオーバーの残ライフをゲージで常時表示する */
    public void updateDangerGauge(int streak, int max) {
        if (streak == lastDangerStreak) return;
        lastDangerStreak = streak;
        dangerGauge.setValue(streak, max);
    }

    /** キャラのセリフを表示する。軽いフェードインで「喋った感」を出す */
    public void showDialogue(String text) {
        if (text == null || text.isEmpty()) return;
        dialogueLabel.setText(text);
        FadeTransition ft = new FadeTransition(Duration.millis(200), dialogueLabel);
        ft.setFromValue(0.3);
        ft.setToValue(1.0);
        ft.play();
    }

    // ============================================================
    //  スキン適用（ワールドローテートごとに呼ばれる）
    //  見た目の定義は UiSkinBank に集約されている
    // ============================================================
    public void applySkin(UiSkin newSkin) {
        this.skin = newSkin;

        setStyle(style == Style.OVERLAY ? skin.compactPanelStyle() : skin.panelStyle());
        header.applySkin(skin);
        header.setTag(skin.name);

        String captionStyle = skin.fontStyle(UiMetrics.FONT_LABEL)
            + " -fx-text-fill: " + KowloonPalette.rgba(KowloonPalette.LIGHT_HEX, 0.65) + ";";
        scoreCaption.setStyle(captionStyle);
        linesCaption.setStyle(captionStyle);
        levelCaption.setStyle(captionStyle);
        bestCaption.setStyle(captionStyle);
        bestValue.setStyle(skin.fontStyle(UiMetrics.FONT_LABEL)
            + " -fx-text-fill: " + KowloonPalette.rgba(KowloonPalette.LIGHT_HEX, 0.65) + ";");

        scoreValue.setStyle(skin.fontStyle(UiMetrics.FONT_SCORE)
            + " -fx-font-weight: bold;"
            + " -fx-text-fill: " + skin.theme.textColor + ";"
            + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.85), 6, 0.0, 2, 2);");
        linesValue.setStyle(skin.fontStyle(UiMetrics.FONT_VALUE)
            + " -fx-text-fill: " + skin.theme.textColor + ";");
        levelValue.setStyle(skin.fontStyle(UiMetrics.FONT_VALUE)
            + " -fx-font-weight: bold;"
            + " -fx-text-fill: " + skin.theme.accentColor + ";");

        rotateGauge.applySkin(skin, KowloonPalette.LIGHT_HEX);
        dangerGauge.applySkin(skin, KowloonPalette.NEON_HEX);

        dialogueLabel.setStyle(skin.fontStyle(UiMetrics.FONT_BODY) + " -fx-line-spacing: 5px;"
            + " -fx-text-fill: " + skin.theme.textColor + ";");
        dialogueMark.setStyle("-fx-background-color: "
            + KowloonPalette.rgba(skin.theme.accentColor, 0.8) + ";");

        for (Region r : rules) {
            r.setStyle("-fx-background-color: "
                + KowloonPalette.rgba(KowloonPalette.LIGHT_HEX, 0.18) + ";");
        }

        // OVERLAY では壁テクスチャを敷かない。立ち絵の上に別の絵が乗って濁るため
        backgroundView.setVisible(style != Style.OVERLAY);
        backgroundView.apply(skin.hudBgImage, ImageAssets.HUD_BG_DEFAULT, Backdrop.PANEL);
    }

    public void showScorePopup(int addedScore) {
        Label popup = PopupFx.label("+" + addedScore, 26, KowloonPalette.LIGHT_HEX);
        // スコアの数字は右揃えなので、加点は左側の空きから昇らせる。
        // 右上に出すと大きなスコア表示の真上を通って、肝心の数字が読めなくなる
        StackPane.setAlignment(popup, Pos.TOP_LEFT);
        StackPane.setMargin(popup, new Insets(
                UiMetrics.HEADER_H + UiMetrics.PAD + FxParams.HUD_POPUP_RISE_PX,
                0, 0, UiMetrics.PAD + 78));
        PopupFx.rise(this, popup,
                -FxParams.HUD_POPUP_RISE_PX, Duration.millis(FxParams.HUD_POPUP_MS));
    }

    public void showTSpinPopup(boolean mini) {
        showBigPopup(
            mini ? "T-SPIN MINI!" : "T-SPIN!",
            mini ? KowloonPalette.LIGHT_HEX : KowloonPalette.NEON_HEX
        );
    }

    public void showRenPopup(int combo) {
        showBigPopup(combo + " REN!", KowloonPalette.RUST_HEX);
    }

    private void showBigPopup(String text, String colorHex) {
        Label popup = PopupFx.label(text, 26, colorHex);
        StackPane.setAlignment(popup, Pos.CENTER);
        PopupFx.burst(this, popup,
                Duration.millis(FxParams.BIG_POPUP_SCALE_MS),
                Duration.millis(FxParams.BIG_POPUP_FADE_MS),
                Duration.millis(FxParams.BIG_POPUP_FADE_DELAY_MS));
    }
}
