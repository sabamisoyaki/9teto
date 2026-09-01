package tetris;

/**
 * テストの共通部品。外部ライブラリを入れないための最小限。
 *
 * <p>JUnit を入れると配布 jar とビルド手順に波及する（{@code package.bat} が
 * javafx-libs を並べている構成へライブラリを持ち込むことになる）。
 * 検証したいのは素の Java で書けるロジックだけなので、これで足りる。
 *
 * <p>各テストクラスは {@code public static void main(String[])} を持ち、
 * 失敗したら {@link AssertionError} を投げる。単体でも走らせられるし、
 * {@link AllTests} からまとめても走らせられる。
 */
public final class TestSupport {

    private TestSupport() {
    }

    public static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void checkEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + "（期待 " + expected + " / 実際 " + actual + "）");
        }
    }

    /**
     * 実行すると {@link IllegalArgumentException} になること、かつ
     * メッセージに手がかりが含まれることを確かめる。
     *
     * @param messagePart エラーに含まれてほしい語（どの項目が悪いか分かるか）
     */
    public static void expectInvalid(Runnable body, String messagePart, String message) {
        try {
            body.run();
        } catch (IllegalArgumentException e) {
            check(e.getMessage() != null && e.getMessage().contains(messagePart),
                    message + " → メッセージに「" + messagePart + "」が無い: " + e.getMessage());
            return;
        }
        throw new AssertionError(message + " → 例外にならなかった");
    }
}
