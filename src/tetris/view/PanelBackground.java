package tetris.view;

import java.nio.file.Path;

import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * パネル背景用の ImageView。枠の大きさを覚えておき、画像を**歪ませずに**枠を覆う
 * （中央クロップ＝CSS の background-size: cover 相当）。
 *
 * 配置定義（{@link UiLayout}）でパネルの縦横比が変わるようになったため、素の
 * fitWidth / fitHeight だと壁のテクスチャが引き伸ばされて、同じ壁が配置ごとに
 * 別の素材に見えてしまう。ここで必ずクロップに寄せる。
 */
public final class PanelBackground extends ImageView {

    private double boxWidth;
    private double boxHeight;

    public PanelBackground() {
        setMouseTransparent(true);
    }

    /** 覆う枠の大きさを設定する */
    public void setBox(double width, double height) {
        this.boxWidth = width;
        this.boxHeight = height;
        refit();
    }

    /** 画像と Backdrop 処理をまとめて適用する（適用後にクロップし直す） */
    public void apply(Path primary, Path fallback, Backdrop backdrop) {
        ImageAssets.setBackdrop(this, primary, fallback, backdrop);
        refit();
    }

    private void refit() {
        setFitWidth(boxWidth);
        setFitHeight(boxHeight);

        Image image = getImage();
        if (image == null || boxWidth <= 0 || boxHeight <= 0) {
            setViewport(null);
            return;
        }

        double srcW = image.getWidth();
        double srcH = image.getHeight();
        if (srcW <= 0 || srcH <= 0) {
            setViewport(null);
            return;
        }

        // 枠を覆う最小倍率を選び、その倍率で必要な範囲だけを中央から切り出す
        double scale = Math.max(boxWidth / srcW, boxHeight / srcH);
        double viewW = Math.min(srcW, boxWidth / scale);
        double viewH = Math.min(srcH, boxHeight / scale);
        setViewport(new Rectangle2D((srcW - viewW) / 2, (srcH - viewH) / 2, viewW, viewH));
    }
}
