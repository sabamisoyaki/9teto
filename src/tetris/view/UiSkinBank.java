package tetris.view;

import java.nio.file.Path;
import java.util.List;

import tetris.ResourcePath;

/**
 * スキンの定義一覧。スキンを増やす・差し替えるときはこのファイルの SKINS に
 * 1 エントリ追加／編集するだけでよい（ステップ→スキンの対応は SKINS の並び順）。
 *
 * 盤面は 90° 回転×4 で元の向きに戻るため、スキンも 4 周期で循環させる。
 * step 0 = 初期状態（盤面が元の向き）に対応する。
 */
public final class UiSkinBank {

    private static final List<UiSkin> SKINS = List.of(
        new UiSkin("CYBER", UiTheme.CYBER_BLUE,
            "Courier New", 5, 2,
            img("hud-bg.png"), img("next-bg.png"), img("playfield-bg.png"),
            img("character.png")),

        new UiSkin("EMBER", UiTheme.EMBER_RED,
            "Consolas", 0, 3,
            img("hud-bg.png"), img("next-bg.png"), img("playfield-bg.png"),
            img("character-rotate-1.png")),

        new UiSkin("NEON", UiTheme.NEON_GREEN,
            "Verdana", 14, 2,
            img("hud-bg.png"), img("next-bg.png"), img("playfield-bg.png"),
            img("character-rotate-2.png")),

        new UiSkin("VIOLET", UiTheme.VIOLET,
            "Georgia", 8, 2,
            img("hud-bg.png"), img("next-bg.png"), img("playfield-bg.png"),
            img("character-rotate-3.png"))
    );

    /** ワールド回転ステップ（または任意の連番）に対応するスキンを返す */
    public static UiSkin forStep(int step) {
        return SKINS.get(Math.floorMod(step, SKINS.size()));
    }

    public static int skinCount() {
        return SKINS.size();
    }

    private static Path img(String filename) {
        return ResourcePath.of("images", filename);
    }

    private UiSkinBank() {
    }
}
