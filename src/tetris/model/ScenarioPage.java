package tetris.model;

import tetris.Json;

/**
 * アドベンチャーパートの 1 ページ。
 *
 * @param speaker    話者名。空文字なら地の文として名前を出さない
 * @param character  立ち絵の名前（{@code images/character/} のファイル名から拡張子を取ったもの）。
 *                   <b>null は「直前のページを引き継ぐ」、空文字は「消す」</b>という別の意味を持つ
 * @param text       本文
 * @param background 背景の名前（{@code images/skin/} のファイル名）。null なら引き継ぐ
 */
public record ScenarioPage(String speaker, String character, String text, String background) {

    static ScenarioPage from(Object json) {
        return new ScenarioPage(
                Json.str(json, "speaker", ""),
                Json.str(json, "character", null),   // 省略と "" を区別する
                Json.str(json, "text", ""),
                Json.str(json, "background", null));
    }

    public boolean hasSpeaker() {
        return speaker != null && !speaker.isBlank();
    }
}
