package tetris;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import tetris.model.RngHub;

/**
 * エラー時に {@code log/error_<日時>.txt} を書く。
 *
 * <p>乱数の値そのものは記録しない。シードがあれば数列は完全に再現できるので、
 * 書くのはシード・各ストリームの消費回数・スタックトレースだけ。
 *
 * <p>{@code log/} には JVM が書くファイル（{@code hs_err_pid*.log} =
 * native クラッシュ、{@code replay_pid*.log} = JIT の再現データ）も落ちるため、
 * ゲーム側が書くものは {@code error_} で始める。
 */
public final class ErrorDump {

    private static final Path LOG_DIR = ResourcePath.of("log");
    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private ErrorDump() {}

    /**
     * 例外を握ったところから呼ぶ。
     *
     * @param hub このプレイの乱数の元。シーン生成前なら null でよい
     */
    public static void write(Throwable t, RngHub hub) {
        try {
            LocalDateTime now = LocalDateTime.now();

            StringBuilder sb = new StringBuilder();
            sb.append("9pazzle error dump\r\n");
            sb.append("time    : ").append(now).append("\r\n");
            if (hub != null) {
                sb.append("seed    : ").append(hub.seed()).append("\r\n");
                sb.append("draws   : ").append(hub.drawCounts()).append("\r\n");
                sb.append("replay  : -Dgame.seed=").append(hub.seed()).append("\r\n");
            } else {
                sb.append("seed    : (未初期化)\r\n");
            }
            sb.append("thread  : ").append(Thread.currentThread().getName()).append("\r\n");
            sb.append("\r\n");

            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            sb.append(sw);

            Files.createDirectories(LOG_DIR);
            Path out = LOG_DIR.resolve("error_" + now.format(STAMP) + ".txt");
            Files.writeString(out, sb.toString());
            System.out.println("[ErrorDump] " + out.toAbsolutePath());
        } catch (IOException | RuntimeException dumpFailure) {
            // ダンプ処理自身が落ちても本体を巻き込まない。ここだけは絶対に投げない
            System.err.println("[ErrorDump] 書き出しに失敗: " + dumpFailure);
            t.printStackTrace();
        }
    }
}
