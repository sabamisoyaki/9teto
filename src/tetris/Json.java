package tetris;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 依存を増やさずに済ませるための最小 JSON リーダ。
 *
 * <p>読めるのはオブジェクト / 配列 / 文字列 / 数値 / true / false / null だけ。
 * 返す型は {@code Map<String,Object>} / {@code List<Object>} / String / Double /
 * Boolean / null。取り出しは {@link #str} {@link #num} {@link #list} {@link #map}
 * を通すと、欠けは既定値で吸収しつつ、型違いは設定ミスとして検出できる。
 *
 * <p>{@code EndCreditPane} も JSON を読んでいるがあちらは正規表現で、
 * 「heading と lines の繰り返し」という平たい構造専用。入れ子があるデータは
 * こちらを使うこと（正規表現だと内側の括弧を越えて食う）。
 *
 * <p>オブジェクトは {@link LinkedHashMap} で作るので、書いた順が保たれる。
 */
public final class Json {

    private static final Object MISSING = new Object();

    private final String src;
    private int pos;

    private Json(String src) {
        this.src = src;
    }

    /**
     * @throws IllegalArgumentException 壊れている場合。黙って null を返さない
     *         （シナリオの打ち間違いを起動時に気付けるようにするため）
     */
    public static Object parse(String text) {
        if (text == null) {
            throw new IllegalArgumentException("JSON が null");
        }
        Json p = new Json(text);
        p.skipWs();
        Object v = p.value();
        p.skipWs();
        if (p.pos < p.src.length()) {
            throw p.error("末尾に余分な文字がある");
        }
        return v;
    }

    // ==================================================
    //   取り出しヘルパ。欠けは既定値、型違いは例外
    // ==================================================

    public static String str(Object o, String key, String def) {
        Object v = optional(o, key);
        if (v == MISSING || v == null) return def;
        if (v instanceof String s) return s;
        throw typeError(key, "文字列", v);
    }

    /** 必須の文字列を取る。欠落・null・型違いはいずれも設定ミス。 */
    public static String requiredStr(Object o, String key) {
        Object v = required(o, key);
        if (v instanceof String s) return s;
        throw typeError(key, "文字列", v);
    }

    /** 無いなら既定値。型違い・小数・範囲外なら設定ミスとして例外にする。 */
    public static int num(Object o, String key, int def) {
        Object v = optional(o, key);
        if (v == MISSING) return def;
        if (!(v instanceof Number n)) throw typeError(key, "数値", v);

        double value = n.doubleValue();
        if (!Double.isFinite(value)
                || value != Math.rint(value)
                || value < Integer.MIN_VALUE
                || value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    key + " は32ビット整数でなければならない: " + n);
        }
        return (int) value;
    }

    /** 配列を取る。無いなら空リスト、型違いなら設定ミス。 */
    public static List<Object> list(Object o, String key) {
        Object v = optional(o, key);
        if (v == MISSING) return List.of();
        if (!(v instanceof List<?> l)) throw typeError(key, "配列", v);
        return new ArrayList<>(l);
    }

    /** 必須の配列を取る。欠落・null・型違いはいずれも設定ミス。 */
    public static List<Object> requiredList(Object o, String key) {
        Object v = required(o, key);
        if (!(v instanceof List<?> l)) throw typeError(key, "配列", v);
        return new ArrayList<>(l);
    }

    /** オブジェクトを取る。無いなら空マップ、型違いなら設定ミス。 */
    public static Map<String, Object> map(Object o, String key) {
        Object v = optional(o, key);
        if (v == MISSING) return Map.of();
        if (!(v instanceof Map<?, ?> m)) throw typeError(key, "オブジェクト", v);
        return copyMap(m);
    }

    /** 必須のオブジェクトを取る。欠落・null・型違いはいずれも設定ミス。 */
    public static Map<String, Object> requiredMap(Object o, String key) {
        Object v = required(o, key);
        if (!(v instanceof Map<?, ?> m)) throw typeError(key, "オブジェクト", v);
        return copyMap(m);
    }

    private static Map<String, Object> copyMap(Map<?, ?> m) {
        Map<String, Object> out = new LinkedHashMap<>();
        m.forEach((k, val) -> out.put(String.valueOf(k), val));
        return out;
    }

    private static Object optional(Object o, String key) {
        if (!(o instanceof Map<?, ?> m)) {
            throw new IllegalArgumentException(
                    key + " を含む値はオブジェクトでなければならない");
        }
        return m.containsKey(key) ? m.get(key) : MISSING;
    }

    private static Object required(Object o, String key) {
        Object v = optional(o, key);
        if (v == MISSING) {
            throw new IllegalArgumentException("必須項目が無い: " + key);
        }
        return v;
    }

    private static IllegalArgumentException typeError(
            String key, String expected, Object actual) {
        String actualType = actual == null
                ? "null"
                : actual.getClass().getSimpleName();
        return new IllegalArgumentException(
                key + " は" + expected + "でなければならない: " + actualType);
    }

    // ==================================================
    //   パーサ本体（再帰下降）
    // ==================================================

    private Object value() {
        if (pos >= src.length()) throw error("値が来る前に終わった");
        char c = src.charAt(pos);
        return switch (c) {
            case '{' -> object();
            case '[' -> array();
            case '"' -> string();
            case 't' -> literal("true", Boolean.TRUE);
            case 'f' -> literal("false", Boolean.FALSE);
            case 'n' -> literal("null", null);
            default  -> number();
        };
    }

    private Map<String, Object> object() {
        Map<String, Object> map = new LinkedHashMap<>();
        expect('{');
        skipWs();
        if (peek() == '}') { pos++; return map; }
        while (true) {
            skipWs();
            if (peek() != '"') throw error("キーは文字列でなければならない");
            String key = string();
            skipWs();
            expect(':');
            skipWs();
            map.put(key, value());
            skipWs();
            char c = next("',' か '}' が要る");
            if (c == '}') return map;
            if (c != ',') throw error("',' か '}' が要る");
        }
    }

    private List<Object> array() {
        List<Object> list = new ArrayList<>();
        expect('[');
        skipWs();
        if (peek() == ']') { pos++; return list; }
        while (true) {
            skipWs();
            list.add(value());
            skipWs();
            char c = next("',' か ']' が要る");
            if (c == ']') return list;
            if (c != ',') throw error("',' か ']' が要る");
        }
    }

    private String string() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = next("文字列が閉じていない");
            if (c == '"') return sb.toString();
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            char e = next("エスケープが途切れている");
            switch (e) {
                case '"'  -> sb.append('"');
                case '\\' -> sb.append('\\');
                case '/'  -> sb.append('/');
                case 'b'  -> sb.append('\b');
                case 'f'  -> sb.append('\f');
                case 'n'  -> sb.append('\n');
                case 'r'  -> sb.append('\r');
                case 't'  -> sb.append('\t');
                case 'u'  -> {
                    if (pos + 4 > src.length()) throw error("\\u の桁が足りない");
                    String hex = src.substring(pos, pos + 4);
                    try {
                        sb.append((char) Integer.parseInt(hex, 16));
                    } catch (NumberFormatException ex) {
                        throw error("\\u の後が 16 進数でない: " + hex);
                    }
                    pos += 4;
                }
                default -> throw error("知らないエスケープ: \\" + e);
            }
        }
    }

    private Double number() {
        int start = pos;
        if (peek() == '-') pos++;

        // JSON の整数部は 0 単独、または 0 以外から始まる数字列。
        // Double.valueOf に丸投げすると .5 / +1 / 01 / 1. まで通ってしまう。
        if (peek() == '0') {
            pos++;
            if (isDigit(peek())) throw error("数値の先頭に余分な 0 がある");
        } else if (isOneToNine(peek())) {
            do { pos++; } while (isDigit(peek()));
        } else {
            throw error("数値として読めない");
        }

        if (peek() == '.') {
            pos++;
            if (!isDigit(peek())) throw error("小数点の後に数字が要る");
            do { pos++; } while (isDigit(peek()));
        }

        if (peek() == 'e' || peek() == 'E') {
            pos++;
            if (peek() == '+' || peek() == '-') pos++;
            if (!isDigit(peek())) throw error("指数部に数字が要る");
            do { pos++; } while (isDigit(peek()));
        }

        String text = src.substring(start, pos);
        try {
            Double value = Double.valueOf(text);
            if (!Double.isFinite(value)) {
                throw error("数値が大きすぎる: " + text);
            }
            return value;
        } catch (NumberFormatException e) {
            throw error("数値として読めない: " + text);
        }
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isOneToNine(char c) {
        return c >= '1' && c <= '9';
    }

    private Object literal(String word, Object result) {
        if (!src.startsWith(word, pos)) throw error("知らない値");
        pos += word.length();
        return result;
    }

    // ==================================================
    //   下回り
    // ==================================================

    private void skipWs() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
    }

    private char peek() {
        return pos < src.length() ? src.charAt(pos) : '\0';
    }

    private char next(String whatWasExpected) {
        if (pos >= src.length()) throw error(whatWasExpected);
        return src.charAt(pos++);
    }

    private void expect(char c) {
        if (peek() != c) throw error("'" + c + "' が要る");
        pos++;
    }

    /** 何行目で転んだかを添える。シナリオを手書きするので位置が分からないと直せない */
    private IllegalArgumentException error(String message) {
        int line = 1;
        int col = 1;
        for (int i = 0; i < Math.min(pos, src.length()); i++) {
            if (src.charAt(i) == '\n') { line++; col = 1; } else { col++; }
        }
        return new IllegalArgumentException(
                message + "（" + line + " 行 " + col + " 文字目）");
    }
}
