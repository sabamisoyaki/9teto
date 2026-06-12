package tetris;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import tetris.controller.GameController;
import tetris.model.Board;
import tetris.model.GameConfig;
import tetris.model.SeEvent;
import tetris.view.EndCreditPane;
import tetris.view.ConfigPane;
import tetris.view.GameView;
import tetris.view.HudPane;
import tetris.view.NextPane;
import tetris.view.Particle;
import tetris.view.Render;
import tetris.view.UiTheme;

public class Main extends Application {

    private Stage primaryStage;
    private static final int WINDOW_WIDTH = 1920;
    private static final int WINDOW_HEIGHT = 1080;
    private static final Path BGM_PATH = ResourcePath.of("audio", "bgm.wav");
    private static final double BGM_MAX_VOLUME = 0.35;
    private MediaPlayer bgmPlayer;
    private final GameConfig config = new GameConfig();

    private static final Path MAIN_BACKGROUND_IMAGE = ResourcePath.of("images", "base-layer-1920x1080.png");
    private static final Path END_CREDIT_BACKGROUND_IMAGE = ResourcePath.of("images", "end-credit-bg.png");
    private static final Path START_BACKGROUND_IMAGE = ResourcePath.of("images", "start-bg.png");
    private static final Path GAME_OVER_BACKGROUND_IMAGE = ResourcePath.of("images", "game-over-bg.png");

