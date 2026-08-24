package tetris.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 1 本のマスターシードから用途別の乱数ストリームを配る。
 *
 * <p>用途を分けるのは、演出の乱数呼び出し回数が変わってもミノ順が動かないように
 * するため。全用途で 1 つの Rng を共有すると、パーティクルを 1 個増やしただけで
 * 再現性が壊れる。
 */
public final class RngHub {

    /** 起動時に {@code -Dgame.seed=<数値>} で固定できる。未指定なら毎回変わる */
    public static final String SEED_PROPERTY = "game.seed";

    /** 派生に使う撹拌定数（黄金比由来）。素朴な加算だと近いシード同士で数列が似る */
    private static final long MIX = 0x9E3779B97F4A7C15L;

    private final long masterSeed;

    /**
     * 用途名 → ストリーム。必ずキャッシュする。毎回 new すると同じ名前で
     * 独立した同一数列が 2 本できてしまい、「1 用途 = 1 数列」の前提が崩れる
     * （消費回数も数えられなくなる）。挿入順を保つため LinkedHashMap。
     */
    private final Map<String, SeededRng> streams = new LinkedHashMap<>();

    private RngHub(long masterSeed) {
        this.masterSeed = masterSeed;
    }

    public static RngHub of(long seed) {
        return new RngHub(seed);
    }

    /** -Dgame.seed があればそれを、無ければ現在時刻を種にする */
    public static RngHub fromSystemProperty() {
        String prop = System.getProperty(SEED_PROPERTY);
        if (prop != null) {
            try {
                return new RngHub(Long.parseLong(prop.trim()));
            } catch (NumberFormatException e) {
                System.out.println("[Rng] " + SEED_PROPERTY + " が数値でない: " + prop);
            }
        }
        return new RngHub(System.nanoTime());
    }

    public long seed() { return masterSeed; }

    /**
     * 用途名でストリームを取る。同じ名前・同じマスターシードなら常に同じ数列。
     * 名前は "piece" / "fx" / "dialogue" のような固定文字列を使うこと。
     */
    public Rng stream(String name) {
        return streams.computeIfAbsent(name,
                n -> new SeededRng(masterSeed * MIX + n.hashCode()));
    }

    /** エラーダンプ用。"piece=487 fx=12043 dialogue=6" の形 */
    public String drawCounts() {
        StringBuilder sb = new StringBuilder();
        streams.forEach((name, rng) -> {
            if (sb.length() > 0) sb.append(' ');
            sb.append(name).append('=').append(rng.draws());
        });
        return sb.toString();
    }
}
