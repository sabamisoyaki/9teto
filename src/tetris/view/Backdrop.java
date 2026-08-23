package tetris.view;

import javafx.scene.CacheHint;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;

/**
 * 背景画像の「引っ込め方」の定義。
 *
 * 背景アセットはどれもそれ自体で完成した1枚絵で、看板・文字・別UIといった
 * 独自の情報を持っている。素のまま敷くと、前景の情報（スコア・NEXT・盤面）と
 * 同じ強さで主張してしまい、画面が「密度が高い」ではなく「読めない」になる。
 *
 * 九龍城の密度は同じ部品の反復で作るものなので、背景に要求するのは
 * **絵柄ではなく素材感**。ここでぼかし・脱色・減光・不透明度をまとめて掛け、
 * 背景を「絵」から「面」へ落とす。
 *
 * 不透明度を下げるのが要点で、下に敷かれたパレット色（パネルの
 * -fx-background-color / ルートの下地）が透けて、どの背景画像を使っても
 * 最終的な色味がパレットへ寄る。
 *
 * 強度を変えたいときはこの表だけを触る。呼び出し側に数値を書かないこと。
 */
public enum Backdrop {

    /**
     * 全画面の最背面。生成済みの壁をさらに一段沈めて、パネルとの前後を作る。
     */
    FAR(2, 0.0, -0.12, -0.05, 0.85),

    /**
     * HUD / NEXT / HOLD のパネル背景。生成時点で文字も看板も持たないので
     * ぼかす必要がない。不透明度だけ下げてパネルのパレット色と馴染ませる。
     */
    PANEL(0, 0.0, 0.0, 0.0, 0.85),

    /**
     * プレイフィールド背景。ミノとグリッドの視認が最優先なので一番静かにする。
     */
    FIELD(0, 0.0, -0.10, -0.10, 0.70),

    /**
     * 立ち絵の後ろ。キャラのシルエットを浮かせるため軽く沈める。
     */
    CHARACTER(2, 0.0, -0.08, 0.0, 0.80),

    /**
     * エンドクレジット。ここだけは手描きアート（九龍城寨の窓）が主役なので、
     * 文字が読める程度に落とすだけに留める。
     */
    CREDIT(4, -0.45, -0.20, -0.10, 0.80);

    private final double blurRadius;
    private final double saturation;
    private final double brightness;
    private final double contrast;
    private final double opacity;

    Backdrop(double blurRadius, double saturation, double brightness, double contrast, double opacity) {
        this.blurRadius = blurRadius;
        this.saturation = saturation;
        this.brightness = brightness;
        this.contrast   = contrast;
        this.opacity    = opacity;
    }

    /** 背景として使う ImageView へ処理を適用する */
    public void applyTo(ImageView view) {
        // ColorAdjust の出力を GaussianBlur の入力にする（＝脱色・減光してからぼかす）
        GaussianBlur blur = new GaussianBlur(blurRadius);
        blur.setInput(new ColorAdjust(0, saturation, brightness, contrast));

        view.setEffect(blur);
        view.setOpacity(opacity);
        view.setMouseTransparent(true);
        // 静止した背景なのでラスタをキャッシュし、毎フレームのぼかし再計算を避ける
        view.setCache(true);
        view.setCacheHint(CacheHint.SPEED);
    }
}
