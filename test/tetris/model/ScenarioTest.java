package tetris.model;

import static tetris.TestSupport.check;
import static tetris.TestSupport.checkEquals;

import java.util.List;

/**
 * ルート選択と引き継ぎ解決の回帰テスト。
 * スキーマ検証は {@link ScenarioValidationTest} が受け持つ。
 */
public final class ScenarioTest {

    private static final String JSON = """
            {
              "parts": {
                "tutorial": {
                  "routes": [
                    {"id":"t1", "pages":[{"text":"one"}]},
                    {"id":"t2", "pages":[{"text":"two"}]}
                  ]
                },
                "ending": {
                  "routes": [
                    {"id":"low",  "minScore":0,     "title":"低", "pages":[
                      {"text":"地の文"},
                      {"speaker":"医", "character":"9F-CLINIC", "text":"あ"},
                      {"text":"い"},
                      {"character":"", "text":"う"}
                    ]},
                    {"id":"mid",  "minScore":30000,  "pages":[{"text":"m"}]},
                    {"id":"high", "minScore":100000, "pages":[{"text":"h"}]}
                  ]
                }
              }
            }
            """;

    public static void main(String[] args) {
        selectsByMinScore();
        toleratesUnknownPart();
        resolvesCarryOver();
        tutorialIsOrdered();
        System.out.println("ScenarioTest: PASS");
    }

    /** 境界そのものを見る。しきい値は JSON で動かすので、ここが崩れると全部ずれる */
    private static void selectsByMinScore() {
        Scenario s = Scenario.parse(JSON);
        checkEquals("low",  idFor(s, 0),       "0 点");
        checkEquals("low",  idFor(s, 29999),   "境界の 1 つ下");
        checkEquals("mid",  idFor(s, 30000),   "境界ちょうど");
        checkEquals("mid",  idFor(s, 99999),   "次の境界の 1 つ下");
        checkEquals("high", idFor(s, 100000),  "次の境界ちょうど");
        checkEquals("high", idFor(s, 9999999), "上限なし");
    }

    /** パート名を打ち間違えても落ちないこと。落ちるとゲームが進めなくなる */
    private static void toleratesUnknownPart() {
        Scenario s = Scenario.parse(JSON);
        check(s.routeFor("endding", 0) == null, "未知のパートは null");
        check(s.routesOf("endding").isEmpty(), "未知のパートは空リスト");
        check(s.routeById(Scenario.ENDING, "nope") == null, "無い id は null");
        check(s.routeById(Scenario.ENDING, "mid") != null, "ある id は引ける");
    }

    /**
     * 立ち絵の引き継ぎ。省略＝継続、空文字＝消す、を区別できていること。
     * ここを描画側に持たせると、回想で途中から開いたときにずれる。
     */
    private static void resolvesCarryOver() {
        ScenarioRoute route = Scenario.parse(JSON).routeById(Scenario.ENDING, "low");
        List<ScenarioPage> pages = route.resolvedPages();
        checkEquals(4, pages.size(), "ページ数");
        check(pages.get(0).character().isEmpty(), "1ページ目は立ち絵なし");
        checkEquals("9F-CLINIC", pages.get(1).character(), "2ページ目で立ち絵が付く");
        checkEquals("9F-CLINIC", pages.get(2).character(), "3ページ目は省略＝継続");
        check(pages.get(3).character().isEmpty(), "4ページ目は空文字＝消す");
        check(!pages.get(0).hasSpeaker(), "話者なしは地の文");
        check(pages.get(1).hasSpeaker(), "話者ありを判別");
        check(route.pages().get(2).character() == null, "解決しても元は書き換えない");
    }

    /** チュートリアルはスコアで選ばず JSON の順に流す */
    private static void tutorialIsOrdered() {
        List<ScenarioRoute> routes = Scenario.parse(JSON).routesOf(Scenario.TUTORIAL);
        checkEquals(2, routes.size(), "本数");
        checkEquals("t1", routes.get(0).id(), "書いた順のまま");
        checkEquals("t2", routes.get(1).id(), "書いた順のまま");
    }

    private static String idFor(Scenario s, int score) {
        ScenarioRoute r = s.routeFor(Scenario.ENDING, score);
        return r == null ? "(null)" : r.id();
    }

    private ScenarioTest() {
    }
}
