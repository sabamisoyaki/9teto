package tetris.model;

import java.util.ArrayList;
import java.util.List;

import tetris.Json;

/**
 * アドベンチャーパートの 1 ルート。
 *
 * @param id        保存データの鍵（どのエンディングを踏んだか）。<b>公開後は変えない</b>。
 *                  変えると既読が外れて、見たはずのエンディングが未到達に戻る
 * @param minScore  このルートが選ばれる下限。{@link Scenario#routeFor} を参照
 * @param title     回想モードの一覧に出す名前。未到達では伏せるのでネタバレを含めない
 * @param background ルート全体の既定背景。ページ側で上書きできる
 * @param pages     ページ列
 */
public record ScenarioRoute(
        String id, int minScore, String title, String background, List<ScenarioPage> pages) {

    public ScenarioRoute {
        pages = List.copyOf(pages);
    }

    static ScenarioRoute from(Object json) {
        List<ScenarioPage> pages = new ArrayList<>();
        for (Object p : Json.list(json, "pages")) {
            pages.add(ScenarioPage.from(p));
        }
        return new ScenarioRoute(
                Json.str(json, "id", ""),
                Json.num(json, "minScore", 0),
                Json.str(json, "title", ""),
                Json.str(json, "background", null),
                pages);
    }

    /**
     * 立ち絵と背景の「引き継ぎ」を解いて、各ページが自分だけで完結した状態にする。
     *
     * <p>省略されたページは直前の値を引き継ぐ。これを描画側でやると
     * 「今なにが出ているか」の状態を画面が持つことになり、
     * 回想で途中から開いたときにずれる。読み込み側で潰しておく。
     */
    public List<ScenarioPage> resolvedPages() {
        List<ScenarioPage> out = new ArrayList<>(pages.size());
        String character = "";
        String bg = background;
        for (ScenarioPage p : pages) {
            if (p.character() != null) character = p.character();
            if (p.background() != null) bg = p.background();
            out.add(new ScenarioPage(p.speaker(), character, p.text(), bg));
        }
        return out;
    }

    public boolean isEmpty() {
        return pages.isEmpty();
    }

    /** 一覧に出す名前。title が無ければ id で代用する（名無しで並ぶのを避ける） */
    public String displayTitle() {
        return (title == null || title.isBlank()) ? id : title;
    }
}
