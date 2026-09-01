package tetris.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;

import tetris.model.Board;
import tetris.model.GameAction;
import tetris.model.KeyBindings;
import tetris.model.PieceRandomizer;
import tetris.model.RngHub;
import tetris.model.SeEvent;
import tetris.model.SevenBagRandomizer;
import tetris.model.ShapeType;
import tetris.model.Tetromino;

public class GameController {

    private final Board board;
    private Tetromino current;
    private Tetromino next;
    private Tetromino hold;
    private boolean canHold = true;
    private final PieceRandomizer randomizer;

    // ワールド回転（重力反転ギミック）の閾値
    private static final int LINE_ROTATE_INTERVAL = 3;
    private int nextRotateThreshold = LINE_ROTATE_INTERVAL;
    private int worldRotateCount = 0;

    // 仮ゲームオーバー（4回で真のゲームオーバー）
    private int gameOverStreak = 0;
    private static final int MAX_GAME_OVER_STREAK = 4;

    // ==========================
    //   レベル・落下速度
    //   （25×25 の広い盤面なので標準テトリスより緩めの加速。プレイテストで調整する）
    // ==========================
    private static final int LINES_PER_LEVEL = 5;
    private static final long BASE_FALL_NANOS = 300_000_000L;  // Lv1 = 300ms
    private static final long MIN_FALL_NANOS  = 80_000_000L;   // 下限 80ms
    private static final long FALL_STEP_NANOS = 25_000_000L;   // 1Lv ごとに -25ms

    // ==========================
    //   入力関連パラメータ
    // ==========================
    private long lastLeftPress = 0;
    private long lastRightPress = 0;

    private static final long DAS = 150_000_000L;
    private static final long ARR = 30_000_000L;

    private long lastMoveLeftRepeat = 0;
    private long lastMoveRightRepeat = 0;

    private long lastSoftDrop = 0;
    private static final long SDF = 40_000_000L;

    // --- ロック遅延 ---
    private boolean isGrounded = false;
    private long groundStartTime = 0;
    private static final long LOCK_DELAY = 500_000_000L;

    // --- ロック遅延のムーブリセット（ガイドライン準拠） ---
    // 接地中の移動・回転成功でロック遅延を巻き戻す。無限回転対策として上限15回
    private int lockResetCount = 0;
    private static final int MAX_LOCK_RESETS = 15;

    // --- ドロップ加点（ガイドライン標準: ソフト1点/マス・ハード2点/マス） ---
    private static final int SOFT_DROP_SCORE_PER_CELL = 1;
    private static final int HARD_DROP_SCORE_PER_CELL = 2;

    // ==========================
    //   スコア・ライン・接地ブロック数
    // ==========================
    private int score = 0;
    private int totalLines = 0;
    private int placedMinoCount = 0;

    // ==========================
    //   T-Spin / Ren
    // ==========================
    private boolean lastActionWasRotate = false;
    private int comboCount = -1;

    // ==========================
    //   ハードドロップ軌跡
    // ==========================
    private final List<int[]> hardDropTrailCells = new ArrayList<>();
    private Color lastHardDropColor = null;

    private enum TSpinType { NONE, MINI, FULL }

    public int getScore()             { return score; }
    public int getLineCount()         { return totalLines; }
    public int getLevel()             { return totalLines / LINES_PER_LEVEL + 1; }

    /** レベルに応じた自然落下間隔。Lv1=300ms から 1Lv ごとに 25ms 短縮、下限 80ms */
    public long getFallIntervalNanos() {
        long interval = BASE_FALL_NANOS - (long) (getLevel() - 1) * FALL_STEP_NANOS;
        return Math.max(MIN_FALL_NANOS, interval);
    }
    public int getPlacedMinoCount()   { return placedMinoCount; }
    public int getWorldRotateCount()  { return worldRotateCount; }
    /** 盤面の物理的な向き（90°×step）。4回転で元に戻るため 0〜3 を循環する */
    public int getWorldRotateStep()   { return worldRotateCount % 4; }
    public int getWorldRotateLoopCount() { return worldRotateCount / 4; }
    public Tetromino getNext()        { return next; }
    public Tetromino getHold()        { return hold; }
    public boolean canHold()          { return canHold; }
    public void rotateWorldClockwise() { rotateWorldAndCount(); }
    public int getComboCount()        { return comboCount; }
    public int getGameOverStreak()    { return gameOverStreak; }
    public int getMaxGameOverStreak() { return MAX_GAME_OVER_STREAK; }
    /** ワールド回転までのライン数（HUD のゲージ目盛り数に使う） */
    public int getLineRotateInterval() { return LINE_ROTATE_INTERVAL; }

