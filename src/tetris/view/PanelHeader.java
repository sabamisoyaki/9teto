package tetris.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * 全パネル共通の見出し帯。左に見出し・右に補助タグを置き、下にアクセント色の罫を敷く。
 *
 * パネルごとに見出しの出し方を変えないための部品。位置や大きさが配置定義で変わっても、
 * 同じ帯が乗っているだけで「同じ様式のパネル」に見える。
 */
public final class PanelHeader extends VBox {

    private final Label titleLabel;
    private final Label tagLabel;
    private final Region rule;

    public PanelHeader(String title) {
        titleLabel = new Label(title);
        tagLabel = new Label();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(titleLabel, spacer, tagLabel);
        row.setAlignment(Pos.BOTTOM_LEFT);
        row.setPadding(new Insets(0, 0, 5, 0));

        rule = new Region();
        rule.setPrefHeight(2);
        rule.setMinHeight(2);
        rule.setMaxHeight(2);

        getChildren().addAll(row, rule);
        setMinHeight(UiMetrics.HEADER_H);
        setPrefHeight(UiMetrics.HEADER_H);
    }

    /** 見出し右端の補助表示（フロア名・状態など）。null / 空文字で消える */
    public void setTag(String text) {
        tagLabel.setText(text == null ? "" : text);
    }

    public void applySkin(UiSkin skin) {
        titleLabel.setStyle(skin.fontStyle(UiMetrics.FONT_HEADER)
            + " -fx-font-weight: bold;"
            + " -fx-text-fill: " + skin.theme.accentColor + ";"
            + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.9), 4, 0.0, 1, 1);");
        tagLabel.setStyle(skin.fontStyle(UiMetrics.FONT_HINT)
            + " -fx-text-fill: " + KowloonPalette.rgba(KowloonPalette.LIGHT_HEX, 0.5) + ";");
        rule.setStyle("-fx-background-color: " + KowloonPalette.rgba(skin.theme.accentColor, 0.7) + ";");
    }
}
