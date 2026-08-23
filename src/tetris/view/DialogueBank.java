package tetris.view;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * トリガーごとのセリフデータ。文面の差し替えはこのファイルだけで完結させる。
 * 将来 JSON 外部化する場合も DialogueTrigger の境界をそのまま使う。
 */
public final class DialogueBank {

    private static final Map<DialogueTrigger, List<String>> LINES = Map.of(
        DialogueTrigger.GAME_START, List.of(
            "始めましょうか。焦らず積んでいくのよ。",
            "今日はどこまで行けるかしらね。"),
        DialogueTrigger.SINGLE, List.of(
            "1ライン。まあ、悪くないわ。",
            "地道ね。嫌いじゃないけど。"),
        DialogueTrigger.DOUBLE_TRIPLE, List.of(
            "いい感じよ。その調子。",
            "まとめて消すと気持ちいいでしょ？"),
        DialogueTrigger.TETRIS, List.of(
            "テトリス！　やるじゃない！",
            "4ライン同時……見直したわ。"),
        DialogueTrigger.ROTATE_SOON, List.of(
            "次の1ラインで世界が回るわよ。準備はいい？"),
        DialogueTrigger.WORLD_ROTATE, List.of(
            "回ったわね。落ち着いて、盤面をよく見て。",
            "世界が回転したわ。慌てないで。"),
        DialogueTrigger.PINCH, List.of(
            "詰まったわね……まだ大丈夫。立て直すわよ。"),
        DialogueTrigger.PINCH_LAST, List.of(
            "次はもう後がないわ。慎重に……！"),
        DialogueTrigger.LEVEL_UP, List.of(
            "速くなるわよ。ついてきなさい。",
            "レベルアップ。ここからが本番ね。"),
        DialogueTrigger.IDLE, List.of(
            "……手が止まってるわよ？",
            "次のミノはこれよ。下にも表示してるけど。")
    );

    private final Random rand = new Random();
    private String last = "";

    /** トリガーに対応する台詞をランダムに1つ返す。直前と同じ台詞は選ばない */
    public String pick(DialogueTrigger trigger) {
        List<String> candidates = LINES.get(trigger);
        if (candidates == null || candidates.isEmpty()) return "";
        if (candidates.size() == 1) return candidates.get(0);
        String s;
        do {
            s = candidates.get(rand.nextInt(candidates.size()));
        } while (s.equals(last));
        last = s;
        return s;
    }
}
