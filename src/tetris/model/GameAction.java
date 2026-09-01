package tetris.model;

/**
 * ゲーム中の操作。キー割り当て（{@link KeyBindings}）と操作ヒントの単位。
 *
 * <p>並び順がそのままキーコンフィグ画面の行順になるので、
 * 手が動く順（左右 → 落下 → 回転 → ホールド）に並べてある。
 *
 * <p><b>{@code name()} は保存キーになる</b>ので、公開後に定数名を変えないこと。
 * 変えると割り当てが既定値へ戻る。
 */
public enum GameAction {

    MOVE_LEFT("左へ移動"),
    MOVE_RIGHT("右へ移動"),
    SOFT_DROP("ソフトドロップ"),
    HARD_DROP("ハードドロップ"),
    ROTATE_LEFT("左回転"),
    ROTATE_RIGHT("右回転"),
    HOLD("ホールド");

    /** キーコンフィグ画面に出す名前 */
    public final String label;

    GameAction(String label) {
        this.label = label;
    }
}
