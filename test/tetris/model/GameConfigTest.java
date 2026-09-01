package tetris.model;

import static tetris.TestSupport.check;
import static tetris.TestSupport.checkEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Stream;

import javafx.scene.input.KeyCode;

/**
 * 保存・復元の回帰テスト。
 *
 * <p><b>実際のセーブデータ（{@code ~/.9pazzle/}）には触らない。</b>
 * 一時ディレクトリを {@link GameConfig#GameConfig(Path)} に渡す。
 * 既定の保存先を使うと、テストのたびにプレイ記録が書き換わってしまう。
 */
public final class GameConfigTest {

    public static void main(String[] args) throws IOException {
        Path dir = Files.createTempDirectory("9pazzle-test");
        try {
            savesAndLoads(dir.resolve("a.properties"));
            startsCleanWithoutFile(dir.resolve("missing.properties"));
            recordsProgress(dir.resolve("b.properties"));
            keepsDefaultsForUnsavedKeys(dir.resolve("c.properties"));
            System.out.println("GameConfigTest: PASS");
        } finally {
            deleteTree(dir);
        }
    }

    private static void savesAndLoads(Path file) {
        GameConfig saved = new GameConfig(file);
        saved.setBgmVolume(0.25);
        saved.setSeEnabled(false);
        saved.updateHighScore(1234);
        saved.getKeyBindings().assign(GameAction.MOVE_LEFT, KeyCode.A);
        saved.save();

        GameConfig loaded = new GameConfig(file);
        loaded.load();
        checkEquals(0.25, loaded.getBgmVolume(), "音量");
        check(!loaded.isSeEnabled(), "SE の有効/無効");
        checkEquals(1234, loaded.getHighScore(), "ハイスコア");
        check(loaded.getKeyBindings().isDown(GameAction.MOVE_LEFT, Set.of(KeyCode.A)),
                "キー割り当て");
    }

    /** 初回起動。ファイルが無くても落ちず、既定値で立ち上がること */
    private static void startsCleanWithoutFile(Path missing) {
        GameConfig config = new GameConfig(missing);
        config.load();
        checkEquals(0, config.getHighScore(), "ハイスコアは 0");
        check(!config.hasSeenTutorial(), "チュートリアル未読");
        checkEquals(0, config.seenEndingCount(), "エンディング未到達");
        check(!config.getKeyBindings().isUnbound(GameAction.HARD_DROP), "キーは既定のまま");
    }

    private static void recordsProgress(Path file) {
        GameConfig config = new GameConfig(file);
        config.markEndingSeen("ed-a");
        config.markEndingSeen("ed-a");   // 重複
        config.markEndingSeen("ed-b");
        config.markEndingSeen("");       // 空は無視
        config.markEndingSeen(null);     // null は無視
        config.markTutorialSeen();
        checkEquals(2, config.seenEndingCount(), "重複と空を弾く");

        GameConfig loaded = new GameConfig(file);
        loaded.load();
        check(loaded.hasSeenEnding("ed-a") && loaded.hasSeenEnding("ed-b"), "両方復元される");
        check(!loaded.hasSeenEnding("ed-c"), "踏んでいないものは false");
        check(loaded.hasSeenTutorial(), "チュートリアル既読が復元される");
    }

    /**
     * 項目を後から増やしても壊れないこと。
     * 古い設定ファイルに無いキーは既定のまま残す。
     */
    private static void keepsDefaultsForUnsavedKeys(Path file) throws IOException {
        Files.writeString(file, "highScore=50\n");
        GameConfig config = new GameConfig(file);
        config.load();
        checkEquals(50, config.getHighScore(), "書いてある項目は読む");
        check(config.getKeyBindings().isDown(GameAction.HOLD, Set.of(KeyCode.H)),
                "書いていないキー割り当ては既定のまま");
        check(config.isBgmEnabled(), "書いていない設定は既定のまま");
    }

    private static void deleteTree(Path dir) throws IOException {
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(p);
            }
        }
    }

    private GameConfigTest() {
    }
}
