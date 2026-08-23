package tetris;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

import javafx.scene.media.AudioClip;
import tetris.model.GameConfig;
import tetris.model.SeEvent;

public class SePlayer {

    private final Map<SeEvent, AudioClip> clips = new EnumMap<>(SeEvent.class);
    private final GameConfig config;

    public SePlayer(GameConfig config) {
        this.config = config;
        SeGenerator.generateMissingSeFiles();
        load(SeEvent.MOVE,       "se_move.wav");
        load(SeEvent.ROTATE,     "se_rotate.wav");
        load(SeEvent.HARD_DROP,  "se_harddrop.wav");
        load(SeEvent.LOCK,       "se_lock.wav");
        load(SeEvent.LINE_CLEAR,   "se_clear.wav");
        load(SeEvent.WORLD_ROTATE, "se_world_rotate.wav");
        load(SeEvent.HOLD,         "se_hold.wav");
        load(SeEvent.T_SPIN,       "se_tspin.wav");
        load(SeEvent.T_SPIN_MINI,  "se_tspin_mini.wav");
        load(SeEvent.REN,          "se_ren.wav");
        load(SeEvent.TEMP_GAME_OVER, "se_pinch.wav");
    }

    private void load(SeEvent event, String filename) {
        Path p = ResourcePath.of("audio", filename);
        if (Files.exists(p)) {
            clips.put(event, new AudioClip(p.toUri().toString()));
        }
    }

    public void play(SeEvent event) {
        if (!config.isSeEnabled()) return;
        AudioClip clip = clips.get(event);
        if (clip != null) {
            clip.play(config.getSeVolume());
        }
    }
}
