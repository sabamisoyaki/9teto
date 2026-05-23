package tetris;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ResourcePath {

    static final Path BASE_DIR = resolveBase();

    private static Path resolveBase() {
        try {
            URI uri = ResourcePath.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path dir = Paths.get(uri).getParent();
            if (dir != null && Files.isDirectory(dir)) {
                return dir;
            }
        } catch (Exception ignored) {
        }
        return Paths.get(".");
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
