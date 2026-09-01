package tetris.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.scene.input.KeyCode;

/**
 * 操作とキーの対応。1 つの操作に複数のキーを割り当てられる
 * （既定でも右回転は X と ↑ の 2 つ）。
 *
 * <p>保存は {@link GameConfig}。書式は {@code key.ROTATE_RIGHT=X,UP} のように
 * {@link KeyCode#name()} をカンマで並べたもの。
 */
public final class KeyBindings {

    /**
     * 予約キー。ここへは割り当てさせない。
     *
     * <p>ESC はポーズ、P はポーズ、F1〜F4 はデバッグに使っており、
     * 操作へ割り当てるとゲーム中に戻れなくなったり配置が飛んだりする。
     */
    private static final Set<KeyCode> RESERVED = Set.of(
            KeyCode.ESCAPE, KeyCode.P, KeyCode.ENTER,
            KeyCode.F1, KeyCode.F2, KeyCode.F3, KeyCode.F4);

    private final Map<GameAction, Set<KeyCode>> map = new EnumMap<>(GameAction.class);

    private KeyBindings() {
    }

    /** 従来から使ってきた配置。設定ファイルが無い・壊れているときはこれに戻る */
    public static KeyBindings defaults() {
        KeyBindings b = new KeyBindings();
        b.map.put(GameAction.MOVE_LEFT,    keys(KeyCode.LEFT));
        b.map.put(GameAction.MOVE_RIGHT,   keys(KeyCode.RIGHT));
        b.map.put(GameAction.SOFT_DROP,    keys(KeyCode.DOWN));
        b.map.put(GameAction.HARD_DROP,    keys(KeyCode.SPACE));
        b.map.put(GameAction.ROTATE_LEFT,  keys(KeyCode.Z));
        b.map.put(GameAction.ROTATE_RIGHT, keys(KeyCode.X, KeyCode.UP));
        b.map.put(GameAction.HOLD,         keys(KeyCode.H));
        return b;
    }

    private static Set<KeyCode> keys(KeyCode... codes) {
        return new LinkedHashSet<>(List.of(codes));
    }

    public Set<KeyCode> keysFor(GameAction action) {
        return Set.copyOf(map.getOrDefault(action, Set.of()));
    }

    /** どれか 1 つでも押されていれば true。{@code GameController} の入力判定に使う */
    public boolean isDown(GameAction action, Set<KeyCode> pressed) {
        for (KeyCode code : map.getOrDefault(action, Set.of())) {
            if (pressed.contains(code)) return true;
        }
        return false;
    }

    /** 割り当てを持たない操作。キーコンフィグ画面で警告を出すのに使う */
    public boolean isUnbound(GameAction action) {
        return map.getOrDefault(action, Set.of()).isEmpty();
    }

    public static boolean isReserved(KeyCode code) {
        return RESERVED.contains(code);
    }

    /**
     * その操作の割り当てを、押されたキー 1 つに<b>置き換える</b>。
     *
     * <p>足すのではなく置き換えるのは、「A を設定したのに ← も効く」が
     * 驚きになるため。既定の 2 キー割り当て（右回転の X / ↑）は、
     * その操作を変更するまで残る。
     *
     * <p>同じキーが 2 つの操作に付くと押したときどちらが動くか分からなくなるので、
     * <b>他の操作からは先に外す</b>。後から割り当てたほうが勝つ。
     *
     * @return 割り当てられなければ false（予約キーだった）
     */
    public boolean assign(GameAction action, KeyCode code) {
        if (code == null || isReserved(code)) return false;
        for (Set<KeyCode> set : map.values()) {
            set.remove(code);
        }
        map.put(action, keys(code));
        return true;
    }

    /** その操作の割り当てを全部外す */
    public void clear(GameAction action) {
        map.getOrDefault(action, Set.of()).clear();
    }

    public void resetToDefaults() {
        map.clear();
        map.putAll(defaults().map);
    }

    /** 画面に出す表記。未割り当ては空文字ではなく印を返す（行が消えて見えないように） */
    public String describe(GameAction action) {
        Set<KeyCode> set = map.getOrDefault(action, Set.of());
        if (set.isEmpty()) return "（未割り当て）";
        List<String> names = new ArrayList<>();
        for (KeyCode code : set) {
            names.add(displayName(code));
        }
        return String.join(" / ", names);
    }

    /** 矢印キーは記号のほうが一目で分かる */
    public static String displayName(KeyCode code) {
        return switch (code) {
            case LEFT  -> "←";
            case RIGHT -> "→";
            case UP    -> "↑";
            case DOWN  -> "↓";
            case SPACE -> "SPACE";
            default    -> code.getName();
        };
    }

    // ==================================================
    //   保存・復元
    // ==================================================

    /** {@code KeyCode#name()} をカンマで並べた文字列。未割り当ては空文字 */
    public String encode(GameAction action) {
        List<String> names = new ArrayList<>();
        for (KeyCode code : map.getOrDefault(action, Set.of())) {
            names.add(code.name());
        }
        return String.join(",", names);
    }

    /**
     * 保存文字列から復元する。知らないキー名・予約キーは黙って捨てる
     * （手で書き換えても起動できなくならないように）。
     */
    public void decode(GameAction action, String csv) {
        if (csv == null) return;
        Set<KeyCode> set = new LinkedHashSet<>();
        for (String name : csv.split(",")) {
            String trimmed = name.trim();
            if (trimmed.isEmpty()) continue;
            try {
                KeyCode code = KeyCode.valueOf(trimmed);
                if (!isReserved(code)) set.add(code);
            } catch (IllegalArgumentException ignored) {
                // 知らないキー名。無視して次へ
            }
        }
        map.put(action, set);
    }
}