    private static final String DEFAULT_END_CREDIT_JSON = """
            {
              "title": "THANK YOU FOR PLAYING",
              "sections": [
                {"heading": "Development", "lines": ["Game Design", "Programming", "Balancing"]},
                {"heading": "Art", "lines": ["UI Layout", "Character Illustration"]},
                {"heading": "Special Thanks", "lines": ["All Players", "Open Source Community"]}
              ]
            }
            """;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("TETRIS");
        stage.setResizable(false);
        config.load();
        initBgmPlayer();
        showStartScene();
        stage.show();
    }

    private void initBgmPlayer() {
        if (!Files.exists(BGM_PATH)) {
            System.out.println("[BGM] Not found: " + BGM_PATH.toAbsolutePath());
            return;
        }

        try {
            Media media = new Media(BGM_PATH.toUri().toString());
            bgmPlayer = new MediaPlayer(media);
            bgmPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            bgmPlayer.setVolume(config.getBgmVolume() * BGM_MAX_VOLUME);
        } catch (Exception e) {
            System.out.println("[BGM] Failed to load: " + e.getMessage());
            bgmPlayer = null;
        }
    }

    private void playBgm() {
        if (bgmPlayer == null || !config.isBgmEnabled()) {
            return;
        }
        bgmPlayer.setVolume(config.getBgmVolume() * BGM_MAX_VOLUME);
        if (bgmPlayer.getStatus() != MediaPlayer.Status.PLAYING) {
            bgmPlayer.play();
        }
    }

    private void fadeOutBgm(double durationSecs, Runnable onFinished) {
        if (bgmPlayer == null || bgmPlayer.getStatus() != MediaPlayer.Status.PLAYING) {
            if (onFinished != null) onFinished.run();
            return;
        }
        double startVol = bgmPlayer.getVolume();
        Timeline fade = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(bgmPlayer.volumeProperty(), startVol)),
            new KeyFrame(Duration.seconds(durationSecs), new KeyValue(bgmPlayer.volumeProperty(), 0.0))
        );
        fade.setOnFinished(e -> {
            bgmPlayer.stop();
            bgmPlayer.seek(Duration.ZERO);
            if (onFinished != null) onFinished.run();
        });
        fade.play();
    }

    // =====================================================
    //  スタート画面
    // =====================================================
    private Scene makeStartScene() {
        StackPane root = new StackPane();
        Path bgPath = Files.exists(START_BACKGROUND_IMAGE) ? START_BACKGROUND_IMAGE : MAIN_BACKGROUND_IMAGE;
        applyBackgroundImage(root, bgPath, true);

        Rectangle overlay = new Rectangle(WINDOW_WIDTH, WINDOW_HEIGHT, Color.rgb(10, 15, 20, 0.6));

        VBox content = new VBox(40);
        content.setAlignment(Pos.CENTER);

        Label title = new Label("TETRIS");
        title.setStyle("-fx-font-size: 100px; -fx-font-weight: bold; -fx-text-fill: #f0f0f0; -fx-font-family: 'Courier New'; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 10, 0.0, 2, 2);");
        TranslateTransition titleTt = new TranslateTransition(Duration.seconds(1), title);
        titleTt.setFromY(-200);
        titleTt.setToY(0);
        titleTt.setInterpolator(Interpolator.EASE_OUT);
        titleTt.play();

        Label sub = new Label("Press SPACE to Start");
        sub.setStyle("-fx-font-size: 28px; -fx-text-fill: #aaddff; -fx-font-family: 'Courier New'; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 5, 0.0, 1, 1);");

        FadeTransition fade = new FadeTransition(Duration.seconds(1.0), sub);
        fade.setFromValue(1.0);
        fade.setToValue(0.2);
        fade.setCycleCount(Animation.INDEFINITE);
        fade.setAutoReverse(true);
        fade.play();

        Label configHint = new Label("C  Config");
        configHint.setStyle("-fx-font-size: 20px; -fx-text-fill: #6688aa; -fx-font-family: 'Courier New';");

        int hs = config.getHighScore();
        Label highScoreLabel = new Label(hs > 0 ? "Best: " + hs : "");
        highScoreLabel.setStyle("-fx-font-size: 22px; -fx-text-fill: #99ccff; -fx-font-family: 'Courier New';");

        content.getChildren().addAll(title, sub, configHint, highScoreLabel);
        root.getChildren().addAll(overlay, content);

        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);

        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.SPACE) {
                showGameScene();
            } else if (e.getCode() == KeyCode.C) {
                showConfigScene();
            }
        });

        return scene;
    }

    private void showStartScene() {
        fadeOutBgm(0.8, null);
        primaryStage.setScene(makeStartScene());
    }

    // =====================================================
    //  コンフィグ画面
    // =====================================================
    private Scene makeConfigScene() {
        ConfigPane pane = new ConfigPane(
            config,
            v -> { if (bgmPlayer != null) bgmPlayer.setVolume(v * BGM_MAX_VOLUME); },
            v -> { /* SE未実装 */ },
            b -> { if (bgmPlayer != null) bgmPlayer.setVolume(b ? config.getBgmVolume() * BGM_MAX_VOLUME : 0.0); },
            b -> { /* SE未実装 */ }
        );

        Scene scene = new Scene(pane.getRoot(), WINDOW_WIDTH, WINDOW_HEIGHT);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                config.save();
                showStartScene();
            }
        });
        return scene;
    }

    private void showConfigScene() {
        primaryStage.setScene(makeConfigScene());
    }

    // =====================================================
    //  ゲーム画面
    // =====================================================
    private Scene makeGameScene() {
        GameView view = new GameView();
        GameController controller = new GameController();
        int cellSize = Math.min(
                (int) (view.getPlayFieldPane().getPlayfieldCanvas().getWidth() / Board.COLS),
                (int) (view.getPlayFieldPane().getPlayfieldCanvas().getHeight() / Board.ROWS));
        Render renderer = new Render(cellSize);
        NextPane holdPane = view.getHoldPane();
        NextPane nextPane = view.getNextPane();
        HudPane hudPane = view.getHudPane();

        // ポーズオーバーレイとカウントダウンラベル
        StackPane pauseOverlay = buildPauseOverlay();
        pauseOverlay.setVisible(false);

        Label countdownLabel = new Label();
        countdownLabel.setStyle(
            "-fx-font-size: 160px; -fx-font-weight: bold; -fx-text-fill: #ffffff;" +
            "-fx-font-family: 'Courier New';" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.95), 20, 0.0, 0, 0);");
        countdownLabel.setVisible(false);

        StackPane gameRoot = new StackPane(view.getRoot(), pauseOverlay, countdownLabel);

        Set<KeyCode> keys = new HashSet<>();
        // 初回描画
        renderer.drawAll(
                view.getPlayFieldPane().getPlayfieldCanvas().getGraphicsContext2D(),
                controller.getBoard(),
                controller.getCurrent(),
                controller.getGhost());
        renderer.drawNext(
                holdPane.getNextCanvas().getGraphicsContext2D(),
                controller.getHold(), 0, 0, !controller.canHold());
        renderer.drawNext(
                nextPane.getNextCanvas().getGraphicsContext2D(),
                controller.getNext(), 0, 0);

        SePlayer sePlayer = new SePlayer(config);

        Consumer<Integer> showCountdown = n -> {
            countdownLabel.setText(String.valueOf(n));
            countdownLabel.setVisible(true);
            ScaleTransition st = new ScaleTransition(Duration.millis(250), countdownLabel);
            st.setFromX(1.6);
            st.setFromY(1.6);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        };

        GameLoopTimer timer = new GameLoopTimer(
                controller,
                view,
                renderer,
                holdPane,
                nextPane,
                hudPane,
                keys,
                sePlayer,
                (finalScore, finalLines) -> showEndCreditScene(
                        DEFAULT_END_CREDIT_JSON,
                        () -> showGameOverScene(finalScore, finalLines)),
                () -> pauseOverlay.setVisible(true),
                () -> pauseOverlay.setVisible(false),
                showCountdown,
                () -> countdownLabel.setVisible(false));

        Scene scene = new Scene(gameRoot, WINDOW_WIDTH, WINDOW_HEIGHT);

        scene.setOnKeyReleased(e -> keys.remove(e.getCode()));
        scene.setOnKeyPressed(e -> {
            KeyCode code = e.getCode();
            if (code == KeyCode.ESCAPE || code == KeyCode.P) {
                timer.togglePause();
            } else if (timer.isGamePaused()) {
                if (code == KeyCode.SPACE) {
                    timer.togglePause();
                } else if (code == KeyCode.R) {
                    timer.stop();
                    showGameScene();
                } else if (code == KeyCode.T) {
                    timer.stop();
                    showStartScene();
                }
            } else if (!timer.isCountingDown()) {
                keys.add(code);
            }
        });

        timer.start();
        return scene;
    }

    private StackPane buildPauseOverlay() {
        Rectangle bg = new Rectangle(WINDOW_WIDTH, WINDOW_HEIGHT, Color.rgb(0, 0, 10, 0.82));

        VBox menu = new VBox(28);
        menu.setAlignment(Pos.CENTER);

        Label title = new Label("PAUSED");
        title.setStyle("-fx-font-size: 80px; -fx-font-weight: bold; -fx-text-fill: #cce0ff; -fx-font-family: 'Courier New';");

        String hintStyle = "-fx-font-size: 26px; -fx-text-fill: #88aacc; -fx-font-family: 'Courier New';";
        Label h1 = new Label("SPACE / P  ·  Resume");
        Label h2 = new Label("R          ·  New Game");
        Label h3 = new Label("T          ·  Title");
        h1.setStyle(hintStyle);
        h2.setStyle(hintStyle);
        h3.setStyle(hintStyle);

        menu.getChildren().addAll(title, h1, h2, h3);

        StackPane overlay = new StackPane(bg, menu);
        return overlay;
    }

    private void showGameScene() {
        playBgm();
        primaryStage.setScene(makeGameScene());
    }

    // =====================================================
    //  ゲームオーバー画面
    // =====================================================
    private Scene makeGameOverScene(int score, int lines) {
        StackPane root = new StackPane();
        Path bgPath = Files.exists(GAME_OVER_BACKGROUND_IMAGE) ? GAME_OVER_BACKGROUND_IMAGE : MAIN_BACKGROUND_IMAGE;
        applyBackgroundImage(root, bgPath, true);

        Rectangle overlay = new Rectangle(WINDOW_WIDTH, WINDOW_HEIGHT, Color.rgb(30, 0, 10, 0.7));

        VBox content = new VBox(40);
        content.setAlignment(Pos.CENTER);

        Label title = new Label("GAME OVER");
        title.setStyle("-fx-font-size: 100px; -fx-font-weight: bold; -fx-text-fill: #ff8888; -fx-font-family: 'Courier New'; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 10, 0.0, 2, 2);");

        VBox statsBox = new VBox(15);
        statsBox.setAlignment(Pos.CENTER);
        statsBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6); -fx-padding: 30px 60px; -fx-border-color: #557788; -fx-border-width: 1px; -fx-border-radius: 5px; -fx-background-radius: 5px;");
        statsBox.setMaxWidth(560);

        Label scoreLabel = new Label("Score:      " + score);
        scoreLabel.setStyle("-fx-font-size: 32px; -fx-text-fill: white; -fx-font-family: 'Courier New';");

        Label linesLabel = new Label("Lines:      " + lines);
        linesLabel.setStyle("-fx-font-size: 32px; -fx-text-fill: white; -fx-font-family: 'Courier New';");

        int hs = config.getHighScore();
        boolean isNewRecord = score == hs && score > 0;
        String hsText = "High Score: " + hs + (isNewRecord ? "  ★ NEW!" : "");
        Label highScoreLabel = new Label(hsText);
        highScoreLabel.setStyle("-fx-font-size: 32px; -fx-text-fill: " + (isNewRecord ? "#ffd700" : "#99ccff") + "; -fx-font-family: 'Courier New';");

        statsBox.getChildren().addAll(scoreLabel, linesLabel, highScoreLabel);

        VBox hints = new VBox(10);
        hints.setAlignment(Pos.CENTER);

        Label retry = new Label("SPACE  ·  Retry");
        retry.setStyle("-fx-font-size: 24px; -fx-text-fill: #aaddff; -fx-font-family: 'Courier New';");
        Label titleHint = new Label("T      ·  Title");
        titleHint.setStyle("-fx-font-size: 24px; -fx-text-fill: #7799bb; -fx-font-family: 'Courier New';");

        FadeTransition fade = new FadeTransition(Duration.seconds(0.8), retry);
        fade.setFromValue(1.0);
        fade.setToValue(0.3);
        fade.setCycleCount(Animation.INDEFINITE);
        fade.setAutoReverse(true);
        fade.play();

        hints.getChildren().addAll(retry, titleHint);
        content.getChildren().addAll(title, statsBox, hints);
        root.getChildren().addAll(overlay, content);

        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.SPACE) {
                showGameScene();
            } else if (e.getCode() == KeyCode.T) {
                showStartScene();
            }
        });

        return scene;
    }

    private void showGameOverScene(int score, int lines) {
        config.updateHighScore(score);
        config.save();

        fadeOutBgm(1.2, null);

        javafx.scene.Node root = primaryStage.getScene().getRoot();
        ScaleTransition st = new ScaleTransition(Duration.seconds(1), root);
        st.setToX(0.8);
        st.setToY(0.8);
        FadeTransition ft = new FadeTransition(Duration.seconds(1), root);
        ft.setToValue(0);
        ParallelTransition pt = new ParallelTransition(st, ft);
        pt.setOnFinished(e -> {
            root.setScaleX(1.0);
            root.setScaleY(1.0);
            root.setOpacity(1.0);
            primaryStage.setScene(makeGameOverScene(score, lines));
        });
        pt.play();
    }


    // =====================================================
    //  エンドクレジット画面
    // =====================================================
    private Scene makeEndCreditScene(String creditJson, Runnable onComplete) {

        EndCreditPane creditPane = new EndCreditPane(creditJson, END_CREDIT_BACKGROUND_IMAGE);
        Scene scene = new Scene(creditPane.getRoot(), WINDOW_WIDTH, WINDOW_HEIGHT);

        Timeline timeline = creditPane.buildScrollAnimation();
        Runnable completeAction = onComplete != null ? onComplete : this::showStartScene;

        timeline.setOnFinished(e -> completeAction.run());
        timeline.play();

        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE || e.getCode() == KeyCode.SPACE) {
                timeline.stop();
                completeAction.run();
            }
        });

        return scene;
    }

    private void showEndCreditScene(String creditJson, Runnable onComplete) {
        primaryStage.setScene(makeEndCreditScene(creditJson, onComplete));
    }


    private void applyBackgroundImage(StackPane target, Path imagePath, boolean applyBlur) {
        if (imagePath == null || !Files.exists(imagePath)) {
            target.setStyle("-fx-background-color: black;");
            return;
        }

        Image image = new Image(imagePath.toUri().toString());
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(WINDOW_WIDTH);
        imageView.setFitHeight(WINDOW_HEIGHT);

        if (applyBlur) {
            imageView.setEffect(new GaussianBlur(5));
        }

        target.getChildren().add(0, imageView);
    }

    public static void main(String[] args) {
        launch();
    }
}

