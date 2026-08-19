package tetris.view;

/**
 * 演出パラメータ（タイミング・強度）の一元管理。
 * 「どの演出がどれだけ続くか」を横断して見比べ・調整できるようにする。
 *
 * パーティクルの個数・速度・寿命はゲームフィールの調整値として
 * 発生箇所（GameLoopTimer）に残している。
 */
public final class FxParams {

    // ---- ワールド回転（DeviceFramePane.playWorldRotateTransition） ----
    public static final int WORLD_ROTATE_BOARD_MS = 450;
    public static final int WORLD_ROTATE_FLASH_MS = 400;
    public static final double WORLD_ROTATE_FLASH_PEAK = 0.35;

    // ---- UIスキン入れ替えフリップ（UiSwapAnimator） ----
    public static final int UI_SWAP_FOLD_MS = 200;
    public static final int UI_SWAP_OPEN_MS = 240;

    // ---- 傾き（増築の“貼り付け角度”） ----
    // 九龍城の密度は「同じ部品の反復」で作る。角度をこの2種に絞ることで、
    // 要素をいくら増やしても画面が散らからない。新しい角度を足さないこと。
    public static final double TILT_A = -6.0;
    public static final double TILT_B = 3.0;

    // ---- 回転連動キャラズーム（CharacterApproachPane） ----
    // 総尺はフリーズ窓（EFFECT_FREEZE_NANOS の導出元）以内に収めること。
    // 超える場合は下の Math.max(...) に CHAR_APPROACH_MS を加え、フリーズ側も追従させる。
    public static final int CHAR_APPROACH_MS = 420;
    public static final double CHAR_APPROACH_FROM_SCALE = 0.72;
    public static final double CHAR_APPROACH_OVERSHOOT = 1.06;
    public static final double CHAR_APPROACH_RISE_PX = 80;

    // ---- 回転演出中のゲーム進行フリーズ（GameLoopTimer） ----
    // 固定値ではなく「最も長い演出時間 + マージン」で導出する。
    // 演出時間を伸ばしたときに「盤面が滑走中に操作が再開する」事故を構造的に防ぐ。
    // マージンを大きくしすぎると操作性が悪化するため 100ms 以内に留めること。
    public static final int FREEZE_MARGIN_MS = 60;
    public static final long EFFECT_FREEZE_NANOS =
        (Math.max(WORLD_ROTATE_BOARD_MS, UI_SWAP_FOLD_MS + UI_SWAP_OPEN_MS) + FREEZE_MARGIN_MS)
            * 1_000_000L;

    // ---- ライン消去フラッシュ（GameLoopTimer.drawLineFlash） ----
    public static final long LINE_FLASH_DURATION_NANOS = 250_000_000L;
    /** フラッシュ強度 = BASE + PER_LINE × 消去ライン数(1〜4) */
    public static final double LINE_FLASH_BASE = 0.35;
    public static final double LINE_FLASH_PER_LINE = 0.15;

    // ---- 危険フラッシュ（DeviceFramePane.triggerDangerFlash） ----
    public static final double DANGER_FLASH_OPACITY = 0.45;
    public static final int DANGER_FLASH_MS = 500;

    // ---- 盤面シェイク（DeviceFramePane.triggerShake） ----
    public static final double SHAKE_AMPLITUDE_PX = 9;
    public static final int SHAKE_MS = 290;

    // ---- ポップアップ（PopupFx 経由） ----
    public static final int PLAYFIELD_POPUP_MS = 900;
    public static final double PLAYFIELD_POPUP_RISE_PX = 90;
    public static final int HUD_POPUP_MS = 1000;
    public static final double HUD_POPUP_RISE_PX = 50;
    public static final int BIG_POPUP_SCALE_MS = 200;
    public static final int BIG_POPUP_FADE_MS = 700;
    public static final int BIG_POPUP_FADE_DELAY_MS = 500;

    // ---- スキン適用時の盤面不透明度フェード（DeviceFramePane.applySkin） ----
    public static final int SKIN_OPACITY_FADE_MS = 500;

    // ---- セリフ・スコアポップアップ制御（GameLoopTimer） ----
    public static final long DIALOGUE_COOLDOWN_NANOS = 2_000_000_000L;
    public static final long IDLE_THRESHOLD_NANOS = 20_000_000_000L;
    /** HUDスコアポップアップを出す最小加点。ソフトドロップの +1 連発を抑制する */
    public static final int MIN_SCORE_POPUP = 5;

    private FxParams() {
    }
}
