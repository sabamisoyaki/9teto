package tetris;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.util.Duration;
import tetris.model.Board;
import tetris.model.Rng;
import tetris.model.SeededRng;
import tetris.model.Scenario;
import tetris.model.ScenarioRoute;
import tetris.model.ShapeType;
import tetris.model.Tetromino;
import tetris.view.HudPane;
import tetris.view.Render;
import tetris.view.UiLayout;
import tetris.view.UiLayoutBank;
import tetris.view.UiMetrics;
import tetris.view.UiSkin;
import tetris.view.UiSkinBank;

/**
 * UI 確認用の一括撮影。各画面を順に組んで PNG へ落とし、終わったらアプリを閉じる。
 * {@code -Dshot.out=<出力先>} を付けて起動すると Main から呼ばれる（shot.bat 参照）。
 *
 * <p>画面座標を撮る外部キャプチャではなく {@link Scene#snapshot} を使う。ウィンドウを
 * 前面へ固定する必要がなく、DPI 仮想化による座標ズレも無く、何より演出の途中フレームを
 * 踏まない。撮影モードでは Main 側がフェードや点滅を止めているので（Main.shotMode）、
 * 同じリビジョンなら何度撮っても同じ絵になる。
 *
 * <p>唯一の例外はゲームオーバー画面のハイスコア行で、ここだけは保存済みの実データを
 * そのまま映す。プレイして記録が伸びると当然その1行だけ変わる。
 */
final class ShotRunner {

    /**
     * シーンを差し替えてから撮るまでの待ち時間。レイアウト確定と Canvas 反映に
     * 数パルス、セリフのフェードイン（200ms）にこれだけ要る。
     * 短くすると 1 つ前の絵や演出の途中が写る。
     */
    private static final int SETTLE_MS = 260;

    /** ダミー盤面の乱数シード。固定なので何度撮っても同じ積みになる */
    private static final long BOARD_SEED = 20260821L;

    /** 縦穴を1本残す列。全段が埋まった盤面は「消えていないのが不自然」に見える */
    private static final int WELL_COL = 18;

    /** ポーズ・セリフを重ねる下地の配置（CLASSIC）。パネル様式なら HUD が全部出る */
    private static final int PANEL_LAYOUT_INDEX = 0;

    // HUD へ流し込む値。実データを使うとマシンやプレイ状況で絵が変わってしまう
    private static final int DUMMY_SCORE = 128450;
    private static final int DUMMY_BEST  = 342100;
    private static final int DUMMY_LINES = 47;
    private static final int DUMMY_LEVEL = 10;

    private final Main app;
    private final Stage stage;
    private final Path outDir;

    private final List<Shot> shots = new ArrayList<>();
    private int index = 0;
    private int written = 0;
    private long startedNanos = 0;

    /** 1 枚ぶんの撮影。prepare で画面を作り込み、落ち着いてから name.png へ落とす */
    private record Shot(String name, Runnable prepare) {}

    // ゲーム画面は 1 度だけ組んで、配置とフロアを差し替えながら撮る（F3 / F2 と同じ動き）
    private Main.GameSceneParts game;
    private Scene gameScene;
    private Board dummyBoard;
    private Tetromino current;
    private Tetromino ghost;
    private Tetromino holdPiece;
    private Tetromino nextPiece;

    ShotRunner(Main app, Stage stage, Path outDir) {
        this.app = app;
        this.stage = stage;
        this.outDir = outDir;
    }

    void run() {
        try {
            Files.createDirectories(outDir);
        } catch (IOException e) {
            // ログは ASCII で出す。cmd の既定コードページだと日本語が化けて読めない
            System.out.println("[shot] cannot create output dir: " + outDir + " (" + e.getMessage() + ")");
            Platform.exit();
            return;
        }

        prepareGameScene();
        buildShotList();

        System.out.println("[shot] " + shots.size() + " shots -> " + outDir.toAbsolutePath());
        startedNanos = System.nanoTime();
        step();
    }

    // =====================================================
    //  撮影リスト
    // =====================================================