final class GameLoopTimer extends AnimationTimer {

    private final GameController controller;
    private final GameView view;
    private final Render renderer;
    private final NextPane holdPane;
    private final NextPane nextPane;
    private final HudPane hudPane;
    private final Set<KeyCode> keys;
    private final SePlayer sePlayer;
    private final BiConsumer<Integer, Integer> gameOverHandler;

    // ポーズ状態
    private boolean isPaused = false;
    private boolean countingDown = false;
    private long countdownStartNanos = 0;
    private int lastShownCountdown = 4;

    // ポーズ用コールバック
    private final Runnable showPauseOverlay;
    private final Runnable hidePauseOverlay;
    private final Consumer<Integer> showCountdown;
    private final Runnable hideCountdown;

    private long lastFall = 0;
    private int lastWorldRotateStep = -1;
    private RotateTransition currentSpinAnim = null;
    private final List<Particle> particles = new ArrayList<>();
    private final Random rand = new Random();

    GameLoopTimer(
            GameController controller,
            GameView view,
            Render renderer,
            NextPane holdPane,
            NextPane nextPane,
            HudPane hudPane,
            Set<KeyCode> keys,
            SePlayer sePlayer,
            BiConsumer<Integer, Integer> gameOverHandler,
            Runnable showPauseOverlay,
            Runnable hidePauseOverlay,
            Consumer<Integer> showCountdown,
            Runnable hideCountdown) {
        this.controller = controller;
        this.view = view;
        this.renderer = renderer;
        this.holdPane = holdPane;
        this.nextPane = nextPane;
        this.hudPane = hudPane;
        this.keys = keys;
        this.sePlayer = sePlayer;
        this.gameOverHandler = gameOverHandler;
        this.showPauseOverlay = showPauseOverlay;
        this.hidePauseOverlay = hidePauseOverlay;
        this.showCountdown = showCountdown;
        this.hideCountdown = hideCountdown;
    }

