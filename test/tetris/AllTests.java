package tetris;

import java.util.LinkedHashMap;
import java.util.Map;

import tetris.model.GameConfigTest;
import tetris.model.KeyBindingsTest;
import tetris.model.ScenarioTest;
import tetris.model.ScenarioValidationTest;

/**
 * 全テストをまとめて走らせる。{@code test.bat} から呼ばれる。
 *
 * <p>失敗しても止めずに最後まで走らせ、まとめて件数を出す。1 つ直すたびに
 * 全部走らせ直すより、壊れている範囲が一度に見えるほうが早い。
 *
 * <p>ここに登録し忘れると走らないので、テストクラスを足したら 1 行足すこと。
 */
public final class AllTests {

    private static final String[] NO_ARGS = new String[0];

    /** 名前 → 実行本体。順番は基盤（Json）から積み上げる順 */
    private static final Map<String, Runnable> TESTS = new LinkedHashMap<>();

    static {
        TESTS.put("JsonTest",              () -> JsonTest.main(NO_ARGS));
        TESTS.put("ScenarioTest",          () -> ScenarioTest.main(NO_ARGS));
        TESTS.put("ScenarioValidationTest", AllTests::runScenarioValidation);
        TESTS.put("KeyBindingsTest",       () -> KeyBindingsTest.main(NO_ARGS));
        TESTS.put("GameConfigTest",        AllTests::runGameConfig);
    }

    public static void main(String[] args) {
        int failed = 0;
        for (Map.Entry<String, Runnable> entry : TESTS.entrySet()) {
            try {
                entry.getValue().run();
            } catch (AssertionError | RuntimeException e) {
                failed++;
                System.out.println(entry.getKey() + ": FAIL  " + e);
            }
        }

        System.out.println();
        System.out.printf("%d / %d passed%n", TESTS.size() - failed, TESTS.size());
        if (failed > 0) {
            System.exit(1);
        }
    }

    // 検査例外を投げるテストは Runnable に直接入らないので、ここで包む
    private static void runScenarioValidation() {
        try {
            ScenarioValidationTest.main(NO_ARGS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void runGameConfig() {
        try {
            GameConfigTest.main(NO_ARGS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private AllTests() {
    }
}