    private void buildShotList() {
        add("01-start",  () -> stage.setScene(app.makeStartScene()));
        add("02-config", () -> stage.setScene(app.makeConfigScene()));
        add("02b-seed",  () -> stage.setScene(app.makeSeedScene()));
        add("02c-keyconfig", () -> stage.setScene(app.makeKeyConfigScene()));

        // ゲーム画面は 配置 × フロア の全組合せ。1 枚ずつシーンを作り直すのではなく、
        // 本編で F3 / F2 を押したときと同じ入れ替えを通すことで、実機と同じ絵にする
        for (int li = 0; li < UiLayoutBank.count(); li++) {
            for (int si = 0; si < UiSkinBank.skinCount(); si++) {
                UiLayout layout = UiLayoutBank.get(li);
                UiSkin skin = UiSkinBank.forStep(si);
                String name = String.format("03-game_L%d-%s_S%d-%s",
                        li + 1, slug(layout.name), si + 1, slug(skin.name));
                add(name, () -> showGame(layout, skin));
            }
        }

        // オーバーレイ系は既定の配置・フロアの上に重ねて撮る
        add("04-pause", () -> {
            showPanelGame();
            game.pauseOverlay.setVisible(true);
        });

        // セリフは枠がどこまで伸びるかを見たいので、短・中・長の 3 本を撮る
        String[][] dialogues = {
            {"short", "いくよ"},
            {"mid",   "その積み方、悪くないんじゃない？"},
            {"long",  "……この階はもう長いね。そろそろ上がろっか。"
                    + "次のフロアは天井が低いから、横に伸ばすより先に穴を埋めたほうがいいと思う"},
        };
        for (String[] d : dialogues) {
            add("05-dialogue-" + d[0], () -> {
                game.pauseOverlay.setVisible(false);
                showPanelGame();
                game.view.getHudPane().showDialogue(d[1]);
            });
        }

        // ハイスコアの更新・保存を伴う showGameOverScene ではなく、画面生成だけを呼ぶ
        add("06-gameover",  () -> stage.setScene(app.makeGameOverScene(DUMMY_SCORE, DUMMY_LINES)));
        add("07-endcredit", () -> stage.setScene(
                app.makeEndCreditScene(Main.DEFAULT_END_CREDIT_JSON, null)));

        // アドベンチャーパートは本編フローだと撮影モードで飛ばされる（Main.showAdventureScene）。
        // 画面を直接組んで、各ルートの 1 ページ目を撮る。立ち絵と本文の枠が
        // どう出るかを配置の変更ごとに見比べられるようにしておく
        addAdventureShots(Scenario.TUTORIAL, "08-adv-tut");
        addAdventureShots(Scenario.OPENING, "08-adv-op");
        addAdventureShots(Scenario.ENDING,  "09-adv-ed");

        // 回想モードは既読の状態で見た目が変わる。撮れるのは今の保存データの状態
        add("10-recollection", () -> stage.setScene(app.makeRecollectionScene()));
    }

    /** そのパートの全ルートを 1 枚ずつ。シナリオが無ければ 1 枚も足さない */
    private void addAdventureShots(String part, String prefix) {
        List<ScenarioRoute> routes = Scenario.load().routesOf(part);
        for (int i = 0; i < routes.size(); i++) {
            ScenarioRoute route = routes.get(i);
            String name = String.format("%s%d-%s", prefix, i + 1, slug(route.id()));
            add(name, () -> stage.setScene(app.makeAdventureScene(route, () -> { })));
        }
    }

    private void add(String name, Runnable prepare) {
        shots.add(new Shot(name, prepare));
    }

    // =====================================================
    //  進行
    // =====================================================

    private void step() {
        if (index >= shots.size()) {
            finish();
            return;
        }
        Shot shot = shots.get(index++);
        try {
            shot.prepare().run();
        } catch (RuntimeException e) {
            System.out.println("[shot] " + shot.name() + " prepare failed: " + e);
            step();
            return;
        }

        // setScene 直後に撮ると 1 パルス前の絵が写る。レイアウトと短い演出が
        // 落ち着くのを待ってからシャッターを切る
        PauseTransition settle = new PauseTransition(Duration.millis(SETTLE_MS));
        settle.setOnFinished(e -> {
            capture(shot.name());
            step();
        });
        settle.play();
    }

