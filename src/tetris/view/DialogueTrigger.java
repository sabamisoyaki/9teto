package tetris.view;

/**
 * キャラセリフのきっかけ。ゲーム側イベント（SeEvent）とは 1:1 にせず、
 * 台詞の文脈として独立に定義する。
 */
public enum DialogueTrigger {
    GAME_START,        // ゲーム開始時
    SINGLE,            // 1ライン消去
    DOUBLE_TRIPLE,     // 2-3ライン消去
    TETRIS,            // 4ライン消去
    ROTATE_SOON,       // 回転まで残り1ライン
    WORLD_ROTATE,      // 回転発生
    PINCH,             // 仮ゲームオーバー
    PINCH_LAST,        // ストリーク 3/4（次で終わり）
    LEVEL_UP,          // レベルアップ
    IDLE               // 一定時間ライン消去なし
}
