package tetris.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** 外部テストライブラリ無しで実行できる、シナリオスキーマの回帰テスト。 */
public final class ScenarioValidationTest {

    public static void main(String[] args) throws IOException {
        acceptsBundledScenario();
        acceptsValidScenarioAndSelectsByScore();
        rejectsWrongFieldTypes();
        rejectsDuplicateMinScore();
        rejectsPageWithoutText();
        System.out.println("ScenarioValidationTest: PASS");
    }

    private static void acceptsBundledScenario() throws IOException {
        Scenario scenario = Scenario.parse(
                Files.readString(Path.of("scenario", "adventure.json")));
        check(scenario.routesOf(Scenario.OPENING).size() == 2,
                "同梱オープニングを2ルート読み込む");
        check(scenario.routesOf(Scenario.ENDING).size() == 3,
                "同梱エンディングを3ルート読み込む");
    }

    private static void acceptsValidScenarioAndSelectsByScore() {
        Scenario scenario = Scenario.parse("""
                {
                  "parts": {
                    "ending": {
                      "routes": [
                        {"id":"low", "pages":[{"text":"low"}]},
                        {"id":"high", "minScore":100, "pages":[{"text":"high"}]}
                      ]
                    }
                  }
                }
                """);
        check("low".equals(scenario.routeFor(Scenario.ENDING, 99).id()),
                "99 点では low を選ぶ");
        check("high".equals(scenario.routeFor(Scenario.ENDING, 100).id()),
                "100 点では high を選ぶ");
    }

    private static void rejectsWrongFieldTypes() {
        expectInvalid("""
                {"parts":{"ending":{"routes":[
                  {"id":"bad", "minScore":"100", "pages":[{"text":"x"}]}
                ]}}}
                """, "minScore");
        expectInvalid("""
                {"parts":{"ending":{"routes":{}}}}
                """, "routes");
    }

    private static void rejectsDuplicateMinScore() {
        expectInvalid("""
                {"parts":{"ending":{"routes":[
                  {"id":"a", "minScore":10, "pages":[{"text":"a"}]},
                  {"id":"b", "minScore":10, "pages":[{"text":"b"}]}
                ]}}}
                """, "minScore");
    }

    private static void rejectsPageWithoutText() {
        expectInvalid("""
                {"parts":{"ending":{"routes":[
                  {"id":"bad", "pages":[{"speaker":"nobody"}]}
                ]}}}
                """, "text");
    }

    private static void expectInvalid(String json, String messagePart) {
        try {
            Scenario.parse(json);
            throw new AssertionError("例外にならなかった: " + messagePart);
        } catch (IllegalArgumentException e) {
            check(e.getMessage().contains(messagePart),
                    "エラーに項目名が含まれる: " + e.getMessage());
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private ScenarioValidationTest() {
    }
}
