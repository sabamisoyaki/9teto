package tetris.model;

import java.util.Random;

/**
 * {@link Random} を包む唯一の {@link Rng} 実装。
 *
 * <p>消費回数を数えているのはエラーダンプのため。「シード + 消費回数」があれば
 * 数列を完全に再現できるので、乱数の値そのものを貯め込む必要が無い。
 */
public final class SeededRng implements Rng {

    private final long seed;
    private final Random random;
    private long draws = 0;

    public SeededRng(long seed) {
        this.seed = seed;
        this.random = new Random(seed);
    }

    @Override
    public int nextInt(int bound) {
        draws++;
        return random.nextInt(bound);
    }

    @Override
    public double nextDouble() {
        draws++;
        return random.nextDouble();
    }

    public long seed() { return seed; }

    /** ここまでの消費回数。この位置まで空引きすれば同じ状態に戻せる */
    public long draws() { return draws; }
}
