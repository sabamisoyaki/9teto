package tetris;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
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
import javafx.scene.canvas.Canvas;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import tetris.SePlayer;
import tetris.controller.GameController;
import tetris.model.Board;
import tetris.model.GameConfig;
import tetris.model.SeEvent;
import tetris.view.ConfigPane;
import tetris.view.EndCreditPane;
import tetris.view.GameView;
import tetris.view.HudPane;
import tetris.view.NextPane;
import tetris.view.Render;

public class Main extends Application {

    private Stage primaryStage;
    private static final int WINDOW_WIDTH = 1920;
    private static final int WINDOW_HEIGHT = 1080;
    private static final Path BGM_PATH = ResourcePath.of("audio", "bgm.wav");
    private static final long AUTO_FALL_INTERVAL_NANOS = 300_000_000L;
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

    private void stopBgm() {
        if (bgmPlayer == null) {
            return;
        }
        bgmPlayer.stop();
        bgmPlayer.seek(Duration.ZERO);
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

        content.getChildren().addAll(title, sub, configHint);
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
        stopBgm();
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
        NextPane nextPane = view.getNextPane();
        HudPane hudPane = view.getHudPane();

        Scene scene = new Scene(view.getRoot(), WINDOW_WIDTH, WINDOW_HEIGHT);

        // キー入力管理
        Set<KeyCode> keys = new HashSet<>();
        scene.setOnKeyPressed(e -> {
            keys.add(e.getCode());
        });
        scene.setOnKeyReleased(e -> keys.remove(e.getCode()));

        // 初回描画
        renderer.drawAll(
                view.getPlayFieldPane().getPlayfieldCanvas().getGraphicsContext2D(),
                controller.getBoard(),
                controller.getCurrent(),
                controller.getGhost());
        renderer.drawNext(
                nextPane.getNextCanvas().getGraphicsContext2D(),
                controller.getNext(),
                0,
                0);

        SePlayer sePlayer = new SePlayer(config);

        AnimationTimer timer = new GameLoopTimer(
                controller,
                view,
                renderer,
                nextPane,
                hudPane,
                keys,
                sePlayer,
                AUTO_FALL_INTERVAL_NANOS,
                (finalScore, finalLines) -> showEndCreditScene(
                        DEFAULT_END_CREDIT_JSON,
                        () -> showGameOverScene(finalScore, finalLines)));

        timer.start();
        return scene;
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

        VBox content = new VBox(50);
        content.setAlignment(Pos.CENTER);

        Label title = new Label("GAME OVER");
        title.setStyle("-fx-font-size: 100px; -fx-font-weight: bold; -fx-text-fill: #ff8888; -fx-font-family: 'Courier New'; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 10, 0.0, 2, 2);");

        VBox statsBox = new VBox(15);
        statsBox.setAlignment(Pos.CENTER);
        statsBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6); -fx-padding: 30px 60px; -fx-border-color: #557788; -fx-border-width: 1px; -fx-border-radius: 5px; -fx-background-radius: 5px;");
        statsBox.setMaxWidth(500);

        Label scoreLabel = new Label("Score: " + score);
        scoreLabel.setStyle("-fx-font-size: 32px; -fx-text-fill: white; -fx-font-family: 'Courier New';");

        Label linesLabel = new Label("Lines: " + lines);
        linesLabel.setStyle("-fx-font-size: 32px; -fx-text-fill: white; -fx-font-family: 'Courier New';");

        statsBox.getChildren().addAll(scoreLabel, linesLabel);

        Label retry = new Label("Press SPACE to Retry");
        retry.setStyle("-fx-font-size: 24px; -fx-text-fill: #aaddff; -fx-font-family: 'Courier New'; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 5, 0.0, 1, 1);");

        FadeTransition fade = new FadeTransition(Duration.seconds(0.8), retry);
        fade.setFromValue(1.0);
        fade.setToValue(0.3);
        fade.setCycleCount(Animation.INDEFINITE);
        fade.setAutoReverse(true);
        fade.play();

        content.getChildren().addAll(title, statsBox, retry);
        root.getChildren().addAll(overlay, content);

        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);

        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.SPACE) {
                showGameScene();
            }
        });

        return scene;
    }

    private void showGameOverScene(int score, int lines) {
        stopBgm();
        primaryStage.setScene(makeGameOverScene(score, lines));
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
    private final NextPane nextPane;
    private final HudPane hudPane;
    private final Set<KeyCode> keys;
    private final SePlayer sePlayer;
    private final long autoFallIntervalNanos;
    private final BiConsumer<Integer, Integer> gameOverHandler;

    private long lastFall = 0;
    private int lastWorldRotateStep = -1;
    private boolean isAnimating = false;

    GameLoopTimer(
            GameController controller,
            GameView view,
            Render renderer,
            NextPane nextPane,
            HudPane hudPane,
            Set<KeyCode> keys,
            SePlayer sePlayer,
            long autoFallIntervalNanos,
            BiConsumer<Integer, Integer> gameOverHandler) {
        this.controller = controller;
        this.view = view;
        this.renderer = renderer;
        this.nextPane = nextPane;
        this.hudPane = hudPane;
        this.keys = keys;
        this.sePlayer = sePlayer;
        this.autoFallIntervalNanos = autoFallIntervalNanos;
        this.gameOverHandler = gameOverHandler;
    }

    @Override
    public void handle(long now) {
        if (controller.isTrueGameOver()) {
            stop();
            gameOverHandler.accept(controller.getScore(), controller.getLineCount());
            return;
        }

        controller.updateInput(keys, now);

        for (SeEvent e : controller.drainEvents()) {
            sePlayer.play(e);
        }

        if (now - lastFall > autoFallIntervalNanos) {
            controller.softDrop();
            lastFall = now;
        }

        renderer.drawAll(
                view.getPlayFieldPane().getPlayfieldCanvas().getGraphicsContext2D(),
                controller.getBoard(),
                controller.getCurrent(),
                controller.getGhost());

        int score = controller.getScore();
        int lines = controller.getLineCount();
        hudPane.updateScore(score);
        hudPane.updateLines(lines);

        renderer.drawNext(
                nextPane.getNextCanvas().getGraphicsContext2D(),
                controller.getNext(),
                0,
                0);

        hudPane.updateDialogue(score, lines);

        int worldRotateStep = controller.getWorldRotateStep();
        if (worldRotateStep != lastWorldRotateStep) {
            view.getCharacterPane().updateCharacterForWorldRotateStep(worldRotateStep);
            lastWorldRotateStep = worldRotateStep;
            if (!isAnimating) {
                startRotationAnimation();
            }
        }
    }

    private void startRotationAnimation() {
        isAnimating = true;
        Canvas canvas = view.getPlayFieldPane().getPlayfieldCanvas();

        // 盤面データは既に90°回転済み — canvas を -90° から 0° へ戻すことで
        // 「世界がスピンして新しい向きに落ち着く」ように見せる
        canvas.setRotate(-90);

        RotateTransition rotate = new RotateTransition(Duration.millis(420), canvas);
        rotate.setFromAngle(-90);
        rotate.setToAngle(0);
        rotate.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition scale = new ScaleTransition(Duration.millis(420), canvas);
        scale.setFromX(0.88);
        scale.setFromY(0.88);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition anim = new ParallelTransition(rotate, scale);
        anim.setOnFinished(e -> {
            canvas.setRotate(0);
            canvas.setScaleX(1.0);
            canvas.setScaleY(1.0);
            isAnimating = false;
        });
        anim.play();
    }
}