    /** 次のワールド回転までの残り消去ライン数 */
    public int getLinesUntilRotate() {
        return Math.max(0, nextRotateThreshold - board.getTotalClearedLines());
    }

    public Color getLastHardDropColor() { return lastHardDropColor; }

    public List<int[]> drainHardDropTrail() {
        List<int[]> result = new ArrayList<>(hardDropTrailCells);
        hardDropTrailCells.clear();
        return result;
    }

    // ==========================
    //   SRS キックテーブル
    // ==========================

    private static final int[][][][] KICK_NORMAL = {
        { { {0,0},{-1,0},{-1,1},{0,-2},{-1,-2} },
          { {0,0},{1,0},{1,1},{0,-2},{1,-2}  } },
        { { {0,0},{1,0},{1,-1},{0,2},{1,2} },
          { {0,0},{1,0},{1,-1},{0,2},{1,2} } },
        { { {0,0},{1,0},{1,1},{0,-2},{1,-2} },
          { {0,0},{-1,0},{-1,1},{0,-2},{-1,-2} } },
        { { {0,0},{-1,0},{-1,-1},{0,2},{-1,2} },
          { {0,0},{-1,0},{-1,-1},{0,2},{-1,2} } }
    };

    private static final int[][][][] KICK_I = {
        { { {0,0},{-2,0},{1,0},{-2,-1},{1,2} },
          { {0,0},{-1,0},{2,0},{-1,2},{2,-1} } },
        { { {0,0},{-1,0},{2,0},{-1,2},{2,-1} },
          { {0,0},{2,0},{-1,0},{2,1},{-1,-2} } },
        { { {0,0},{2,0},{-1,0},{2,1},{-1,-2} },
          { {0,0},{1,0},{-2,0},{1,-2},{-2,1} } },
        { { {0,0},{1,0},{-2,0},{1,-2},{-2,1} },
          { {0,0},{-2,0},{1,0},{-2,-1},{1,2} } }
    };

    // ==================================================
    //                 コンストラクタ
    // ==================================================

    /** 既存の呼び出し互換。7-bag ＋ 毎回変わるシード */
    public GameController() {
        this(new SevenBagRandomizer(RngHub.fromSystemProperty().stream("piece")));
    }

    public GameController(PieceRandomizer randomizer) {
        this.randomizer = randomizer;
        board = new Board();
        current = getNextTetromino();
        next = getNextTetromino();
    }

    private Tetromino getNextTetromino() {
        return new Tetromino(randomizer.next());
    }

    public Board getBoard()        { return board; }
    public Tetromino getCurrent()  { return current; }

    // ==================================================
    //                    移動系 API
    // ==================================================

    /**
     * 1段落下させる。
     *
     * @param byPlayer プレイヤーのソフトドロップ入力なら true（1マスごとに加点）。
     *                 自然落下は false（加点なし）。
     */
    public boolean softDrop(boolean byPlayer) {
        if (board.canMoveDown(current)) {
            current.setRow(current.getRow() + 1);
            if (byPlayer) {
                score += SOFT_DROP_SCORE_PER_CELL;
            }
            isGrounded = false;
            groundStartTime = 0;
            lockResetCount = 0; // 落下に成功したらムーブリセット回数を回復
            return true;
        } else {
            if (!isGrounded) {
                isGrounded = true;
                groundStartTime = System.nanoTime();
                return true;
            }
            long now = System.nanoTime();
            if (now - groundStartTime > LOCK_DELAY) {
                lockPiece();
            }
            return false;
        }
    }

    public void moveLeft() {
        if (board.canMove(current, 0, -1)) {
            current.setCol(current.getCol() - 1);
            resetGroundIfLifted();
            onSuccessfulShift();
            lastActionWasRotate = false;
            frameEvents.add(SeEvent.MOVE);
        }
    }

    public void moveRight() {
        if (board.canMove(current, 0, 1)) {
            current.setCol(current.getCol() + 1);
            resetGroundIfLifted();
            onSuccessfulShift();
            lastActionWasRotate = false;
            frameEvents.add(SeEvent.MOVE);
        }
    }