    private void capture(String name) {
        Path file = outDir.resolve(name + ".png");
        try {
            // NextPane.draw() は枠の実サイズ（getWidth）を見て描くので、レイアウトパスが
            // 走る前に描くと NEXT / HOLD が空になる。だから描くのはシャッターの直前。
            // Scene.snapshot() は同期時に Canvas の描画コマンドを吐き出すので間に合う
            if (stage.getScene() == gameScene) {
                redrawGame();
            }
            WritableImage image = stage.getScene().snapshot(null);
            writePng(image, file);
            written++;
            System.out.printf("[shot] %2d/%d  %s%n", index, shots.size(), file.getFileName());
        } catch (Exception e) {
            System.out.println("[shot] " + name + " write failed: " + e);
        }
    }

    private void finish() {
        double secs = (System.nanoTime() - startedNanos) / 1_000_000_000.0;
        System.out.printf("[shot] done %d/%d (%.1f s) -> %s%n",
                written, shots.size(), secs, outDir.toAbsolutePath());
        Platform.exit();
    }

    // =====================================================
    //  ゲーム画面
    // =====================================================

    private void prepareGameScene() {
        game = app.buildGameSceneParts();
        gameScene = new Scene(game.gameRoot, UiMetrics.SCREEN_W, UiMetrics.SCREEN_H);

        dummyBoard = new Board();
        paintDummyStack(dummyBoard);

        // 落下中のミノは積みの上へ浮かせて、ゴーストと一緒に見えるようにする
        current = piece(ShapeType.T, 5, 10);
        ghost = dropped(current);
        holdPiece = piece(ShapeType.I, 0, 0);
        nextPiece = piece(ShapeType.L, 0, 0);

        HudPane hud = game.view.getHudPane();
        hud.setBestScore(DUMMY_BEST);
        hud.updateScore(DUMMY_SCORE);
        hud.updateLines(DUMMY_LINES);
        hud.updateLevel(DUMMY_LEVEL);
        hud.updateRotateCountdown(1, 3);
        hud.updateDangerGauge(2, 4);
    }

    /**
     * オーバーレイ系を重ねる下地。既定配置（ROUGH A）ではなく CLASSIC を使う。
     * OVERLAY 様式は HudPane.applyStyle() がセリフ枠ごと隠す仕様なので、
     * 既定配置のままではセリフの撮れ高がゼロになる。
     */
    private void showPanelGame() {
        showGame(UiLayoutBank.get(PANEL_LAYOUT_INDEX), UiSkinBank.forStep(0));
    }

    private void showGame(UiLayout layout, UiSkin skin) {
        game.view.applyLayout(layout);
        // 配置が変わると様式も変わるので見た目を入れ直す（GameView.cycleLayout と同じ順）
        game.view.applySkin(skin);
        stage.setScene(gameScene);
        // 盤面と NEXT / HOLD を描くのは capture() の直前。ここで描いても
        // このあとのレイアウトパスで枠が組み変わって描き直しになる
    }

    /** 盤面と NEXT / HOLD を描き直す。本編は毎フレーム描いているが撮影モードには時計が無い */
    private void redrawGame() {
        Render renderer = game.renderer;
        renderer.drawAll(
                game.view.getPlayFieldPane().getPlayfieldCanvas().getGraphicsContext2D(),
                dummyBoard, current, ghost);
        game.view.getHoldPane().draw(renderer, holdPiece, false);
        game.view.getNextPane().draw(renderer, nextPiece, false);
    }

    /**
     * 実戦っぽい積みを固定シードで作る。手書きのパターン表と違って桁がズレないし、
     * シードが同じなら毎回同じ絵になるので UI 改修の差分比較に使える。
     */
    private static void paintDummyStack(Board board) {
        Rng rnd = new SeededRng(BOARD_SEED);
        ShapeType[] palette = ShapeType.values();
        for (int c = 0; c < Board.COLS; c++) {
            int height = 7 + (int) Math.round(3 * Math.sin(c * 0.7)) + rnd.nextInt(3);
            for (int h = 0; h < height; h++) {
                if (c == WELL_COL && h >= 2) continue;
                if (rnd.nextInt(11) == 0) continue; // 穴を散らす
                // 色は 2x2 のまとまりで変える。1 マスごとに散らすと積みに見えない
                ShapeType type = palette[((c / 2) + (h / 2) * 3) % palette.length];
                paintCell(board, type, Board.ROWS - 1 - h, c);
            }
        }
    }

