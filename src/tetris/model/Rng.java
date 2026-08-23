package tetris.model;

import java.util.List;

/**
 * 乱数の最小インタフェース。実装は {@link SeededRng} 1 つだけだが、
 * ここを挟んでおくとテストで固定列を返すダミーに差し替えられる。
 *
 * <p>{@link #nextInt(int)} と {@link #nextDouble()} 以外は default 実装で、
 * この 2 つを呼ぶ形に統一してある（消費回数の計測箇所を 2 か所に閉じるため）。
 */
public interface Rng {

    int nextInt(int bound);

    double nextDouble();

    /** lo 以上 hi 未満の実数。角度・速度・寿命など演出パラメータ用 */
    default double range(double lo, double hi) {
        return lo + nextDouble() * (hi - lo);
    }

    /** リストから 1 つ選ぶ。空リストは呼び出し側で弾くこと */
    default <T> T pick(List<T> list) {
        return list.get(nextInt(list.size()));
    }

    /** リストをその場でシャッフルする（Fisher-Yates） */
    default <T> void shuffle(List<T> list) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = nextInt(i + 1);
            T tmp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, tmp);
        }
    }
}