    public void rotateRight() {
        rotateSRS(true);
    }

    public void rotateLeft() {
        rotateSRS(false);
    }

    public void hardDrop() {
        int startRow = current.getRow();
        int col = current.getCol();
        int[][] shape = current.getShape();
        lastHardDropColor = current.getColor();

        while (board.canMoveDown(current)) {
            current.setRow(current.getRow() + 1);
        }

        int endRow = current.getRow();
        score += (endRow - startRow) * HARD_DROP_SCORE_PER_CELL;
        hardDropTrailCells.clear();
        for (int r = startRow; r < endRow; r++) {
            for (int sr = 0; sr < 4; sr++) {
                for (int sc = 0; sc < 4; sc++) {
                    if (shape[sr][sc] == 1) {
                        hardDropTrailCells.add(new int[]{r + sr, col + sc});
                    }
                }
            }
        }

        frameEvents.add(SeEvent.HARD_DROP);
        lockPiece();
    }

    public void holdCurrentPiece() {
        if (!canHold || current == null || trueGameOver) {
            return;
        }

        // 交換先ミノのスポーン位置が塞がっている場合はホールド自体を不発にする。
        // 自衛操作のホールドで仮ゲームオーバー＋世界回転が誘発されるのは理不尽なため
        Tetromino incoming = (hold == null) ? next : new Tetromino(hold.getType());
        if (!board.canMove(incoming, 0, 0)) {
            return;
        }

        ShapeType currentType = current.getType();
        if (hold == null) {
            hold = new Tetromino(currentType);
            current = next;
            next = getNextTetromino();
        } else {
            ShapeType holdType = hold.getType();
            hold = new Tetromino(currentType);
            current = new Tetromino(holdType);
        }

        canHold = false;
        isGrounded = false;
        groundStartTime = 0;
        lockResetCount = 0;
        lastActionWasRotate = false;
        frameEvents.add(SeEvent.HOLD);
    }

    // ==================================================
    //              入力状態（1フレーム前保持）
    // ==================================================
    private boolean prevLeft = false;
    private boolean prevRight = false;
    private boolean prevRotL = false;
    private boolean prevRotR = false;
    // ゲーム開始時点で SPACE は「押されているもの」とみなす。
    // スタート/リトライをSPACEで行うため、押しっぱなしでの即ハードドロップ暴発を防ぐ。
    private boolean prevSpace = true;
    // SPACE はシーン遷移キーでもあるため、一度離されるのを確認するまで
    // ハードドロップを許可しない（キーリピートが初フレーム後に届く競合への対策）
    private boolean spaceArmed = false;
    private boolean prevH = false;

    public void updateInput(Set<KeyCode> keys, KeyBindings binds, long now) {

        boolean left  = binds.isDown(GameAction.MOVE_LEFT, keys);
        boolean right = binds.isDown(GameAction.MOVE_RIGHT, keys);
        boolean down  = binds.isDown(GameAction.SOFT_DROP, keys);
        boolean rotR  = binds.isDown(GameAction.ROTATE_RIGHT, keys);
        boolean rotL  = binds.isDown(GameAction.ROTATE_LEFT, keys);
        boolean space = binds.isDown(GameAction.HARD_DROP, keys);
        boolean h     = binds.isDown(GameAction.HOLD, keys);

        if (left && right) {
            left = false;
            right = false;
        }

        if (left) {
            if (!prevLeft) {
                moveLeft();
                lastLeftPress = now;
                lastMoveLeftRepeat = now;
            } else if (now - lastLeftPress > DAS &&
                       now - lastMoveLeftRepeat > ARR) {
                moveLeft();
                lastMoveLeftRepeat = now;
            }
        }

        if (right) {
            if (!prevRight) {
                moveRight();
                lastRightPress = now;
                lastMoveRightRepeat = now;
            } else if (now - lastRightPress > DAS &&
                       now - lastMoveRightRepeat > ARR) {
                moveRight();
                lastMoveRightRepeat = now;
            }
        }

        if (rotL && !prevRotL) {
            rotateLeft();
        }
        // 右回転は既定で X と ↑ の 2 つ。どれか押されていれば 1 回だけ回す
        if (rotR && !prevRotR) {
            rotateRight();
        }

        if (space && !prevSpace && spaceArmed) {
            hardDrop();
        }
        if (!space) {
            spaceArmed = true;
        }

        if (h && !prevH) {
            holdCurrentPiece();
        }

        if (down) {
            if (now - lastSoftDrop > SDF) {
                softDrop(true);
                lastSoftDrop = now;
            }
        }

        prevLeft = left;
        prevRight = right;
        prevRotL = rotL;
        prevRotR = rotR;
        prevSpace = space;
        prevH = h;
    }

