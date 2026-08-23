package tetris.view;

import java.nio.file.Files;
import java.nio.file.Path;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import tetris.ResourcePath;

/**
 * 画像アセットの共有パス定数と、存在チェック付きロード処理の一元化。
 *
 * フォールバック方針は ASSETS.md の表と一致させる:
 *   全画面背景 = 黒 / パネル背景 = 透過(null) / キャラクター = 空欄
 *
 * 適用方式の使い分け:
 *   - setImage: 前景として見せる画像（キャラ立ち絵）。加工しない
 *   - setBackdrop: パネル背景。ImageView 方式なので inline style
 *     (-fx-background-color) と共存でき、下地のパレット色が透ける
 *   - addBackdropView: 全画面背景。Background にはエフェクトを付けられないため
 *     ImageView を最背面に挿入し、その下をパレット色で塗る
 *
 * **背景として敷く画像は必ず Backdrop を通す**こと。素のまま敷くと背景アートが
 * 前景の情報と競合して画面が読めなくなる（理由は Backdrop の javadoc）。
 */
public final class ImageAssets {

    // ---- 複数クラスから参照される共有アセット ----
    // 背景は tools/GenerateSkinImages.java が描いた「増築ユニットの壁」を使う。
    // 旧アセット（base-layer-1920x1080.png / start-bg.png / character-closeup-bg.png 等）は
    // それ自体が1枚絵で独自の看板・文字を持っており、前景の情報と競合するため外した。
    // ファイルは images/archive/ に残してあるので、戻すならここのパスを差し替えるだけでよい。
    //
    // 置き場の使い分け（ASSETS.md と対にすること）:
    //   images/          … 手で置く既定・フォールバック
    //   images/skin/     … GenerateSkinImages の出力
    //   images/mock/     … BuildRoughMocks の出力
    //   images/character/… 差し替え用の立ち絵
    //   images/archive/  … 未参照。配布には含めない
    public static final Path BASE_LAYER           = skinImg("bg-kowloon-base-layer.png");
    public static final Path END_CREDIT_BG        = img("end-credit-bg.png");
    /** 専用のスタート画面背景。無ければ BASE_LAYER にフォールバックする */
    public static final Path START_BG             = img("bg-kowloon-start.png");
    /** 専用のゲームオーバー背景。無ければ BASE_LAYER にフォールバックする */
    public static final Path GAME_OVER_BG         = img("bg-kowloon-game-over.png");
    public static final Path HUD_BG_DEFAULT       = img("hud-bg.png");
    public static final Path NEXT_BG_DEFAULT      = img("next-bg.png");
    public static final Path PLAYFIELD_BG_DEFAULT = img("playfield-bg.png");
    public static final Path CHARACTER_DEFAULT    = img("character.png");
    public static final Path CHARACTER_CLOSEUP_BG = skinImg("bg-kowloon-character-panel.png");

    private static Path img(String filename) {
        return ResourcePath.of("images", filename);
    }

    /** 生成物の置き場。tools/GenerateSkinImages.java の出力先と対にすること */
    private static Path skinImg(String filename) {
        return ResourcePath.of("images", "skin", filename);
    }

    /** 存在すれば Image、無ければ null */
    public static Image loadOrNull(Path path) {
        if (path == null || !Files.exists(path)) {
            return null;
        }
        return new Image(path.toUri().toString());
    }

    /** primary → fallback の順に試す。両方無ければ null */
    public static Image loadOrNull(Path primary, Path fallback) {
        Image image = loadOrNull(primary);
        return image != null ? image : loadOrNull(fallback);
    }

    /** ImageView へそのまま適用する。前景（キャラ立ち絵）専用 */
    public static void setImage(ImageView view, Path primary, Path fallback) {
        view.setImage(loadOrNull(primary, fallback));
    }

    /** パネル背景の ImageView へ、画像と Backdrop 処理をまとめて適用する */
    public static void setBackdrop(ImageView view, Path primary, Path fallback, Backdrop backdrop) {
        view.setImage(loadOrNull(primary, fallback));
        backdrop.applyTo(view);
    }

    /**
     * Pane の最背面(index 0)へフルスクリーン背景を挿入する。
     * ぼかしで縁が透けるため、下地は必ずパレットの煤黒で塗っておく。
     */
    public static void addBackdropView(
            Pane target, Path path, double width, double height, Backdrop backdrop) {
        target.setStyle("-fx-background-color: " + KowloonPalette.SHADOW_HEX + ";");

        Image image = loadOrNull(path);
        if (image == null) {
            return; // 下地の煤黒がそのまま背景になる
        }
        ImageView view = new ImageView(image);
        view.setFitWidth(width);
        view.setFitHeight(height);
        backdrop.applyTo(view);
        target.getChildren().add(0, view);
    }

    private ImageAssets() {
    }
}
