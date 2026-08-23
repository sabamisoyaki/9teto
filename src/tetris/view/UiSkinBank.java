package tetris.view;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import tetris.ResourcePath;

/**
 * スキンの定義一覧。スキンを増やす・差し替えるときはこのファイルの SKINS に
 * 1 エントリ追加／編集するだけでよい（ステップ→スキンの対応は SKINS の並び順）。
 *
 * 盤面は 90° 回転×4 で元の向きに戻るため、スキンも 4 周期で循環させる。
 * step 0 = 初期状態（盤面が元の向き）に対応する。
 *
 * 世界観は九龍城で統一し、4 ステップを「階層の移動」として見せる
 * （1F 電気街 → 5F 市場 → 9F 診療所 → RF 屋上 → 1F …）。配色は
 * {@link KowloonPalette} の 5 色のみ、割り当て表は {@link UiTheme} を参照。
 *
 * 全フロアで意図的に共通化しているもの（増やすと画面が散らかるため）:
 *   - fontFamily … 番地札・張り紙の等幅感で統一
 *   - borderRadius … 0 固定。増築は角丸にならない
 *   - 傾き … FxParams.TILT_A / TILT_B の 2 種のみ
 * フロア差は borderWidth（増築の雑さ）と UiTheme の役割割り当てで作る。
 *
 * 立ち絵だけは images/character/<フロア名>.png を置くと差し替えられる（{@link #character}）。
 * 本番の絵はそちらへ置くこと。生成ツールはこの階層を書かないので上書きされない。
 *
 * skin-* 画像は tools/GenerateSkinImages.java で生成したプレースホルダ
 * （パネル背景 = 基底画像をパレットへデュオトーン化。フロア名の焼き込みは
 * プレイフィールドのみで、HUD / NEXT は PanelHeader がフロア名を出す、
 * キャラ = 透過シルエット）。画像が無い環境では各 Pane が基底画像
 * （hud-bg.png 等）へフォールバックする。
 */
public final class UiSkinBank {

    /** 全フロア共通のフォント。張り紙・番地札の等幅感を出す */
    private static final String FONT = "Courier New";

    private static final List<UiSkin> SKINS = List.of(
        new UiSkin("1F ARCADE", UiTheme.KOWLOON_ARCADE,
            FONT, 0, 2,
            img("skin-kowloon-arcade-hud-bg.png"),
            img("skin-kowloon-arcade-next-bg.png"),
            img("skin-kowloon-arcade-playfield-bg.png"),
            character("1F-ARCADE.png", "skin-kowloon-arcade-character.png"),
            img("skin-kowloon-arcade-approach.png"), 0),

        new UiSkin("5F MARKET", UiTheme.KOWLOON_MARKET,
            FONT, 0, 3,
            img("skin-kowloon-market-hud-bg.png"),
            img("skin-kowloon-market-next-bg.png"),
            img("skin-kowloon-market-playfield-bg.png"),
            character("5F-MARKET.png", "skin-kowloon-market-character.png"),
            img("skin-kowloon-market-approach.png"), FxParams.TILT_A),

        new UiSkin("9F CLINIC", UiTheme.KOWLOON_CLINIC,
            FONT, 0, 2,
            img("skin-kowloon-clinic-hud-bg.png"),
            img("skin-kowloon-clinic-next-bg.png"),
            img("skin-kowloon-clinic-playfield-bg.png"),
            character("9F-CLINIC.png", "skin-kowloon-clinic-character.png"),
            img("skin-kowloon-clinic-approach.png"), FxParams.TILT_B),

        new UiSkin("RF ROOFTOP", UiTheme.KOWLOON_ROOFTOP,
            FONT, 0, 4,
            img("skin-kowloon-rooftop-hud-bg.png"),
            img("skin-kowloon-rooftop-next-bg.png"),
            img("skin-kowloon-rooftop-playfield-bg.png"),
            character("RF-ROOFTOP.png", "skin-kowloon-rooftop-character.png"),
            img("skin-kowloon-rooftop-approach.png"), FxParams.TILT_A)
    );

    /** ワールド回転ステップ（または任意の連番）に対応するスキンを返す */
    public static UiSkin forStep(int step) {
        return SKINS.get(Math.floorMod(step, SKINS.size()));
    }

    public static int skinCount() {
        return SKINS.size();
    }

    /** 生成物の置き場。tools/GenerateSkinImages.java の出力先と対にすること */
    private static Path img(String filename) {
        return ResourcePath.of("images", "skin", filename);
    }

    /**
     * 立ち絵のパスを決める。images/character/ に絵があればそれを、無ければ
     * 自動生成のシルエット（skin-*-character.png）を返す。
     *
     * 差し替えは images/character/ へ PNG を置くだけでよい。
     * tools/GenerateSkinImages.java はこの階層へ書かないので、手で描いた絵が
     * 生成の再実行で消えることはない。
     *
     * 判定はクラス初期化時の 1 回だけ。置き換えたらアプリを起動し直すこと。
     *
     * @param artFile         images/character/ に置く差し替え用のファイル名
     * @param placeholderFile 無かったときに使う生成物のファイル名
     */
    private static Path character(String artFile, String placeholderFile) {
        Path art = ResourcePath.of("images", "character", artFile);
        return Files.exists(art) ? art : img(placeholderFile);
    }

    private UiSkinBank() {
    }
}
