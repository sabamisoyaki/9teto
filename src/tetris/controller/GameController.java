package tetris.controller;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;

import tetris.model.Board;
import tetris.model.SeEvent;
import tetris.model.ShapeType;
import tetris.model.Tetromino;

public class GameController {

    private final Board board;
    private Tetromino current;
    private Tetromino next;
    private Tetromino hold;
    private boolean canHold = true;
    private final Queue<ShapeType> bag = new ArrayDeque<>();

    // ワールド回転（重力反転ギミック）の閾値
    private static final int LINE_ROTATE_INTERVAL = 3;
    private int nextRotateThreshold = LINE_ROTATE_INTERVAL;
    private int worldRotateCount = 0;
    private int worldRotateStep = 0;
    private int worldRotateLoopCount = 0;

    // 仮ゲームオーバー（4回で真のゲームオーバー）
    private int gameOverStreak = 0;
    private static final int MAX_GAME_OVER_STREAK = 4;

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
    public int getLevel()             { return Math.min(10, (totalLines / 10) + 1); }
    public int getPlacedMinoCount()   { return placedMinoCount; }
    public int getWorldRotateCount()  { return worldRotateCount; }
    public int getWorldRotateStep()   { return worldRotateStep; }
    public int getWorldRotateLoopCount() { return worldRotateLoopCount; }
    public Tetromino getNext()        { return next; }
    public Tetromino getHold()        { return hold; }
    public boolean canHold()          { return canHold; }
    public void rotateWorldClockwise() { rotateWorldAndCount(); }
    public int getComboCount()        { return comboCount; }

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

    private static final int[][] KICK_NONE = { {0, 0} };

    // ==================================================
    //                 コンストラクタ
    // ==================================================

    public GameController() {
        board = new Board();
        generateBag();
        current = getNextTetromino();
        next = getNextTetromino();
    }

    private void generateBag() {
        List<ShapeType> list = new ArrayList<>(Arrays.asList(ShapeType.values()));
        Collections.shuffle(list);
        bag.addAll(list);
    }

    private Tetromino getNextTetromino() {
        if (bag.isEmpty()) {
            generateBag();
        }
        return new Tetromino(bag.poll());
    }

    public Board getBoard()        { return board; }
    public Tetromino getCurrent()  { return current; }

    // ==================================================
    //                    移動系 API
    // ==================================================

    public boolean softDrop() {
        if (board.canMoveDown(current)) {
            current.setRow(current.getRow() + 1);
            isGrounded = false;
            groundStartTime = 0;
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
            lastActionWasRotate = false;
            frameEvents.add(SeEvent.MOVE);
        }
    }

    public void moveRight() {
        if (board.canMove(current, 0, 1)) {
            current.setCol(current.getCol() + 1);
            resetGroundIfLifted();
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
        lastActionWasRotate = false;
        frameEvents.add(SeEvent.HOLD);

        handleSpawnBlocked();
    }

    // ==================================================
    //              入力状態（1フレーム前保持）
    // ==================================================
    private boolean prevLeft = false;
    private boolean prevRight = false;
    private boolean prevZ = false;
    private boolean prevX = false;
    private boolean prevUp = false;
    private boolean prevSpace = false;
    private boolean prevH = false;

    public void updateInput(Set<KeyCode> keys, long now) {

        boolean left  = keys.contains(KeyCode.LEFT);
        boolean right = keys.contains(KeyCode.RIGHT);
        boolean down  = keys.contains(KeyCode.DOWN);
        boolean up    = keys.contains(KeyCode.UP);
        boolean z     = keys.contains(KeyCode.Z);
        boolean x     = keys.contains(KeyCode.X);
        boolean space = keys.contains(KeyCode.SPACE);
        boolean h     = keys.contains(KeyCode.H);

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

        if (z && !prevZ) {
            rotateLeft();
        }
        if (x && !prevX) {
            rotateRight();
        }
        if (up && !prevUp) {
            rotateRight();
        }

        if (space && !prevSpace) {
            hardDrop();
        }

        if (h && !prevH) {
            holdCurrentPiece();
        }

        if (down) {
            if (now - lastSoftDrop > SDF) {
                softDrop();
                lastSoftDrop = now;
            }
        }

        prevLeft = left;
        prevRight = right;
        prevZ = z;
        prevX = x;
        prevUp = up;
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

        board.fixToBoard(current);
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
        placedMinoCount++;
        frameEvents.add(SeEvent.LOCK);
    }

    private boolean handleSpawnBlocked() {
        if (board.canMove(current, 0, 0)) {
            return false;
        }

        gameOverStreak++;
        rotateWorldAndCount();

        if (gameOverStreak >= MAX_GAME_OVER_STREAK) {
            trueGameOver = true;
            return true;
        }

        current = getNextTetromino();
        return true;
    }

    private void rotateWorldAndCount() {
        board.rotateClockwise();
        worldRotateCount++;
        worldRotateStep = (worldRotateStep % 3) + 1;
        if (worldRotateStep == 1 && worldRotateCount >= 4) {
            worldRotateLoopCount++;
        }
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

    // ==================================================
    //                     SRS 本体
    // ==================================================

    private int toIndex(int rot) {
        return (rot % 4 + 4) % 4;
    }

    private void rotateSRS(boolean clockwise) {
        Tetromino t = current;

        int oldRot = toIndex(t.getRotation());
        int newRot = clockwise ? toIndex(oldRot + 1) : toIndex(oldRot - 1);

        int[][] rotatedShape = t.getType().getShape(newRot);

        int[][] kicks;
        if (t.getType() == ShapeType.O) {
            kicks = KICK_NONE;
        } else {
            int[][][][] table = (t.getType() == ShapeType.I) ? KICK_I : KICK_NORMAL;
            kicks = table[oldRot][clockwise ? 0 : 1];
        }

        for (int[] k : kicks) {
            int newCol = t.getCol() + k[0];
            int newRow = t.getRow() - k[1];

            if (board.canPlace(rotatedShape, newRow, newCol)) {
                t.setShape(rotatedShape);
                t.setCol(newCol);
                t.setRow(newRow);
                t.setRotation(newRot);

                resetGroundIfLifted();
                lastActionWasRotate = true;
                frameEvents.add(SeEvent.ROTATE);
                return;
            }
        }
    }
}
