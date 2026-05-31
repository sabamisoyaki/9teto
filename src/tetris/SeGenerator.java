package tetris;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SeGenerator {

    private static final int SAMPLE_RATE = 44100;

    public static void generateMissingSeFiles() {
        Path audioDir = ResourcePath.of("audio");
        try {
            if (!Files.exists(audioDir)) {
                Files.createDirectories(audioDir);
            }
            
            checkAndGenerate(audioDir.resolve("se_move.wav"), SeType.MOVE);
            checkAndGenerate(audioDir.resolve("se_rotate.wav"), SeType.ROTATE);
            checkAndGenerate(audioDir.resolve("se_harddrop.wav"), SeType.HARD_DROP);
            checkAndGenerate(audioDir.resolve("se_lock.wav"), SeType.LOCK);
            checkAndGenerate(audioDir.resolve("se_clear.wav"),        SeType.CLEAR);
            checkAndGenerate(audioDir.resolve("se_world_rotate.wav"), SeType.WORLD_ROTATE);
        } catch (IOException e) {
            System.err.println("[SE Generator] Failed to create directories or write files: " + e.getMessage());
        }
    }

    private static void checkAndGenerate(Path path, SeType type) {
        if (Files.exists(path)) {
            return;
        }
        System.out.println("[SE Generator] Generating missing sound: " + path.getFileName());
        try {
            byte[] wavData = generateWavData(type);
            Files.write(path, wavData);
        } catch (IOException e) {
            System.err.println("[SE Generator] Failed to generate " + path.getFileName() + ": " + e.getMessage());
        }
    }

    private enum SeType {
        MOVE, ROTATE, HARD_DROP, LOCK, CLEAR, WORLD_ROTATE
    }

    private static byte[] generateWavData(SeType type) {
        double duration = switch (type) {
            case MOVE -> 0.06;
            case ROTATE -> 0.08;
            case HARD_DROP -> 0.15;
            case LOCK -> 0.15;
            case CLEAR -> 0.45;
            case WORLD_ROTATE -> 0.08;
        };

        int numSamples = (int) (SAMPLE_RATE * duration);
        short[] samples = new short[numSamples];

        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double sampleValue = 0;

            switch (type) {
                case MOVE -> {
                    // Quick high beep chirp
                    double freq = 700 + (t / duration) * 500; // 700 to 1200 Hz
                    double env = Math.exp(-t * 60); // Fast decay
                    sampleValue = Math.sin(2 * Math.PI * freq * t) * env;
                }
                case ROTATE -> {
                    // Quick upward-sweep bubble sound
                    double freq = 500 + Math.sin(t / duration * Math.PI * 4) * 200 + (t / duration) * 300;
                    double env = Math.sin(t / duration * Math.PI); // Smooth attack/decay
                    sampleValue = Math.sin(2 * Math.PI * freq * t) * env;
                }
                case HARD_DROP -> {
                    // Descending laser/impact
                    double freq = 1200 - (t / duration) * 1050; // 1200 down to 150 Hz
                    double env = Math.exp(-t * 25);
                    sampleValue = Math.sin(2 * Math.PI * freq * t) * env;
                }
                case LOCK -> {
                    // Solid low thump
                    double freq = 180 - (t / duration) * 100; // 180 down to 80 Hz
                    double env = Math.exp(-t * 20);
                    // Add a tiny bit of noise for texture
                    double noise = Math.random() * 2.0 - 1.0;
                    sampleValue = (Math.sin(2 * Math.PI * freq * t) * 0.8 + noise * 0.2) * env;
                }
                case WORLD_ROTATE -> {
                    // テスト用: ROTATE と同じ波形で代替
                    double freq = 500 + Math.sin(t / duration * Math.PI * 4) * 200 + (t / duration) * 300;
                    double env = Math.sin(t / duration * Math.PI);
                    sampleValue = Math.sin(2 * Math.PI * freq * t) * env;
                }
                case CLEAR -> {
                    // Beautiful sparkling C major arpeggio sweep (C5 -> E5 -> G5 -> C6)
                    double noteDuration = duration / 4.0;
                    int noteIndex = (int) (t / noteDuration);
                    double freq = switch (noteIndex) {
                        case 0 -> 523.25; // C5
                        case 1 -> 659.25; // E5
                        case 2 -> 783.99; // G5
                        default -> 1046.50; // C6
                    };
                    double noteT = t - (noteIndex * noteDuration);
                    double env = Math.exp(-noteT * 12) * (1.0 - (t / duration) * 0.5);
                    sampleValue = Math.sin(2 * Math.PI * freq * t) * env;
                }
            }

            // Clamp and convert to 16-bit PCM
            double volume = 0.5; // Moderate volume
            samples[i] = (short) (Math.max(-1.0, Math.min(1.0, sampleValue)) * 32767 * volume);
        }

        // Now build the WAV byte array
        int subChunk2Size = numSamples * 2; // 16-bit = 2 bytes per sample
        int chunkSize = 36 + subChunk2Size;
        byte[] wav = new byte[44 + subChunk2Size];

        // RIFF header
        wav[0] = 'R'; wav[1] = 'I'; wav[2] = 'F'; wav[3] = 'F';
        writeInt(wav, 4, chunkSize);
        wav[8] = 'W'; wav[9] = 'A'; wav[10] = 'V'; wav[11] = 'E';

        // fmt subchunk
        wav[12] = 'f'; wav[13] = 'm'; wav[14] = 't'; wav[15] = ' ';
        writeInt(wav, 16, 16); // Subchunk1Size
        writeShort(wav, 20, (short) 1); // AudioFormat = 1 (PCM)
        writeShort(wav, 22, (short) 1); // NumChannels = 1 (Mono)
        writeInt(wav, 24, SAMPLE_RATE);
        writeInt(wav, 28, SAMPLE_RATE * 2); // ByteRate = SAMPLE_RATE * NumChannels * BitsPerSample/8
        writeShort(wav, 32, (short) 2); // BlockAlign = NumChannels * BitsPerSample/8
        writeShort(wav, 34, (short) 16); // BitsPerSample = 16

        // data subchunk
        wav[36] = 'd'; wav[37] = 'a'; wav[38] = 't'; wav[39] = 'a';
        writeInt(wav, 40, subChunk2Size);

        // write sample data
        for (int i = 0; i < numSamples; i++) {
            int offset = 44 + i * 2;
            wav[offset] = (byte) (samples[i] & 0xFF);
            wav[offset + 1] = (byte) ((samples[i] >> 8) & 0xFF);
        }

        return wav;
    }

    private static void writeInt(byte[] data, int offset, int val) {
        data[offset] = (byte) (val & 0xFF);
        data[offset + 1] = (byte) ((val >> 8) & 0xFF);
        data[offset + 2] = (byte) ((val >> 16) & 0xFF);
        data[offset + 3] = (byte) ((val >> 24) & 0xFF);
    }

    private static void writeShort(byte[] data, int offset, short val) {
        data[offset] = (byte) (val & 0xFF);
        data[offset + 1] = (byte) ((val >> 8) & 0xFF);
    }

    private SeGenerator() {
    }
}