    public boolean isGamePaused()  { return isPaused; }
    public boolean isCountingDown() { return countingDown; }

    public void togglePause() {
        if (controller.isTrueGameOver()) return;
        if (countingDown) {
            // カウントダウン中に再度ポーズ → ポーズに戻る
            countingDown = false;
            isPaused = true;
            hideCountdown.run();
            showPauseOverlay.run();
            return;
        }
        isPaused = !isPaused;
        if (isPaused) {
            showPauseOverlay.run();
        } else {
            hidePauseOverlay.run();
            startCountdown();
        }
    }

    private void startCountdown() {
        countingDown = true;
        countdownStartNanos = System.nanoTime();
        lastShownCountdown = 4;
    }

    @Override
    public void handle(long now) {
        if (isPaused) return;

        if (countingDown) {
            long elapsed = now - countdownStartNanos;
            int secondsLeft = 3 - (int) (elapsed / 1_000_000_000L);
            if (secondsLeft <= 0) {
                countingDown = false;
                keys.clear();
                hideCountdown.run();
            } else if (secondsLeft != lastShownCountdown) {
                lastShownCountdown = secondsLeft;
                showCountdown.accept(secondsLeft);
            }
            return;
        }

        if (controller.isTrueGameOver()) {
            stop();
            gameOverHandler.accept(controller.getScore(), controller.getLineCount());
            return;
        }

        int oldLines = controller.getLineCount();
        int oldScore = controller.getScore();

        controller.updateInput(keys, now);

        long currentFallInterval = Math.max(100_000_000L, 1_000_000_000L - (controller.getLevel() - 1) * 100_000_000L);
        if (now - lastFall > currentFallInterval) {
            controller.softDrop();
            lastFall = now;
        }

        int linesCleared = controller.getLineCount() - oldLines;
        int newScore = controller.getScore();
        if (newScore > oldScore) {
            hudPane.showScorePopup(newScore - oldScore);
        }

        for (SeEvent e : controller.drainEvents()) {
            sePlayer.play(e);
            if (e == SeEvent.WORLD_ROTATE) {
                triggerBoardSpinAnimation();
                view.getPlayFieldPane().triggerShake();
            }
            if (e == SeEvent.LINE_CLEAR) {
                spawnLineParticles();
                if (linesCleared >= 4) {
                    view.getPlayFieldPane().triggerShake();
                }
            }
            if (e == SeEvent.T_SPIN) {
                hudPane.showTSpinPopup(false);
            }
            if (e == SeEvent.T_SPIN_MINI) {
                hudPane.showTSpinPopup(true);
            }
            if (e == SeEvent.REN) {
                hudPane.showRenPopup(controller.getComboCount());
            }
            if (e == SeEvent.HARD_DROP) {
                spawnHardDropTrail();
            }
        }

        javafx.scene.canvas.GraphicsContext gc = view.getPlayFieldPane().getPlayfieldCanvas().getGraphicsContext2D();
        renderer.drawAll(gc, controller.getBoard(), controller.getCurrent(), controller.getGhost());

        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.update();
            if (p.isDead()) it.remove();
        }
        renderer.drawParticles(gc, particles);

