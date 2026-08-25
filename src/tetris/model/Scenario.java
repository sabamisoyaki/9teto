package tetris.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tetris.Json;
import tetris.ResourcePath;

/**
 * アドベンチャーパートのシナリオ。{@code scenario/adventure.json} を読む。
 *
 * <p>パート名（"opening" / "ending"）でルート束を引き、スコアで 1 本を選ぶ。
 * 画面もルート選択も 2 つのパートで共通なので、ここに違いは無い。
 *
 * <p>ファイルが無い・壊れている場合は<b>空のシナリオ</b>を返す。
 * アセットが無ければ静かにフォールバックする、という既存方針（ASSETS.md）に合わせ、
 * シナリオが用意できていなくてもゲームは動く。
 */
public final class Scenario {

    public static final String OPENING = "opening";
    public static final String ENDING  = "ending";

    private static final Path FILE = ResourcePath.of("scenario", "adventure.json");

    /** 起動中に読み直さない。差し替えたらアプリを起動し直す（UiSkinBank と同じ流儀） */
    private static Scenario cached;

    private final Map<String, List<ScenarioRoute>> parts;

    private Scenario(Map<String, List<ScenarioRoute>> parts) {
        this.parts = parts;
    }

    public static Scenario load() {
        if (cached == null) {
            cached = read();
        }
        return cached;
    }

    private static Scenario read() {
        if (!Files.exists(FILE)) {
            System.out.println("[Scenario] Not found: " + FILE.toAbsolutePath());
            return new Scenario(Map.of());
        }
        try {
            Object root = Json.parse(Files.readString(FILE));
            Map<String, List<ScenarioRoute>> parts = new LinkedHashMap<>();
            Json.map(root, "parts").forEach((name, part) -> {
                List<ScenarioRoute> routes = new ArrayList<>();
                Set<String> ids = new HashSet<>();
                for (Object r : Json.list(part, "routes")) {
                    ScenarioRoute route = ScenarioRoute.from(r);
                    if (!ids.add(route.id())) {
                        throw new IllegalArgumentException(
                                "パート " + name + " のルート id が重複している: " + route.id());
                    }
                    routes.add(route);
                }
                parts.put(name, List.copyOf(routes));
            });
            System.out.println("[Scenario] Loaded: " + parts.keySet());
            return new Scenario(parts);
        } catch (IOException | IllegalArgumentException e) {
            // 打ち間違いは黙らせない。どこで転んだかは Json が行番号を添えている
            System.err.println("[Scenario] Failed to load: " + e.getMessage());
            return new Scenario(Map.of());
        }
    }

    /** パートのルート束。未知のパート名なら空リスト */
    public List<ScenarioRoute> routesOf(String part) {
        return parts.getOrDefault(part, List.of());
    }

    /**
     * {@code minScore <= score} を満たすルートのうち、{@code minScore} が最大のものを返す。
     * 並び順に依存しないので、後からルートを足すときに順番を気にしなくていい。
     *
     * @return 該当が無ければ null（呼び出し側は素通りする）
     */
    public ScenarioRoute routeFor(String part, int score) {
        ScenarioRoute best = null;
        for (ScenarioRoute r : routesOf(part)) {
            if (r.minScore() > score) continue;
            if (best == null || r.minScore() > best.minScore()) best = r;
        }
        return best;
    }

    /** id でルートを引く。回想モードが記録済みのものを直接開くのに使う */
    public ScenarioRoute routeById(String part, String id) {
        for (ScenarioRoute r : routesOf(part)) {
            if (r.id().equals(id)) return r;
        }
        return null;
    }
}