    /**
     * 盤面の 1 マスだけを塗る。Board にセッターは無いが、fixToBoard() は
     * Tetromino の 4x4 マスクをそのまま見るので、1 マスだけ立てたマスクを渡せばよい。
     */
    private static void paintCell(Board board, ShapeType type, int row, int col) {
        Tetromino t = new Tetromino(type);
        int[][] mask = new int[4][4];
        mask[0][0] = 1;
        t.setShape(mask);
        t.setRow(row);
        t.setCol(col);
        board.fixToBoard(t);
    }

    private static Tetromino piece(ShapeType type, int row, int col) {
        Tetromino t = new Tetromino(type);
        t.setRow(row);
        t.setCol(col);
        return t;
    }

    /** 落下先のゴースト。本編の GameController.getGhost() と同じく行けるところまで下げる */
    private Tetromino dropped(Tetromino src) {
        Tetromino g = src.copy();
        while (dummyBoard.canMoveDown(g)) {
            g.setRow(g.getRow() + 1);
        }
        return g;
    }

    /** 配置名・フロア名をファイル名へ落とす（"1F ARCADE" -> "1F-ARCADE"） */
    private static String slug(String name) {
        String s = name.replaceAll("[^A-Za-z0-9]+", "-");
        s = s.replaceAll("^-+|-+$", "");
        return s.isEmpty() ? "x" : s;
    }

    // =====================================================
    //  PNG 書き出し
    //  SwingFXUtils なら 1 行で済むが、そのためだけに javafx.swing を
    //  製品のモジュール構成へ足したくないので、必要な最小限を自前で組む。
    // =====================================================

    private static void writePng(WritableImage image, Path file) throws IOException {
        int w = (int) image.getWidth();
        int h = (int) image.getHeight();
        PixelReader reader = image.getPixelReader();

        // 各行 = フィルタ種別 1 バイト + RGB。フィルタは Sub（左隣との差分）。
        // UI は横方向に平坦な面が多いので、無フィルタよりだいぶ小さくなる
        byte[] raw = new byte[h * (1 + w * 3)];
        int[] row = new int[w];
        int p = 0;
        for (int y = 0; y < h; y++) {
            raw[p++] = 1;
            reader.getPixels(0, y, w, 1, PixelFormat.getIntArgbInstance(), row, 0, w);
            for (int x = 0; x < w; x++) {
                int cur = row[x];
                int left = x > 0 ? row[x - 1] : 0;
                raw[p++] = (byte) ((cur >> 16) - (left >> 16));
                raw[p++] = (byte) ((cur >> 8) - (left >> 8));
                raw[p++] = (byte) (cur - left);
            }
        }

        byte[] idat = deflate(raw);

        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(file))) {
            out.write(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});

            ByteArrayOutputStream header = new ByteArrayOutputStream(13);
            DataOutputStream hd = new DataOutputStream(header);
            hd.writeInt(w);
            hd.writeInt(h);
            hd.writeByte(8); // ビット深度
            hd.writeByte(2); // カラータイプ: RGB（シーンの背景は不透明なので α は要らない）
            hd.writeByte(0); // 圧縮方式: deflate
            hd.writeByte(0); // フィルタ方式
            hd.writeByte(0); // インタレース: なし

            writeChunk(out, "IHDR", header.toByteArray());
            writeChunk(out, "IDAT", idat);
            writeChunk(out, "IEND", new byte[0]);
        }
    }

    private static void writeChunk(OutputStream out, String type, byte[] data) throws IOException {
        byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
        DataOutputStream d = new DataOutputStream(out);
        d.writeInt(data.length);
        d.write(typeBytes);
        d.write(data);

        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        d.writeInt((int) crc.getValue());
    }

    private static byte[] deflate(byte[] data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(data.length / 4);
        try (Deflater deflater = new Deflater()) {
            deflater.setInput(data);
            deflater.finish();

            byte[] buf = new byte[64 * 1024];
            while (!deflater.finished()) {
                out.write(buf, 0, deflater.deflate(buf));
            }
        }
        return out.toByteArray();
    }
}
