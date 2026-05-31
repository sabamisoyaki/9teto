package tetris;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ResourcePath {

    static final Path BASE_DIR = resolveBase();

    private static Path resolveBase() {
        try {
            var cs = ResourcePath.class.getProtectionDomain().getCodeSource();
            if (cs == null) return Paths.get(System.getProperty("user.dir", "."));
            var location = cs.getLocation();
            if (location == null) return Paths.get(System.getProperty("user.dir", "."));

            Path loc = Paths.get(location.toURI());
            // jarから実行中（パッケージ版）: jarの隣にimages/audio/が置かれる
            if (!Files.isDirectory(loc)) {
                Path dir = loc.getParent();
                if (dir != null && Files.isDirectory(dir)) {
                    return dir;
                }
            }
        } catch (URISyntaxException | SecurityException | IllegalArgumentException ignored) {
        }
        // クラスファイル直接実行中（IDEデバッグ）: cwd=プロジェクトルートを使う
        return Paths.get(System.getProperty("user.dir", "."));
    }

    public static Path of(String first, String... more) {
        Path p = BASE_DIR.resolve(first);
        for (String part : more) {
            p = p.resolve(part);
        }
        return p;
    }

    private ResourcePath() {
    }
}
