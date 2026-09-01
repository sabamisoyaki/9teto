package tetris.model;

import static tetris.TestSupport.check;
import static tetris.TestSupport.checkEquals;

import java.util.Set;

import javafx.scene.input.KeyCode;

/** キー割り当ての回帰テスト。JavaFX の KeyCode を使うだけで、画面は起動しない。 */
public final class KeyBindingsTest {

    public static void main(String[] args) {
        hasWorkingDefaults();
        assignReplacesInsteadOfAdding();
        assignStealsFromOtherActions();
        rejectsReservedKeys();
        clearAndReset();
        survivesRoundTrip();
        ignoresGarbageOnDecode();
        System.out.println("KeyBindingsTest: PASS");
    }

    private static void hasWorkingDefaults() {
        KeyBindings b = KeyBindings.defaults();
        for (GameAction action : GameAction.values()) {
            check(!b.isUnbound(action), action + " に既定の割り当てがある");
        }
        check(b.isDown(GameAction.MOVE_LEFT, Set.of(KeyCode.LEFT)), "← で左移動");
        check(b.isDown(GameAction.ROTATE_RIGHT, Set.of(KeyCode.X)), "X で右回転");
        check(b.isDown(GameAction.ROTATE_RIGHT, Set.of(KeyCode.UP)), "↑ でも右回転");
        check(!b.isDown(GameAction.HOLD, Set.of(KeyCode.LEFT)), "無関係のキーは反応しない");
    }

    /** 「A を設定したのに ← も効く」を防ぐ。置き換えであることを固定する */
    private static void assignReplacesInsteadOfAdding() {
        KeyBindings b = KeyBindings.defaults();
        check(b.assign(GameAction.MOVE_LEFT, KeyCode.A), "割り当てに成功する");
        check(b.isDown(GameAction.MOVE_LEFT, Set.of(KeyCode.A)), "A が効く");
        check(!b.isDown(GameAction.MOVE_LEFT, Set.of(KeyCode.LEFT)), "← はもう効かない");
        checkEquals(1, b.keysFor(GameAction.MOVE_LEFT).size(), "1 つに置き換わる");
    }

    /** 同じキーが 2 つの操作に付くと、押したときどちらが動くか分からなくなる */
    private static void assignStealsFromOtherActions() {
        KeyBindings b = KeyBindings.defaults();
        b.assign(GameAction.HOLD, KeyCode.LEFT);
        check(b.isDown(GameAction.HOLD, Set.of(KeyCode.LEFT)), "後から割り当てたほうが勝つ");
        check(!b.isDown(GameAction.MOVE_LEFT, Set.of(KeyCode.LEFT)), "前の操作からは外れる");
        check(b.isUnbound(GameAction.MOVE_LEFT), "奪われた側は未割り当てになる");
    }

    /** 予約キーを取られるとゲーム中に戻れなくなる・UI 配置が飛ぶ */
    private static void rejectsReservedKeys() {
        KeyBindings b = KeyBindings.defaults();
        for (KeyCode reserved : new KeyCode[]{
                KeyCode.ESCAPE, KeyCode.P, KeyCode.ENTER, KeyCode.F2, KeyCode.F4}) {
            check(!b.assign(GameAction.HOLD, reserved), reserved + " は割り当て不可");
            check(KeyBindings.isReserved(reserved), reserved + " は予約キー");
        }
        check(b.isDown(GameAction.HOLD, Set.of(KeyCode.H)), "失敗しても元の割り当ては残る");
    }

    private static void clearAndReset() {
        KeyBindings b = KeyBindings.defaults();
        b.clear(GameAction.HARD_DROP);
        check(b.isUnbound(GameAction.HARD_DROP), "外せる");
        checkEquals("（未割り当て）", b.describe(GameAction.HARD_DROP), "未割り当ての表記");
        b.resetToDefaults();
        check(!b.isUnbound(GameAction.HARD_DROP), "既定へ戻せる");
        check(b.isDown(GameAction.HARD_DROP, Set.of(KeyCode.SPACE)), "既定は SPACE");
    }

    private static void survivesRoundTrip() {
        KeyBindings saved = KeyBindings.defaults();
        saved.assign(GameAction.MOVE_LEFT, KeyCode.A);

        KeyBindings loaded = KeyBindings.defaults();
        for (GameAction action : GameAction.values()) {
            loaded.decode(action, saved.encode(action));
        }
        check(loaded.isDown(GameAction.MOVE_LEFT, Set.of(KeyCode.A)), "変更が復元される");
        check(loaded.isDown(GameAction.ROTATE_RIGHT, Set.of(KeyCode.UP)), "複数割り当ても復元される");
        checkEquals(saved.describe(GameAction.ROTATE_RIGHT),
                loaded.describe(GameAction.ROTATE_RIGHT), "表記まで一致");
    }

    /** 手で settings.properties を書き換えても起動できなくならないこと */
    private static void ignoresGarbageOnDecode() {
        KeyBindings b = KeyBindings.defaults();
        b.decode(GameAction.HOLD, "NOPE, , ESCAPE ,J");
        check(b.isDown(GameAction.HOLD, Set.of(KeyCode.J)), "読めるものだけ残る");
        check(!b.isDown(GameAction.HOLD, Set.of(KeyCode.ESCAPE)), "予約キーは捨てる");
        checkEquals(1, b.keysFor(GameAction.HOLD).size(), "知らない名前と空要素は捨てる");
    }

    private KeyBindingsTest() {
    }
}
