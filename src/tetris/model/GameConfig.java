package tetris.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

public class GameConfig {

    private double bgmVolume  = 1.0;
    private double seVolume   = 0.70;
    private boolean bgmEnabled = true;
    private boolean seEnabled  = true;
    private int highScore = 0;

    /**
     * 踏んだエンディングの id。回想モードの解錠に使う。
     *
     * <p>並び順の添字ではなく id を保存する。JSON のルートを並べ替えても既読がずれない。
     * 裏返すと、公開後に id を変えると既読が外れて未到達へ戻る。
     * 出現順を保つため LinkedHashSet。
     */
    private final Set<String> seenEndings = new LinkedHashSet<>();

    private static final Path SAVE_FILE = Path.of(
        System.getProperty("user.home"), ".9pazzle", "settings.properties");

    public double getBgmVolume()        { return bgmVolume; }
    public void   setBgmVolume(double v){ bgmVolume = v; }

    public double getSeVolume()         { return seVolume; }
    public void   setSeVolume(double v) { seVolume = v; }

    public boolean isBgmEnabled()          { return bgmEnabled; }
    public void    setBgmEnabled(boolean b){ bgmEnabled = b; }

    public boolean isSeEnabled()           { return seEnabled; }
    public void    setSeEnabled(boolean b) { seEnabled = b; }

    public int getHighScore() { return highScore; }

    public void updateHighScore(int score) {
        if (score > highScore) highScore = score;
    }

    public boolean hasSeenEnding(String id) {
        return seenEndings.contains(id);
    }

    public int seenEndingCount() {
        return seenEndings.size();
    }

    /**
     * エンディングを踏んだ記録を残す。<b>読み終わりではなくルートに入った時点で呼ぶ</b>。
     * 送り切ったときだけ記録すると、スキップした人が回想に追加できず
     * 「飛ばしたら二度と読めない」になってしまう（スキップを入れた理由と噛み合わない）。
     *
     * <p>新しく増えたときだけ即保存する。次に落ちても記録が消えないように。
     */
    public void markEndingSeen(String id) {
        if (id == null || id.isBlank()) return;
        if (seenEndings.add(id)) save();
    }

    public void save() {
        try {
            Files.createDirectories(SAVE_FILE.getParent());
            Properties props = new Properties();
            props.setProperty("bgmVolume",  String.valueOf(bgmVolume));
            props.setProperty("seVolume",   String.valueOf(seVolume));
            props.setProperty("bgmEnabled", String.valueOf(bgmEnabled));
            props.setProperty("seEnabled",  String.valueOf(seEnabled));
            props.setProperty("highScore",  String.valueOf(highScore));
            props.setProperty("seenEndings", String.join(",", seenEndings));
            try (OutputStream os = Files.newOutputStream(SAVE_FILE)) {
                props.store(os, "9pazzle settings");
            }
        } catch (IOException e) {
            System.err.println("[Config] Failed to save: " + e.getMessage());
        }
    }

    public void load() {
        if (!Files.exists(SAVE_FILE)) return;
        try (InputStream is = Files.newInputStream(SAVE_FILE)) {
            Properties props = new Properties();
            props.load(is);
            bgmVolume  = parseDouble(props, "bgmVolume",  bgmVolume);
            seVolume   = parseDouble(props, "seVolume",   seVolume);
            bgmEnabled = parseBoolean(props, "bgmEnabled", bgmEnabled);
            seEnabled  = parseBoolean(props, "seEnabled",  seEnabled);
            highScore  = parseInt(props, "highScore", 0);
            readSeenEndings(props.getProperty("seenEndings", ""));
        } catch (IOException e) {
            System.err.println("[Config] Failed to load: " + e.getMessage());
        }
    }

    /** 空文字・区切りだけ・前後の空白を吸収する。手で書き換えても壊れないように */
    private void readSeenEndings(String csv) {
        seenEndings.clear();
        if (csv == null || csv.isBlank()) return;
        for (String id : csv.split(",")) {
            String trimmed = id.trim();
            if (!trimmed.isEmpty()) seenEndings.add(trimmed);
        }
    }

    private static double parseDouble(Properties p, String key, double def) {
        try { return Double.parseDouble(p.getProperty(key, String.valueOf(def))); }
        catch (NumberFormatException e) { return def; }
    }

    private static boolean parseBoolean(Properties p, String key, boolean def) {
        String v = p.getProperty(key);
        return v != null ? Boolean.parseBoolean(v) : def;
    }

    private static int parseInt(Properties p, String key, int def) {
        try { return Integer.parseInt(p.getProperty(key, String.valueOf(def))); }
        catch (NumberFormatException e) { return def; }
    }
}
