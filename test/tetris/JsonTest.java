package tetris;

import static tetris.TestSupport.check;
import static tetris.TestSupport.checkEquals;
import static tetris.TestSupport.expectInvalid;

import java.util.List;
import java.util.Map;

/** {@link Json} の回帰テスト。 */
public final class JsonTest {

    public static void main(String[] args) {
        readsNestedStructure();
        readsScalars();
        readsEscapesAndJapanese();
        absorbsMissingWithDefaults();
        rejectsTypeMismatch();
        rejectsBrokenJson();
        keepsInsertionOrder();
        System.out.println("JsonTest: PASS");
    }

    /**
     * 正規表現方式が壊れるケース。片方のルートの pages が隣まで食わないこと。
     * これを確認したくて自前パーサに切り替えたので、真っ先に見る。
     */
    private static void readsNestedStructure() {
        Object root = Json.parse("""
                { "parts": { "ending": { "routes": [
                    { "id": "a", "pages": [ {"text":"one"}, {"text":"two"} ] },
                    { "id": "b", "pages": [ {"text":"three"} ] } ] } } }
                """);
        Object ending = Json.map(root, "parts").get("ending");
        List<Object> routes = Json.list(ending, "routes");
        checkEquals(2, routes.size(), "routes が 2 本");
        checkEquals(2, Json.list(routes.get(0), "pages").size(), "1本目の pages");
        checkEquals(1, Json.list(routes.get(1), "pages").size(), "2本目の pages が隣を食わない");
        checkEquals("b", Json.str(routes.get(1), "id", ""), "id が読める");
    }

    private static void readsScalars() {
        Object o = Json.parse("{\"n\":30000, \"neg\":-5, \"exp\":1e3, \"t\":true, \"z\":null}");
        checkEquals(30000, Json.num(o, "n", -1), "整数");
        checkEquals(-5, Json.num(o, "neg", 0), "負数");
        checkEquals(1000, Json.num(o, "exp", 0), "指数表記");
        check(Boolean.TRUE.equals(((Map<?, ?>) o).get("t")), "true");
        check(((Map<?, ?>) o).containsKey("z") && ((Map<?, ?>) o).get("z") == null, "null");
    }

    private static void readsEscapesAndJapanese() {
        Object o = Json.parse("{\"a\":\"改行\\nタブ\\t引用\\\"円\\\\\", \"u\":\"\\u9F8D\"}");
        String a = Json.str(o, "a", "");
        check(a.contains("\n") && a.contains("\t"), "改行とタブ");
        check(a.contains("\"") && a.contains("\\"), "引用符とバックスラッシュ");
        check(a.startsWith("改行"), "日本語がそのまま読める");
        checkEquals("龍", Json.str(o, "u", ""), "\\uXXXX");
    }

    private static void absorbsMissingWithDefaults() {
        Object o = Json.parse("{}");
        checkEquals("def", Json.str(o, "nope", "def"), "欠けたキーは既定値");
        checkEquals(42, Json.num(o, "nope", 42), "欠けた数値は既定値");
        check(Json.list(o, "nope").isEmpty(), "欠けた配列は空");
        check(Json.map(o, "nope").isEmpty(), "欠けたオブジェクトは空");
    }

    /** 打ち間違いを既定値で握り潰さないこと。黙って動くと原因が追えない */
    private static void rejectsTypeMismatch() {
        Object o = Json.parse("{\"n\":\"100\", \"s\":5, \"frac\":1.5}");
        expectInvalid(() -> Json.num(o, "n", 0), "n", "数値のはずが文字列");
        expectInvalid(() -> Json.str(o, "s", ""), "s", "文字列のはずが数値");
        expectInvalid(() -> Json.num(o, "frac", 0), "frac", "整数のはずが小数");
        expectInvalid(() -> Json.requiredStr(o, "missing"), "missing", "必須項目が無い");
    }

    private static void rejectsBrokenJson() {
        expectInvalid(() -> Json.parse("{\"a\":1"), "行", "閉じ括弧が無い");
        expectInvalid(() -> Json.parse("{\"a\":1,}"), "行", "カンマ余り");
        expectInvalid(() -> Json.parse("{a:1}"), "行", "キーが文字列でない");
        expectInvalid(() -> Json.parse("{\"a\":\"xx}"), "行", "文字列が閉じない");
        expectInvalid(() -> Json.parse("{} garbage"), "行", "末尾にゴミ");
    }

    /** 書いた順が保たれること。読み込みの検証やダンプで並びが崩れると読みにくい */
    private static void keepsInsertionOrder() {
        Object o = Json.parse("{\"z\":1, \"a\":2, \"m\":3}");
        checkEquals("[z, a, m]", ((Map<?, ?>) o).keySet().toString(), "並び順が保たれる");
    }

    private JsonTest() {
    }
}
