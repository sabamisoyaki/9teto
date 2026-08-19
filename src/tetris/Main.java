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
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
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
import tetris.view.Backdrop;
import tetris.view.DialogueBank;
import tetris.view.DialogueTrigger;
import tetris.view.EndCreditPane;
import tetris.view.ConfigPane;
import tetris.view.FxParams;
import tetris.view.GameView;
import tetris.view.HudPane;
import tetris.view.ImageAssets;
import tetris.view.KowloonPalette;
import tetris.view.MenuStyle;
import tetris.view.NextPane;
import tetris.view.Particle;
import tetris.view.Render;
import tetris.view.UiSkin;
import tetris.view.UiSkinBank;

public class Main extends Application {

    private Stage primaryStage;
    private static final int WINDOW_WIDTH = 1920;
    private static final int WINDOW_HEIGHT = 1080;
    private static final Path BGM_PATH = ResourcePath.of("audio", "bgm.wav");
    private static final double BGM_MAX_VOLUME = 0.35;
    private MediaPlayer bgmPlayer;
    private Timeline bgmFade; // 実行中のフェードアウト。再生再開時に止めて二重制御を防ぐ
    private final GameConfig config = new GameConfig();

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
        // フェードアウト進行中にリトライした場合、走り続けるフェードの onFinished が
        // 直後に stop() を呼んで新ゲームのBGMを止めてしまうため、ここで打ち切る
        if (bgmFade != null) {
            bgmFade.stop();
            bgmFade = null;
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
        if (bgmFade != null) {
            bgmFade.stop();
        }
        double startVol = bgmPlayer.getVolume();
        Timeline fade = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(bgmPlayer.volumeProperty(), startVol)),
            new KeyFrame(Duration.seconds(durationSecs), new KeyValue(bgmPlayer.volumeProperty(), 0.0))
        );
        fade.setOnFinished(e -> {
            bgmFade = null;
            bgmPlayer.stop();
            bgmPlayer.seek(Duration.ZERO);
            if (onFinished != null) onFinished.run();
        });
        bgmFade = fade;
        fade.play();
    }

    // =====================================================
    //  シーン遷移演出
    // =====================================================
    private boolean transitioning = false;

    // シーンに紐づく無限ループのアニメーション。シーンを作り直しても JavaFX の
    // プライマリタイマーから強参照され続けて GC されないため、明示的に stop する。
    private final List<Animation> loopingAnimations = new ArrayList<>();

    /** 無限ループのアニメーションを登録して再生する（シーン切替時に一括停止される） */
    private void playLooping(Animation anim) {
        loopingAnimations.add(anim);
        anim.play();
    }

    /** 前シーンの無限ループアニメーションを全て停止する。新シーン構築の冒頭で呼ぶ */
    private void stopLoopingAnimations() {
        for (Animation a : loopingAnimations) {
            a.stop();
        }
        loopingAnimations.clear();
    }

    /** 現在のシーンを縮小＋フェードアウトさせてから次のシーンを表示する */
    private void transitionOut(Runnable showNextScene) {
        if (transitioning) return; // 連打による多重遷移を防ぐ
        Scene current = primaryStage.getScene();
        if (current == null) {
            showNextScene.run();
            return;
        }
        transitioning = true;
        Parent root = current.getRoot();

        ScaleTransition scale = new ScaleTransition(Duration.millis(500), root);
        scale.setToX(0.92);
        scale.setToY(0.92);
        FadeTransition fade = new FadeTransition(Duration.millis(500), root);
        fade.setToValue(0.0);

        ParallelTransition out = new ParallelTransition(scale, fade);
        out.setInterpolator(Interpolator.EASE_IN);
        out.setOnFinished(e -> {
            transitioning = false;
            showNextScene.run();
        });
        out.play();
    }

    /** 新しいシーンをフェードインで表示する */
    private void fadeInScene(Scene scene) {
        Parent root = scene.getRoot();
        root.setOpacity(0.0);
        primaryStage.setScene(scene);
        FadeTransition in = new FadeTransition(Duration.millis(350), root);
        in.setToValue(1.0);
        in.play();
    }

    // =====================================================
    //  スタート画面
    // =====================================================
    private Scene makeStartScene() {
        stopLoopingAnimations();
        StackPane root = new StackPane();
        Path bgPath = Files.exists(ImageAssets.START_BG) ? ImageAssets.START_BG : ImageAssets.BASE_LAYER;
        ImageAssets.addBackdropView(root, bgPath, WINDOW_WIDTH, WINDOW_HEIGHT, Backdrop.FAR);

        Rectangle overlay = new Rectangle(WINDOW_WIDTH, WINDOW_HEIGHT,
                KowloonPalette.alpha(KowloonPalette.SHADOW, 0.6));

        VBox content = new VBox(40);
        content.setAlignment(Pos.CENTER);

        Label title = new Label("TETRIS");
        // -fx-effect は setEffect(glow) と競合するためスタイル文字列には入れない
        title.setStyle("-fx-font-size: 100px; -fx-font-weight: bold;"
            + " -fx-text-fill: " + KowloonPalette.LIGHT_HEX + "; -fx-font-family: 'Courier New';");

        DropShadow glow = new DropShadow(30, KowloonPalette.NEON);
        glow.setSpread(0.25);
        title.setEffect(glow);

        // グロウの強弱を往復させて脈動させる（無限ループ）。
        // シーン切替時に stopLoopingAnimations() で停止されるよう playLooping で登録する。
        Timeline glowPulse = new Timeline(
            new KeyFrame(Duration.ZERO,         new KeyValue(glow.radiusProperty(), 18)),
            new KeyFrame(Duration.seconds(1.6), new KeyValue(glow.radiusProperty(), 40)));
        glowPulse.setCycleCount(Animation.INDEFINITE);
        glowPulse.setAutoReverse(true);
        playLooping(glowPulse);

        TranslateTransition titleTt = new TranslateTransition(Duration.seconds(1), title);
        titleTt.setFromY(-200);
        titleTt.setToY(0);
        titleTt.setInterpolator(Interpolator.EASE_OUT);
        FadeTransition titleFade = new FadeTransition(Duration.millis(600), title);
        titleFade.setFromValue(0.0);
        titleFade.setToValue(1.0);
        new ParallelTransition(titleTt, titleFade).play();

        Label sub = new Label("Press SPACE to Start");
        sub.setStyle(MenuStyle.prompt());

        FadeTransition fade = new FadeTransition(Duration.seconds(1.0), sub);
        fade.setFromValue(1.0);
        fade.setToValue(0.2);
        fade.setCycleCount(Animation.INDEFINITE);
        fade.setAutoReverse(true);
        playLooping(fade);

        Label configHint = new Label("C  Config");
        configHint.setStyle(MenuStyle.hint());

        int hs = config.getHighScore();
        Label highScoreLabel = new Label(hs > 0 ? "Best: " + hs : "");
        highScoreLabel.setStyle(MenuStyle.value(22, KowloonPalette.RUST_HEX));

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
        fadeInScene(makeStartScene());
    }

    // =====================================================
    //  コンフィグ画面
    // =====================================================
    private Scene makeConfigScene() {
        stopLoopingAnimations();
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
        stopLoopingAnimations();
        GameView view = new GameView();
        GameController controller = new GameController();
        int cellSize = Math.min(
                (int) (view.getPlayFieldPane().getPlayfieldCanvas().getWidth() / Board.COLS),
                (int) (view.getPlayFieldPane().getPlayfieldCanvas().getHeight() / Board.ROWS));
        Render renderer = new Render(
                cellSize,
                view.getPlayFieldPane().getPlayfieldCanvas().getWidth(),
                view.getPlayFieldPane().getPlayfieldCanvas().getHeight());
        // 枠線はセルサイズ確定後でないと盤面と揃わない（切り捨てぶん Canvas が余る）
        view.alignPlayFieldFrame(renderer);

        NextPane holdPane = view.getHoldPane();
        NextPane nextPane = view.getNextPane();
        HudPane hudPane = view.getHudPane();
        hudPane.setBestScore(config.getHighScore());

        // ポーズオーバーレイとカウントダウンラベル
        StackPane pauseOverlay = buildPauseOverlay();
        pauseOverlay.setVisible(false);

        Label countdownLabel = new Label();
        countdownLabel.setStyle(MenuStyle.title(160, KowloonPalette.LIGHT_HEX));
        countdownLabel.setVisible(false);

        StackPane gameRoot = new StackPane(view.getRoot(), pauseOverlay, countdownLabel);

        Set<KeyCode> keys = new HashSet<>();
        // 初回描画
        renderer.drawAll(
                view.getPlayFieldPane().getPlayfieldCanvas().getGraphicsContext2D(),
                controller.getBoard(),
                controller.getCurrent(),
                controller.getGhost());
        holdPane.draw(renderer, controller.getHold(), !controller.canHold());
        nextPane.draw(renderer, controller.getNext(), false);

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
                (finalScore, finalLines) -> transitionOut(() -> showEndCreditScene(
                        DEFAULT_END_CREDIT_JSON,
                        () -> showGameOverScene(finalScore, finalLines))),
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
            } else if (code == KeyCode.U) {
                // デバッグ: ボタン一発で UI を総入れ替え（配置＋スキンを同時に次へ）
                String layoutName = view.cycleLayout();
                String skinName = timer.debugCycleSkin();
                view.getPlayFieldPane().spawnScorePopup(
                        "UI: " + layoutName + " / " + skinName,
                        javafx.scene.paint.Color.LIGHTSKYBLUE);
            } else if (code == KeyCode.F2) {
                // デバッグ: UIスキン（見た目）だけをフリップ演出付きで次へ入れ替える
                String name = timer.debugCycleSkin();
                view.getPlayFieldPane().spawnScorePopup(
                        "SKIN: " + name, javafx.scene.paint.Color.LIGHTSKYBLUE);
            } else if (code == KeyCode.F3) {
                // デバッグ: UI配置（位置）だけを次の定義へ切り替える
                String name = view.cycleLayout();
                view.getPlayFieldPane().spawnScorePopup(
                        "LAYOUT: " + name, javafx.scene.paint.Color.LIGHTSKYBLUE);
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
        Rectangle bg = new Rectangle(WINDOW_WIDTH, WINDOW_HEIGHT,
                KowloonPalette.alpha(KowloonPalette.SHADOW, 0.85));

        VBox menu = new VBox(28);
        menu.setAlignment(Pos.CENTER);

        Label title = new Label("PAUSED");
        title.setStyle(MenuStyle.title(80, KowloonPalette.LIGHT_HEX));

        String hintStyle = MenuStyle.hint();
        Label h1 = new Label("SPACE / P  ·  Resume");
        Label h2 = new Label("R          ·  New Game");
        Label h3 = new Label("T          ·  Title");
        h1.setStyle(hintStyle);
        h2.setStyle(hintStyle);
        h3.setStyle(hintStyle);

        // ヒントは行頭を揃える。等幅で「キー・動作」の桁を合わせているので、
        // 1 行ずつ中央寄せすると桁が崩れて読みにくい
        VBox hints = new VBox(28, h1, h2, h3);
        hints.setAlignment(Pos.CENTER_LEFT);
        // 中身の幅に縮める。既定の maxWidth のままだと親いっぱいに広がって左端へ張り付く
        hints.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);

        menu.getChildren().addAll(title, hints);

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
        stopLoopingAnimations();
        StackPane root = new StackPane();
        Path bgPath = Files.exists(ImageAssets.GAME_OVER_BG) ? ImageAssets.GAME_OVER_BG : ImageAssets.BASE_LAYER;
        ImageAssets.addBackdropView(root, bgPath, WINDOW_WIDTH, WINDOW_HEIGHT, Backdrop.FAR);

        // ゲームオーバーは煤黒を錆へ寄せた暗幕。暗さは保ったままスタート画面と描き分ける
        Rectangle overlay = new Rectangle(WINDOW_WIDTH, WINDOW_HEIGHT,
                KowloonPalette.alpha(KowloonPalette.SHADOW.interpolate(KowloonPalette.RUST, 0.35), 0.7));

        VBox content = new VBox(40);
        content.setAlignment(Pos.CENTER);

        Label title = new Label("GAME OVER");
        title.setStyle(MenuStyle.title(100, KowloonPalette.NEON_HEX));

        VBox statsBox = new VBox(15);
        statsBox.setAlignment(Pos.CENTER);
        statsBox.setStyle(MenuStyle.box());
        statsBox.setMaxWidth(560);

        Label scoreLabel = new Label("Score:      " + score);
        scoreLabel.setStyle(MenuStyle.value(32, KowloonPalette.LIGHT_HEX));

        Label linesLabel = new Label("Lines:      " + lines);
        linesLabel.setStyle(MenuStyle.value(32, KowloonPalette.LIGHT_HEX));

        int hs = config.getHighScore();
        boolean isNewRecord = score == hs && score > 0;
        String hsText = "High Score: " + hs + (isNewRecord ? "  ★ NEW!" : "");
        Label highScoreLabel = new Label(hsText);
        highScoreLabel.setStyle(MenuStyle.value(32,
                isNewRecord ? KowloonPalette.NEON_HEX : KowloonPalette.RUST_HEX));

        statsBox.getChildren().addAll(scoreLabel, linesLabel, highScoreLabel);

        VBox hints = new VBox(10);
        hints.setAlignment(Pos.CENTER);

        Label retry = new Label("SPACE  ·  Retry");
        retry.setStyle(MenuStyle.prompt());
        Label titleHint = new Label("T      ·  Title");
        titleHint.setStyle(MenuStyle.hint());

        FadeTransition fade = new FadeTransition(Duration.seconds(0.8), retry);
        fade.setFromValue(1.0);
        fade.setToValue(0.3);
        fade.setCycleCount(Animation.INDEFINITE);
        fade.setAutoReverse(true);
        playLooping(fade);

        hints.getChildren().addAll(retry, titleHint);
        content.getChildren().addAll(title, statsBox, hints);
        root.getChildren().addAll(overlay, content);

        // GAME OVER → スコア → リトライ案内 の順に表示する
        title.setTranslateY(-60);
        title.setOpacity(0);
        TranslateTransition titleSlide = new TranslateTransition(Duration.millis(450), title);
        titleSlide.setToY(0);
        FadeTransition titleFade = new FadeTransition(Duration.millis(450), title);
        titleFade.setToValue(1.0);
        new ParallelTransition(titleSlide, titleFade).play();

        statsBox.setOpacity(0);
        FadeTransition statsFade = new FadeTransition(Duration.millis(400), statsBox);
        statsFade.setToValue(1.0);
        statsFade.setDelay(Duration.millis(300));
        statsFade.play();

        hints.setOpacity(0);
        FadeTransition hintsFade = new FadeTransition(Duration.millis(400), hints);
        hintsFade.setToValue(1.0);
        hintsFade.setDelay(Duration.millis(600));
        hintsFade.play();

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

        transitionOut(() -> fadeInScene(makeGameOverScene(score, lines)));
    }


    // =====================================================
    //  エンドクレジット画面
    // =====================================================
    private Scene makeEndCreditScene(String creditJson, Runnable onComplete) {
        stopLoopingAnimations();
        EndCreditPane creditPane = new EndCreditPane(creditJson, ImageAssets.END_CREDIT_BG);
        Scene scene = new Scene(creditPane.getRoot(), WINDOW_WIDTH, WINDOW_HEIGHT);

        Timeline timeline = creditPane.buildScrollAnimation();
        Runnable rawAction = onComplete != null ? onComplete : this::showStartScene;

        // スキップ連打や「自然終了直後のキー入力」で二重実行されないよう1回だけ通す
        boolean[] completed = {false};
        Runnable completeAction = () -> {
            if (completed[0]) return;
            completed[0] = true;
            rawAction.run();
        };

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
        fadeInScene(makeEndCreditScene(creditJson, onComplete));
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
    private long pauseStartNanos = 0;

    // ポーズ用コールバック
    private final Runnable showPauseOverlay;
    private final Runnable hidePauseOverlay;
    private final Consumer<Integer> showCountdown;
    private final Runnable hideCountdown;

    private long lastFall = 0;
    private final List<Particle> particles = new ArrayList<>();
    private final Random rand = new Random();

    // ワールド回転演出
    // 演出中はゲーム進行（入力・落下）を止めて、回転直後の理不尽な死を防ぐ
    private boolean effectFrozen = false;
    private long freezeStartNanos = 0;
    private long freezeUntilNanos = 0;
    // F2 デバッグ用: スキンだけを先送りして見た目を確認できる
    private int debugSkinShift = 0;

    // ライン消去フラッシュ（消去行だけを Canvas 上で白く光らせる）
    private final List<Integer> flashRows = new ArrayList<>();
    private long flashStartNanos = 0;
    private int flashStrength = 1;

    // レベルアップ検出（初期値 0 = 初期化フレームでポップアップを出さない）
    private int lastLevel = 0;

    // セリフ制御: 1フレーム内で最も優先度の高いトリガーだけを発話する
    private final DialogueBank dialogue = new DialogueBank();
    private long lastDialogueNanos = 0;
    private long lastClearNanos = 0;
    private boolean spokeGameStart = false;
    private int prevLinesUntilRotate = Integer.MAX_VALUE;
    private DialogueTrigger pendingTrigger = null;
    private int pendingPriority = -1;
    private boolean pendingForce = false;

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
            // 回転演出フリーズ中にポーズした場合はフリーズを破棄する
            // （ポーズ解除時の補正がフリーズ分も含めてカバーするため二重シフトを防ぐ）
            effectFrozen = false;
            pauseStartNanos = System.nanoTime();
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
                // ポーズ時間ぶんタイマーを補正して、解除直後の即落下・即ロックを防ぐ。
                // セリフ用タイムスタンプも補正しないと、長時間ポーズ後に IDLE が即発火する
                long pausedNanos = now - pauseStartNanos;
                controller.shiftTimersAfterPause(pausedNanos);
                shiftDialogueTimers(pausedNanos);
                lastFall = now;
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

        // ワールド回転演出中: ゲーム進行（入力・落下）を止めて描画だけ続ける
        if (effectFrozen) {
            if (now < freezeUntilNanos) {
                renderFrame(now);
                return;
            }
            effectFrozen = false;
            // フリーズ時間ぶんタイマーを補正して、解除直後の即落下・即ロックを防ぐ
            long frozenNanos = now - freezeStartNanos;
            controller.shiftTimersAfterPause(frozenNanos);
            shiftDialogueTimers(frozenNanos);
            lastFall = now;
        }

        if (!spokeGameStart) {
            spokeGameStart = true;
            lastClearNanos = now;
            proposeDialogue(DialogueTrigger.GAME_START, 110, true);
        }

        int oldLines = controller.getLineCount();
        int oldScore = controller.getScore();

        controller.updateInput(keys, now);

        if (now - lastFall > controller.getFallIntervalNanos()) {
            controller.softDrop(false); // 自然落下（加点なし）
            lastFall = now;
        }

        int linesCleared = controller.getLineCount() - oldLines;
        int newScore = controller.getScore();
        // ソフトドロップの +1 は毎秒約25回入るため、小さな加点ではポップアップを出さない。
        // ロック（+20）・ハードドロップ・ライン消去など意味のある加点だけ表示する。
        if (newScore - oldScore >= FxParams.MIN_SCORE_POPUP) {
            hudPane.showScorePopup(newScore - oldScore);
        }

        Set<SeEvent> events = controller.drainEvents();
        // 回転と同時のフレームでは、消去行・軌跡の座標が回転前の盤面基準で
        // 無効になっているため、行単位の演出（フラッシュ・パーティクル）は出さない
        boolean worldRotated = events.contains(SeEvent.WORLD_ROTATE);

        for (SeEvent e : events) {
            sePlayer.play(e);
            if (e == SeEvent.WORLD_ROTATE) {
                onWorldRotated(now);
            }
            if (e == SeEvent.LINE_CLEAR) {
                // pollLastClearedLines() は呼ぶと内部リストがクリアされるため
                // ここで一度だけ poll して各演出に共有する
                List<Integer> rows = new ArrayList<>();
                List<javafx.scene.paint.Color[]> colors = new ArrayList<>();
                controller.getBoard().pollLastClearedLines(rows, colors);
                if (!worldRotated) {
                    spawnLineParticles(rows, colors);
                    startLineFlash(rows, linesCleared, now);
                }
                spawnScorePopup(linesCleared);
                if (linesCleared >= 4 && !worldRotated) {
                    view.getPlayFieldPane().triggerShake();
                }
                lastClearNanos = now;
                if (linesCleared >= 4) {
                    proposeDialogue(DialogueTrigger.TETRIS, 90, true);
                } else if (linesCleared >= 2) {
                    proposeDialogue(DialogueTrigger.DOUBLE_TRIPLE, 70, false);
                } else if (linesCleared == 1) {
                    proposeDialogue(DialogueTrigger.SINGLE, 60, false);
                }
            }
            if (e == SeEvent.TEMP_GAME_OVER) {
                int streak = controller.getGameOverStreak();
                int max = controller.getMaxGameOverStreak();
                view.getPlayFieldPane().spawnScorePopup(
                        "PINCH! " + streak + "/" + max, javafx.scene.paint.Color.ORANGERED);
                view.getPlayFieldPane().triggerDangerFlash();
                proposeDialogue(streak >= max - 1
                        ? DialogueTrigger.PINCH_LAST
                        : DialogueTrigger.PINCH, 100, true);
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
                spawnHardDropTrail(worldRotated);
            }
        }

        renderFrame(now);

        // レベルアップ検出（初期化フレームは lastLevel == 0 のため除外）
        int level = controller.getLevel();
        if (level != lastLevel) {
            if (lastLevel > 0) {
                view.getPlayFieldPane().spawnScorePopup(
                        "LEVEL " + level + "!", javafx.scene.paint.Color.LIGHTGREEN);
                proposeDialogue(DialogueTrigger.LEVEL_UP, 50, false);
            }
            lastLevel = level;
        }

        // 回転まで残り1ラインになった瞬間に予告セリフ
        int linesUntilRotate = controller.getLinesUntilRotate();
        if (linesUntilRotate == 1 && prevLinesUntilRotate != 1) {
            proposeDialogue(DialogueTrigger.ROTATE_SOON, 40, false);
        }
        prevLinesUntilRotate = linesUntilRotate;

        // 20秒間ライン消去が無ければ IDLE セリフ（発火後はタイムスタンプをリセット）
        if (now - lastClearNanos > FxParams.IDLE_THRESHOLD_NANOS) {
            proposeDialogue(DialogueTrigger.IDLE, 10, false);
            lastClearNanos = now;
        }

        flushDialogue(now);
    }

    /** 盤面・パーティクル・HUD・プレビューの描画。演出フリーズ中も毎フレーム呼ばれる */
    private void renderFrame(long now) {
        javafx.scene.canvas.GraphicsContext gc = view.getPlayFieldPane().getPlayfieldCanvas().getGraphicsContext2D();
        renderer.drawAll(gc, controller.getBoard(), controller.getCurrent(), controller.getGhost());
        drawLineFlash(gc, now);

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
        hudPane.updateRotateCountdown(
                controller.getLinesUntilRotate(), controller.getLineRotateInterval());
        hudPane.updateDangerGauge(
                controller.getGameOverStreak(), controller.getMaxGameOverStreak());

        holdPane.draw(renderer, controller.getHold(), !controller.canHold());
        nextPane.draw(renderer, controller.getNext(), false);
    }

    /**
     * このフレームで発話したいセリフを登録する。
     * 同一フレームに複数のトリガーが立った場合は優先度が最も高いものだけ残す。
     */
    private void proposeDialogue(DialogueTrigger trigger, int priority, boolean force) {
        if (priority > pendingPriority) {
            pendingTrigger = trigger;
            pendingPriority = priority;
            pendingForce = force;
        } else if (priority == pendingPriority) {
            pendingForce |= force;
        }
    }

    /** フレーム末尾で1回だけ呼ぶ。クールダウン中は force 指定のセリフのみ通す */
    private void flushDialogue(long now) {
        if (pendingTrigger == null) return;
        if (pendingForce || now - lastDialogueNanos >= FxParams.DIALOGUE_COOLDOWN_NANOS) {
            hudPane.showDialogue(dialogue.pick(pendingTrigger));
            lastDialogueNanos = now;
        }
        pendingTrigger = null;
        pendingPriority = -1;
        pendingForce = false;
    }

    /**
     * ポーズ／演出フリーズ解除時に、セリフ系の System.nanoTime 基準タイムスタンプを
     * 停止時間ぶんずらす。これが無いと長時間ポーズ後に IDLE セリフが即発火する。
     */
    private void shiftDialogueTimers(long pausedNanos) {
        lastClearNanos += pausedNanos;
        lastDialogueNanos += pausedNanos;
    }

    /**
     * ワールド回転の演出をまとめて発火する唯一の入口。
     * - 盤面: 回転前のスナップショットを 90°CW に回しながら新盤面へクロスフェード
     * - 周辺UI: 次のフロアのスキンへ即時入れ替え（演出は挟まない）
     * - ゲーム進行: EFFECT_FREEZE_NANOS の間フリーズ
     *
     * かつてはここに「UIパネルのフリップ入れ替え」と「キャラの寄り演出」も乗っていたが、
     *   - 寄り演出が画面中央＝盤面の真上を一瞬完全に覆う
     *   - フリップも寄りも、構図ラフの言う「回転で構図が遷移する」とは別物
     * という理由で外した。戻すなら git 履歴（proto/character-overlay）を参照。
     * 盤面のクロスフェードだけは「積みが 90°回った」ことを見せる役があるので残す。
     */
    private void onWorldRotated(long now) {
        UiSkin skin = UiSkinBank.forStep(
                controller.getWorldRotateStep() + debugSkinShift);
        view.getPlayFieldPane().playWorldRotateTransition(skin);
        view.applySkin(skin);
        view.getPlayFieldPane().triggerShake();
        proposeDialogue(DialogueTrigger.WORLD_ROTATE, 80, true);

        effectFrozen = true;
        freezeStartNanos = now;
        freezeUntilNanos = now + FxParams.EFFECT_FREEZE_NANOS;
    }

    /**
     * F2 デバッグ用: 盤面を回さずスキンだけを次へ入れ替える。
     * シフト量は以降の本物のワールド回転にも引き継がれる。
     *
     * @return 適用したスキン名
     */
    public String debugCycleSkin() {
        debugSkinShift = (debugSkinShift + 1) % UiSkinBank.skinCount();
        UiSkin skin = UiSkinBank.forStep(
                controller.getWorldRotateStep() + debugSkinShift);
        view.applySkin(skin);
        return skin.name;
    }

    private void spawnLineParticles(List<Integer> rows, List<javafx.scene.paint.Color[]> colors) {
        int cs = renderer.getCellSize();

        for (int i = 0; i < rows.size(); i++) {
            int row = rows.get(i);
            javafx.scene.paint.Color[] lineColors = colors.get(i);
            double cy = renderer.toPixelY(row) + cs * 0.5;

            for (int col = 0; col < Board.COLS; col++) {
                javafx.scene.paint.Color color = lineColors[col];
                if (color == null) color = javafx.scene.paint.Color.WHITE;
                double cx = renderer.toPixelX(col) + cs * 0.5;

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

    private void startLineFlash(List<Integer> rows, int cleared, long now) {
        flashRows.clear();
        flashRows.addAll(rows);
        flashStartNanos = now;
        flashStrength = Math.max(1, Math.min(4, cleared));
    }

    private void drawLineFlash(javafx.scene.canvas.GraphicsContext gc, long now) {
        if (flashRows.isEmpty()) return;
        double t = (now - flashStartNanos) / (double) FxParams.LINE_FLASH_DURATION_NANOS;
        if (t >= 1.0) {
            flashRows.clear();
            return;
        }

        // 消去ライン数で強度を変える: 1ライン=0.5, 4ライン=0.95
        double peak = FxParams.LINE_FLASH_BASE + FxParams.LINE_FLASH_PER_LINE * flashStrength;
        double alpha = peak * (1.0 - t);

        // 白ではなく蛍光灯色で光らせる（パレット外の色を画面に出さない）
        gc.setFill(KowloonPalette.alpha(KowloonPalette.LIGHT, alpha));
        int cs = renderer.getCellSize();
        for (int row : flashRows) {
            gc.fillRect(renderer.toPixelX(0), renderer.toPixelY(row),
                    (double) cs * Board.COLS, cs);
        }
    }

    private void spawnScorePopup(int cleared) {
        String text = switch (cleared) {
            case 1 -> "+100";
            case 2 -> "+300  DOUBLE!";
            case 3 -> "+500  TRIPLE!";
            case 4 -> "+800  TETRIS!!";
            default -> "+0";
        };
        javafx.scene.paint.Color color = cleared >= 4 ? javafx.scene.paint.Color.GOLD
                : cleared >= 2 ? javafx.scene.paint.Color.LIGHTSKYBLUE
                : javafx.scene.paint.Color.WHITE;
        view.getPlayFieldPane().spawnScorePopup(text, color);
    }

    private void spawnHardDropTrail(boolean suppressed) {
        // 軌跡データは必ずドレインする（残留防止）。回転と同時のフレームでは
        // 座標が旧盤面基準のため描画しない
        List<int[]> trail = controller.drainHardDropTrail();
        javafx.scene.paint.Color trailColor = controller.getLastHardDropColor();
        if (suppressed || trail.isEmpty() || trailColor == null) return;

        int cs = renderer.getCellSize();
        javafx.scene.paint.Color bright = trailColor.deriveColor(0, 0.5, 2.0, 0.85);

        for (int[] cell : trail) {
            double cx = renderer.toPixelX(cell[1]) + cs * 0.5;
            double cy = renderer.toPixelY(cell[0]) + cs * 0.5;
            particles.add(new Particle(cx, cy, 0, -0.3, bright, 10 + rand.nextInt(6)));
        }
    }
}
