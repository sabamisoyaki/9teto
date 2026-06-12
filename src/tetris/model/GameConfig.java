package tetris.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class GameConfig {

    private double bgmVolume  = 1.0;
    private double seVolume   = 0.70;
    private boolean bgmEnabled = true;
    private boolean seEnabled  = true;
    private int highScore = 0;

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

    public void save() {
        try {
            Files.createDirectories(SAVE_FILE.getParent());
            Properties props = new Properties();
            props.setProperty("bgmVolume",  String.valueOf(bgmVolume));
            props.setProperty("seVolume",   String.valueOf(seVolume));
            props.setProperty("bgmEnabled", String.valueOf(bgmEnabled));
            props.setProperty("seEnabled",  String.valueOf(seEnabled));
            props.setProperty("highScore",  String.valueOf(highScore));
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
        } catch (IOException e) {
            System.err.println("[Config] Failed to load: " + e.getMessage());
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