    // ==================================================
    //                    ゴースト
    // ==================================================
    public Tetromino getGhost() {
        Tetromino g = current.copy();
        while (board.canMoveDown(g)) {
            g.setRow(g.getRow() + 1);
        }
        return g;
    }

    // ==================================================
    //              ロジック・ロック処理
    // ==================================================
    private final EnumSet<SeEvent> frameEvents = EnumSet.noneOf(SeEvent.class);

    public Set<SeEvent> drainEvents() {
        if (frameEvents.isEmpty()) return Collections.emptySet();
        Set<SeEvent> snapshot = EnumSet.copyOf(frameEvents);
        frameEvents.clear();
        return snapshot;
    }

    private boolean trueGameOver = false;

    public boolean isTrueGameOver() {
        return trueGameOver;
    }

    // ==========================
    //   T-Spin 検出
    // ==========================
    private TSpinType detectTSpin() {
        if (!lastActionWasRotate || current.getType() != ShapeType.T) {
            return TSpinType.NONE;
        }

        int r = current.getRow();
        int c = current.getCol();

        boolean tl = board.isBlocked(r,     c);
        boolean tr = board.isBlocked(r,     c + 2);
        boolean bl = board.isBlocked(r + 2, c);
        boolean br = board.isBlocked(r + 2, c + 2);

        int occupied = (tl ? 1 : 0) + (tr ? 1 : 0) + (bl ? 1 : 0) + (br ? 1 : 0);
        if (occupied < 3) return TSpinType.NONE;

        // T の突起方向がフロントコーナー（A-side）
        boolean frontA, frontB;
        switch (current.getRotation()) {
            case 0 -> { frontA = tl; frontB = tr; } // T上向き: 上コーナーがフロント
            case 1 -> { frontA = tr; frontB = br; } // T右向き: 右コーナーがフロント
            case 2 -> { frontA = bl; frontB = br; } // T下向き: 下コーナーがフロント
            case 3 -> { frontA = tl; frontB = bl; } // T左向き: 左コーナーがフロント
            default -> { frontA = false; frontB = false; }
        }

        return (frontA && frontB) ? TSpinType.FULL : TSpinType.MINI;
    }

    private void lockPiece() {
        TSpinType tSpin = detectTSpin();

        if (!board.fixToBoard(current)) {
            // ロックアウト: 全ブロックが盤面外（天井より上）で固定された。
            // ブロックが「欠けて」残ることはないため、スポーン詰まりと同じペナルティに乗せる
            comboCount = -1;
            canHold = true;
            lastActionWasRotate = false;
            isGrounded = false;
            groundStartTime = 0;
            lockResetCount = 0;
            applyTempGameOverPenalty();
            return;
        }

        score += current.countBlocks() * 5;

        int before = board.getTotalClearedLines();
        board.clearCompletedLines();
        int after  = board.getTotalClearedLines();
        int cleared = after - before;

        if (cleared > 0) {
            totalLines += cleared;
            comboCount++;
            if (comboCount >= 1) {
                score += comboCount * 50;
                frameEvents.add(SeEvent.REN);
            }
            addScore(cleared, tSpin);
            frameEvents.add(SeEvent.LINE_CLEAR);
            if (tSpin == TSpinType.FULL) {
                frameEvents.add(SeEvent.T_SPIN);
            } else if (tSpin == TSpinType.MINI) {
                frameEvents.add(SeEvent.T_SPIN_MINI);
            }
        } else {
            comboCount = -1;
        }

        // ライン閾値による「盤面回転」
        if (board.getTotalClearedLines() >= nextRotateThreshold) {
            rotateWorldAndCount();
            nextRotateThreshold += LINE_ROTATE_INTERVAL;
        }

        current = next;
        next = getNextTetromino();
        canHold = true;
        lastActionWasRotate = false;

        if (handleSpawnBlocked()) {
            return;
        }

        gameOverStreak = 0;
        isGrounded = false;
        groundStartTime = 0;
        lockResetCount = 0;
        placedMinoCount++;
        frameEvents.add(SeEvent.LOCK);
    }