        hudPane.updateScore(controller.getScore());
        hudPane.updateLines(controller.getLineCount());
        hudPane.updateLevel(controller.getLevel());

        renderer.drawNext(
                holdPane.getNextCanvas().getGraphicsContext2D(),
                controller.getHold(), 0, 0, !controller.canHold());

        renderer.drawNext(
                nextPane.getNextCanvas().getGraphicsContext2D(),
                controller.getNext(), 0, 0);

        hudPane.updateDialogue(controller.getScore(), controller.getLineCount());

        int worldRotateStep = controller.getWorldRotateStep();
        if (worldRotateStep != lastWorldRotateStep) {
            view.getCharacterPane().updateCharacterForWorldRotateStep(worldRotateStep);
            view.applyTheme(UiTheme.forStep(worldRotateStep));
            lastWorldRotateStep = worldRotateStep;
        }
    }

    private void triggerBoardSpinAnimation() {
        if (currentSpinAnim != null
                && currentSpinAnim.getStatus() == Animation.Status.RUNNING) {
            currentSpinAnim.stop();
            view.getPlayFieldPane().setRotate(0);
        }
        RotateTransition rt = new RotateTransition(
                Duration.millis(400), view.getPlayFieldPane());
        rt.setByAngle(360);
        rt.setInterpolator(Interpolator.EASE_BOTH);
        rt.setCycleCount(1);
        rt.play();
        currentSpinAnim = rt;
    }

    private void spawnLineParticles() {
        List<Integer> rows = new ArrayList<>();
        List<javafx.scene.paint.Color[]> colors = new ArrayList<>();
        controller.getBoard().pollLastClearedLines(rows, colors);

        view.getPlayFieldPane().triggerFlash(rows.size());

        int cs = renderer.getCellSize();

        for (int i = 0; i < rows.size(); i++) {
            int row = rows.get(i);
            javafx.scene.paint.Color[] lineColors = colors.get(i);
            double cy = row * cs + cs * 0.5;

            for (int col = 0; col < Board.COLS; col++) {
                javafx.scene.paint.Color color = lineColors[col];
                if (color == null) color = javafx.scene.paint.Color.WHITE;
                double cx = col * cs + cs * 0.5;

                for (int k = 0; k < 5; k++) {
                    double angle = rand.nextDouble() * 2 * Math.PI;
                    double speed = 1.5 + rand.nextDouble() * 4.0;
                    double vx = Math.cos(angle) * speed;
                    double vy = Math.sin(angle) * speed - 1.5;
                    int life = 30 + rand.nextInt(20);
                    particles.add(new Particle(cx, cy, vx, vy, color, life));
                }
            }
        }
    }

    private void spawnHardDropTrail() {
        List<int[]> trail = controller.drainHardDropTrail();
        javafx.scene.paint.Color trailColor = controller.getLastHardDropColor();
        if (trail.isEmpty() || trailColor == null) return;

        int cs = renderer.getCellSize();
        javafx.scene.paint.Color bright = trailColor.deriveColor(0, 0.5, 2.0, 0.85);

        for (int[] cell : trail) {
            double cx = cell[1] * cs + cs * 0.5;
            double cy = cell[0] * cs + cs * 0.5;
            particles.add(new Particle(cx, cy, 0, -0.3, bright, 10 + rand.nextInt(6)));
        }
    }
}
