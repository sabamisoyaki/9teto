package tetris.model;

/**
 * 次に出すミノの種類を決める。7-bag 以外の方式（完全ランダム・履歴付き・
 * ハンデ用の偏り）に差し替えるための境界。
 */
public interface PieceRandomizer {
    ShapeType next();
}
