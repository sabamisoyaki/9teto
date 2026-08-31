package tetris;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
import tetris.model.Rng;
import tetris.model.RngHub;
import tetris.model.Scenario;
import tetris.model.ScenarioRoute;
import tetris.model.SeEvent;
import tetris.model.SevenBagRandomizer;
import tetris.view.AdventurePane;
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
import tetris.view.RecollectionPane;
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
    /** メニュー系の効果音。ゲーム画面のものとは別に、初めて要るときだけ作る */
    private SePlayer menuSePlayer;
    private Timeline bgmFade; // 実行中のフェードアウト。再生再開時に止めて二重制御を防ぐ
    private final GameConfig config = new GameConfig();
    /** シーンをまたいだ OS のキーリピートを、最初の KEY_RELEASED まで遮断する。 */
    private final Set<KeyCode> heldInputKeys = new HashSet<>();
    /** ゲーム中に物理的に押されている操作キー。フォーカス喪失時にも必ず解除する。 */
    private final Set<KeyCode> gameInputKeys = new HashSet<>();

    /**
     * 撮影モード（-Dshot.out 指定時）。演出を再生せず最終状態で描くことで、
     * 何度撮っても同じ絵になるようにする。ShotRunner から参照する。
     */
    static boolean shotMode = false;

    /** 撮影モードの乱数シード。固定なので何度撮っても同じ絵になる */
    private static final long SHOT_SEED = 20260821L;

    /**
     * このプレイの乱数の元。makeGameScene() で引き直すので、リトライすると
     * 別のミノ順になる（-Dgame.seed 指定時を除く）。
     */
    private RngHub rngHub;

    static final String DEFAULT_END_CREDIT_JSON = """
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
        // 保険。シーン構築中やイベントハンドラ内のエラーを拾う。
        // ゲームループ内のエラーは GameLoopTimer.handle() 側で受ける
        Thread.setDefaultUncaughtExceptionHandler(
                (thread, e) -> ErrorDump.write(e, rngHub));

        this.primaryStage = stage;
        stage.setTitle("TETRIS");
        stage.setResizable(false);
        // フォーカスを失うと KEY_RELEASED が届かないことがあるため、押下状態を捨てる。
        stage.focusedProperty().addListener((obs, wasFocused, focused) -> {
            if (!focused) {
                heldInputKeys.clear();
                gameInputKeys.clear();
            }
        });
        config.load();

        // 撮影モード: 各画面を順に組んで PNG へ落としたら終了する（shot.bat から使う）。
        // BGM は初期化しない。音が鳴るうえ MediaPlayer のスレッドが終了を遅らせる
        String shotOut = System.getProperty("shot.out");
        if (shotOut != null && !shotOut.isBlank()) {
            shotMode = true;
            stage.show();
            new ShotRunner(this, stage, Path.of(shotOut.trim())).run();
            return;
        }

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

    /**
     * 1 回の物理押下につきハンドラを 1 回だけ呼ぶ。
     * 押しっぱなしでシーンが切り替わっても、解放までは次シーンへ入力を渡さない。
     */
    private void setDebouncedKeyHandler(Scene scene, Consumer<KeyCode> handler) {
        scene.setOnKeyPressed(e -> {
            KeyCode code = e.getCode();
            if (acceptKeyPress(code)) {
                handler.accept(code);
            }
        });
        scene.setOnKeyReleased(e -> heldInputKeys.remove(e.getCode()));
    }

    private boolean acceptKeyPress(KeyCode code) {
        boolean firstPress = heldInputKeys.add(code);
        // カーソル移動は長押しリピートを残す。画面遷移・送り・ポーズに使うキーだけ、
        // 解放まで再入力を遮断する。
        return firstPress || switch (code) {
            case SPACE, ENTER, ESCAPE, RIGHT, C, R, T, P -> false;
            default -> true;
        };
    }

    /** 無限ループのアニメーションを登録して再生する（シーン切替時に一括停止される） */
    private void playLooping(Animation anim) {
        // 撮影モードでは再生しない。脈動や点滅は撮るたびに位相が変わってしまう
        if (shotMode) return;
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
        if (shotMode) { // 撮影モードは演出を挟まず最終状態で差し替える
            root.setOpacity(1.0);
            primaryStage.setScene(scene);
            return;
        }
        root.setOpacity(0.0);
        primaryStage.setScene(scene);
        FadeTransition in = new FadeTransition(Duration.millis(350), root);
        in.setToValue(1.0);
        in.play();
    }

    // =====================================================
    //  スタート画面
    // =====================================================
    Scene makeStartScene() {
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
        // 撮影モードでは再生しない。未再生なら translateY=0 / opacity=1 の最終状態のまま
        if (!shotMode) {
            new ParallelTransition(titleTt, titleFade).play();
        }

        Label sub = new Label("Press SPACE to Start");
        sub.setStyle(MenuStyle.prompt());

        FadeTransition fade = new FadeTransition(Duration.seconds(1.0), sub);
        fade.setFromValue(1.0);
        fade.setToValue(0.2);
        fade.setCycleCount(Animation.INDEFINITE);
        fade.setAutoReverse(true);
        playLooping(fade);

        Label configHint = new Label("C  Config     T  Tutorial     R  Recollection");
        configHint.setStyle(MenuStyle.hint());

        int hs = config.getHighScore();
        Label highScoreLabel = new Label(hs > 0 ? "Best: " + hs : "");
        highScoreLabel.setStyle(MenuStyle.value(22, KowloonPalette.RUST_HEX));

        content.getChildren().addAll(title, sub, configHint, highScoreLabel);
        root.getChildren().addAll(overlay, content);

        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);

        setDebouncedKeyHandler(scene, code -> {
            if (code == KeyCode.SPACE) {
                // タイトル → OP → チュートリアル（初回のみ）→ ゲーム。
                // 自己ベストを渡して「初見か・やり込んでいるか」で掴みを変える。
                // showGameScene() 側には入れない（リトライやポーズ R でも出てしまう）
                showAdventureScene(Scenario.OPENING, config.getHighScore(),
                        () -> playTutorial(false, this::showGameScene));
            } else if (code == KeyCode.C) {
                showConfigScene();
            } else if (code == KeyCode.R) {
                showRecollectionScene();
            } else if (code == KeyCode.T) {
                // 読み返しに来ているので、既読でも必ず通す（force = true）
                playTutorial(true, this::showStartScene);
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
    Scene makeConfigScene() {
        stopLoopingAnimations();
        ConfigPane pane = new ConfigPane(
            config,
            v -> { if (bgmPlayer != null) bgmPlayer.setVolume(v * BGM_MAX_VOLUME); },
            v -> { /* SE未実装 */ },
            b -> { if (bgmPlayer != null) bgmPlayer.setVolume(b ? config.getBgmVolume() * BGM_MAX_VOLUME : 0.0); },
            b -> { /* SE未実装 */ }
        );

        Scene scene = new Scene(pane.getRoot(), WINDOW_WIDTH, WINDOW_HEIGHT);
        setDebouncedKeyHandler(scene, code -> {
            if (code == KeyCode.ESCAPE) {
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
    /**
     * ゲーム画面の骨格。本編と撮影モード（ShotRunner）で組み方を共有するために束ねる。
     * ここに無いもの（GameController / GameLoopTimer / キー入力）は本編だけが持つ。
     */
    static final class GameSceneParts {
        final GameView view;
        final Render renderer;
        final StackPane pauseOverlay;
        final Label countdownLabel;
        final StackPane gameRoot;

        GameSceneParts(GameView view, Render renderer, StackPane pauseOverlay,
                       Label countdownLabel, StackPane gameRoot) {
            this.view = view;
            this.renderer = renderer;
            this.pauseOverlay = pauseOverlay;
            this.countdownLabel = countdownLabel;
            this.gameRoot = gameRoot;
        }
    }

    /** GameView・Render・ポーズ層までを組む（ゲーム進行は含まない） */
    GameSceneParts buildGameSceneParts() {
        GameView view = new GameView();
        int cellSize = Math.min(
                (int) (view.getPlayFieldPane().getPlayfieldCanvas().getWidth() / Board.COLS),
                (int) (view.getPlayFieldPane().getPlayfieldCanvas().getHeight() / Board.ROWS));
        Render renderer = new Render(
                cellSize,
                view.getPlayFieldPane().getPlayfieldCanvas().getWidth(),
                view.getPlayFieldPane().getPlayfieldCanvas().getHeight());
        // 枠線はセルサイズ確定後でないと盤面と揃わない（切り捨てぶん Canvas が余る）
        view.alignPlayFieldFrame(renderer);

        // ポーズオーバーレイとカウントダウンラベル
        StackPane pauseOverlay = buildPauseOverlay();
        pauseOverlay.setVisible(false);

        Label countdownLabel = new Label();
        countdownLabel.setStyle(MenuStyle.title(160, KowloonPalette.LIGHT_HEX));
        countdownLabel.setVisible(false);

        StackPane gameRoot = new StackPane(view.getRoot(), pauseOverlay, countdownLabel);
        return new GameSceneParts(view, renderer, pauseOverlay, countdownLabel, gameRoot);
    }

    private Scene makeGameScene() {
        stopLoopingAnimations();
        GameSceneParts parts = buildGameSceneParts();
        GameView view = parts.view;
        Render renderer = parts.renderer;
        StackPane pauseOverlay = parts.pauseOverlay;
        Label countdownLabel = parts.countdownLabel;
        StackPane gameRoot = parts.gameRoot;

        rngHub = shotMode
                ? RngHub.of(SHOT_SEED)
                : RngHub.fromSystemProperty();
        System.out.println("[Rng] seed=" + rngHub.seed());

        GameController controller = new GameController(
                new SevenBagRandomizer(rngHub.stream("piece")));
        NextPane holdPane = view.getHoldPane();
        NextPane nextPane = view.getNextPane();
        HudPane hudPane = view.getHudPane();
        hudPane.setBestScore(config.getHighScore());

        // 前のゲームで KEY_RELEASED を受け取れなかった場合も、新しいゲームへ持ち越さない。
        gameInputKeys.clear();
        Set<KeyCode> keys = gameInputKeys;
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
                rngHub,
                controller,
                view,
                renderer,
                holdPane,
                nextPane,
                hudPane,
                keys,
                sePlayer,
                // アドベンチャーパート → エンドクレジット → ゲームオーバー画面 の順。
                // 話を見せてからスタッフロールへ流す
                (finalScore, finalLines) -> {
                    // 読み物の途中で終了されても今回の記録を失わないよう、
                    // ゲームオーバーが確定した時点で先に保存する。
                    config.updateHighScore(finalScore);
                    config.save();
                    transitionOut(() -> showAdventureScene(
                            Scenario.ENDING, finalScore,
                            () -> showEndCreditScene(DEFAULT_END_CREDIT_JSON,
                                    () -> showGameOverScene(finalScore, finalLines))));
                },
                () -> pauseOverlay.setVisible(true),
                () -> pauseOverlay.setVisible(false),
                showCountdown,
                () -> countdownLabel.setVisible(false));

        Scene scene = new Scene(gameRoot, WINDOW_WIDTH, WINDOW_HEIGHT);

        scene.setOnKeyReleased(e -> {
            keys.remove(e.getCode());
            heldInputKeys.remove(e.getCode());
        });
        scene.setOnKeyPressed(e -> {
            KeyCode code = e.getCode();
            if (!acceptKeyPress(code)) return;
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
            } else if (code == KeyCode.F4) {
                // デバッグ: ワールド回転を1回起こす。幕間は回転4回ごとなので、
                // これが無いと 12 ライン消さないと幕間の確認ができない
                controller.rotateWorldClockwise();
                view.getPlayFieldPane().spawnScorePopup(
                        "WORLD ROTATE", javafx.scene.paint.Color.LIGHTSKYBLUE);
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
    Scene makeGameOverScene(int score, int lines) {
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

        // GAME OVER → スコア → リトライ案内 の順に表示する。
        // 撮影モードは段階表示を飛ばし、出揃った最終状態を撮る
        if (shotMode) {
            title.setTranslateY(0);
            title.setOpacity(1.0);
            statsBox.setOpacity(1.0);
            hints.setOpacity(1.0);
        } else {
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
        }

        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        setDebouncedKeyHandler(scene, code -> {
            if (code == KeyCode.SPACE) {
                showGameScene();
            } else if (code == KeyCode.T) {
                showStartScene();
            }
        });

        return scene;
    }

    private void showGameOverScene(int score, int lines) {
        fadeOutBgm(1.2, null);

        transitionOut(() -> fadeInScene(makeGameOverScene(score, lines)));
    }


    // =====================================================
    //  エンドクレジット画面
    // =====================================================
    Scene makeEndCreditScene(String creditJson, Runnable onComplete) {
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
        if (shotMode) {
            // 撮影モードは流さない。流し始めの位置は画面外なので、全体が収まる位置で止める
            creditPane.centerForStill();
        } else {
            timeline.play();
        }

        setDebouncedKeyHandler(scene, code -> {
            if (code == KeyCode.ESCAPE || code == KeyCode.SPACE) {
                timeline.stop();
                completeAction.run();
            }
        });

        return scene;
    }

    private void showEndCreditScene(String creditJson, Runnable onComplete) {
        fadeInScene(makeEndCreditScene(creditJson, onComplete));
    }

    // =====================================================
    //  アドベンチャーパート
    // =====================================================

    /**
     * アドベンチャーパートを挟んでから onComplete へ進む。
     *
     * <p>シナリオが無い・ルートが無い・撮影モードのときは<b>黙って素通り</b>する。
     * アセットが無ければ静かにフォールバックする既存方針（ASSETS.md）に合わせ、
     * シナリオが未整備でもゲームは通しで遊べる。
     *
     * @param part  {@link Scenario#OPENING} か {@link Scenario#ENDING}
     * @param score ルート選択に使う点数。OP は自己ベスト、ED は今回の得点
     */
    private void showAdventureScene(String part, int score, Runnable onComplete) {
        ScenarioRoute route = Scenario.load().routeFor(part, score);
        if (shotMode || route == null || route.isEmpty()) {
            onComplete.run();
            return;
        }
        // 「踏んだ」のはルートに入った時点。読み終わりで記録すると、
        // スキップした人が回想に追加できず「飛ばしたら二度と読めない」になる
        if (Scenario.ENDING.equals(part)) {
            config.markEndingSeen(route.id());
        }
        playAdventureRoute(route, onComplete);
    }

    /**
     * ルートを直接再生する。回想モードは既読のものをここから開く
     * （{@link #showAdventureScene} と違い minScore の判定を通さない）。
     */
    private void playAdventureRoute(ScenarioRoute route, Runnable onComplete) {
        // OP は無音のまま始まってしまうのでここで鳴らす。ED は既に鳴っているので
        // 何も起きない（playBgm は再生中なら触らない）
        playBgm();
        fadeInScene(makeAdventureScene(route, onComplete));
    }

    Scene makeAdventureScene(ScenarioRoute route, Runnable onComplete) {
        stopLoopingAnimations();
        AdventurePane pane = new AdventurePane(route);
        Scene scene = new Scene(pane.getRoot(), WINDOW_WIDTH, WINDOW_HEIGHT);

        // 送り切りとスキップで二重に進まないよう1回だけ通す（クレジットと同じ作り）
        boolean[] completed = {false};
        Runnable complete = () -> {
            if (completed[0]) return;
            completed[0] = true;
            onComplete.run();
        };

        pane.advance(); // 1 ページ目

        setDebouncedKeyHandler(scene, code -> {
            if (code == KeyCode.ESCAPE) {
                complete.run();
            } else if (code == KeyCode.SPACE || code == KeyCode.ENTER
                    || code == KeyCode.RIGHT) {
                if (pane.advance()) {
                    menuSe().play(SeEvent.MOVE);
                } else {
                    complete.run();
                }
            }
        });

        return scene;
    }

    /**
     * 幕間を挟む。ゲームのシーンは作り直さず、読み終わったら同じものへ戻す
     * （盤面も積みもそのまま。作り直すとプレイが失われる）。
     *
     * <p>タイマーは呼ばれた時点で既にポーズ済み。戻すのはこちらの責任。
     */
    /**
     * チュートリアルを頭から通して、終わったら onComplete へ進む。
     *
     * <p>ルートは JSON の並び順に連続で流す（1 本ずつ切らない）。
     * ページを送り切ると次のルートへ入り、最後まで行くと抜ける。
     *
     * @param force タイトルから開いたときは true。既読でも必ず通す。
     *              初回の自動再生では false を渡し、既読なら黙って飛ばす
     */
    private void playTutorial(boolean force, Runnable onComplete) {
        List<ScenarioRoute> routes = Scenario.load().routesOf(Scenario.TUTORIAL);
        if (shotMode || routes.isEmpty() || (!force && config.hasSeenTutorial())) {
            onComplete.run();
            return;
        }
        // 飛ばされても「通した」ことにする。でないと ESC を押した人に毎回出る
        config.markTutorialSeen();
        playRoutesInSequence(routes, 0, onComplete);
    }

    /** ルート列を先頭から順に再生する。1 本終わるたびに次を開く */
    private void playRoutesInSequence(List<ScenarioRoute> routes, int index, Runnable onComplete) {
        if (index >= routes.size()) {
            onComplete.run();
            return;
        }
        ScenarioRoute route = routes.get(index);
        if (route.isEmpty()) {
            playRoutesInSequence(routes, index + 1, onComplete);
            return;
        }
        playAdventureRoute(route, () -> playRoutesInSequence(routes, index + 1, onComplete));
    }

    // =====================================================
    //  回想モード
    // =====================================================

    Scene makeRecollectionScene() {
        stopLoopingAnimations();
        List<ScenarioRoute> routes = Scenario.load().routesOf(Scenario.ENDING);
        List<Boolean> unlocked = new ArrayList<>();
        for (ScenarioRoute r : routes) {
            unlocked.add(config.hasSeenEnding(r.id()));
        }

        RecollectionPane pane = new RecollectionPane(routes, unlocked);
        Scene scene = new Scene(pane.getRoot(), WINDOW_WIDTH, WINDOW_HEIGHT);

        setDebouncedKeyHandler(scene, code -> {
            switch (code) {
                case ESCAPE -> showStartScene();
                case UP     -> pane.moveCursor(-1);
                case DOWN   -> pane.moveCursor(1);
                case SPACE, ENTER -> {
                    ScenarioRoute route = pane.selected();
                    if (route != null && !route.isEmpty()) {
                        // 読み終わりもスキップも一覧へ戻す。ゲームオーバー画面へは行かない
                        playAdventureRoute(route, this::showRecollectionScene);
                    }
                }
                default -> { }
            }
        });

        return scene;
    }

    private void showRecollectionScene() {
        fadeInScene(makeRecollectionScene());
    }

    /** メニュー系の効果音。ゲーム画面の SePlayer とは別に、必要になったとき1つだけ作る */
    private SePlayer menuSe() {
        if (menuSePlayer == null) {
            menuSePlayer = new SePlayer(config);
        }
        return menuSePlayer;
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
    private final RngHub hub;
    private final Rng fxRng;

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
    private final DialogueBank dialogue;
    private long lastDialogueNanos = 0;
    private long lastClearNanos = 0;
    private boolean spokeGameStart = false;
    private int prevLinesUntilRotate = Integer.MAX_VALUE;
    private DialogueTrigger pendingTrigger = null;
    private int pendingPriority = -1;
    private boolean pendingForce = false;

    GameLoopTimer(
            RngHub hub,
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
        this.hub = hub;
        this.fxRng = hub.stream("fx");
        this.dialogue = new DialogueBank(hub.stream("dialogue"));
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
        // AnimationTimer 内の例外は JavaFX のパルス処理に飲まれてしまい、
        // setDefaultUncaughtExceptionHandler まで届かない。ここで自前に受ける
        try {
            handleFrame(now);
        } catch (RuntimeException e) {
            ErrorDump.write(e, hub);
            // 壊れたまま 60fps で回すと同じダンプが量産されるので必ず止める
            stop();
            gameOverHandler.accept(controller.getScore(), controller.getLineCount());
        }
    }

    private void handleFrame(long now) {
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
                    double angle = fxRng.range(0, 2 * Math.PI);
                    double speed = fxRng.range(1.5, 5.5);
                    double vx = Math.cos(angle) * speed;
                    double vy = Math.sin(angle) * speed - 1.5;
                    int life = 30 + fxRng.nextInt(20);
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
            particles.add(new Particle(cx, cy, 0, -0.3, bright, 10 + fxRng.nextInt(6)));
        }
    }
}