    private boolean handleSpawnBlocked() {
        if (board.canMove(current, 0, 0)) {
            return false;
        }
        applyTempGameOverPenalty();
        return true;
    }

    /**
     * 仮ゲームオーバー（スポーン詰まり・ロックアウト）共通のペナルティ。
     * ストリーク加算＋世界回転を行い、上限到達で真のゲームオーバー。
     * 続行できる場合は現在のミノを破棄して新しいミノに差し替える。
     */
    private void applyTempGameOverPenalty() {
        gameOverStreak++;
        frameEvents.add(SeEvent.TEMP_GAME_OVER);
        rotateWorldAndCount();

        if (gameOverStreak >= MAX_GAME_OVER_STREAK) {
            trueGameOver = true;
            return;
        }

        current = getNextTetromino();
    }

    private void rotateWorldAndCount() {
        board.rotateClockwise();
        worldRotateCount++;
        frameEvents.add(SeEvent.WORLD_ROTATE);
    }

    private void addScore(int cleared, TSpinType tSpin) {
        if (tSpin == TSpinType.FULL) {
            score += switch (cleared) {
                case 0 -> 400;
                case 1 -> 800;
                case 2 -> 1200;
                case 3 -> 1600;
                default -> 800;
            };
        } else if (tSpin == TSpinType.MINI) {
            score += switch (cleared) {
                case 0 -> 100;
                case 1 -> 200;
                case 2 -> 400;
                default -> 200;
            };
        } else {
            score += switch (cleared) {
                case 1 -> 100;
                case 2 -> 300;
                case 3 -> 500;
                case 4 -> 800;
                default -> 0;
            };
        }
    }

    private void resetGroundIfLifted() {
        if (board.canMoveDown(current)) {
            isGrounded = false;
            groundStartTime = 0;
        }
    }

    /**
     * 接地中の移動・回転成功時にロック遅延を巻き戻す（ムーブリセット）。
     * 無限回転・無限スライド対策として上限 MAX_LOCK_RESETS 回、超過後は延長しない。
     */
    private void onSuccessfulShift() {
        if (isGrounded && lockResetCount < MAX_LOCK_RESETS) {
            groundStartTime = System.nanoTime();
            lockResetCount++;
        }
    }

    /**
     * ポーズ解除時に System.nanoTime() 基準のタイマーをポーズ時間ぶんずらす。
     * これが無いと接地中にポーズ → 解除した瞬間にロック遅延が即発火する。
     */
    public void shiftTimersAfterPause(long pausedNanos) {
        if (groundStartTime != 0) groundStartTime += pausedNanos;
        lastLeftPress += pausedNanos;
        lastRightPress += pausedNanos;
        lastMoveLeftRepeat += pausedNanos;
        lastMoveRightRepeat += pausedNanos;
        lastSoftDrop += pausedNanos;
    }

    // ==================================================
    //                     SRS 本体
    // ==================================================

    private int toIndex(int rot) {
        return (rot % 4 + 4) % 4;
    }

    private void rotateSRS(boolean clockwise) {
        // O ミノは回転しない（ガイドライン準拠）。SE もロック遅延リセットも発生させない
        if (current.getType() == ShapeType.O) {
            return;
        }

        Tetromino t = current;

        int oldRot = toIndex(t.getRotation());
        int newRot = clockwise ? toIndex(oldRot + 1) : toIndex(oldRot - 1);

        int[][] rotatedShape = t.getType().getShape(newRot);

        int[][][][] table = (t.getType() == ShapeType.I) ? KICK_I : KICK_NORMAL;
        int[][] kicks = table[oldRot][clockwise ? 0 : 1];

        for (int[] k : kicks) {
            int newCol = t.getCol() + k[0];
            int newRow = t.getRow() - k[1];

            if (board.canPlace(rotatedShape, newRow, newCol)) {
                t.setShape(rotatedShape);
                t.setCol(newCol);
                t.setRow(newRow);
                t.setRotation(newRot);

                resetGroundIfLifted();
                onSuccessfulShift();
                lastActionWasRotate = true;
                frameEvents.add(SeEvent.ROTATE);
                return;
            }
        }
    }
}
