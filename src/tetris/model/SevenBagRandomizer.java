package tetris.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

/**
 * 7 種を 1 巡ずつ引く標準方式。同じミノが極端に続けて来ないことを保証する。
 * 旧 {@code GameController.generateBag()} をそのまま移設したもの。
 */
public final class SevenBagRandomizer implements PieceRandomizer {

    private final Rng rng;
    private final Deque<ShapeType> bag = new ArrayDeque<>();

    public SevenBagRandomizer(Rng rng) {
        this.rng = rng;
    }

    @Override
    public ShapeType next() {
        if (bag.isEmpty()) refill();
        return bag.poll();
    }

    private void refill() {
        List<ShapeType> list = new ArrayList<>(Arrays.asList(ShapeType.values()));
        rng.shuffle(list);
        bag.addAll(list);
    }
}
