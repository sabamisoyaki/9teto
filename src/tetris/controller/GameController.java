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

import tetris.model.Board;
import tetris.model.SeEvent;
import tetris.model.ShapeType;
import tetris.model.Tetromino;

public class GameController {

    private final Board board;
    private Tetromino current;
    private Tetromino next;
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

    private static final long DAS = 150_000_000L;  // 150ms
    private static final long ARR = 30_000_000L;   // 30ms

    private long lastMoveLeftRepeat = 0;
    private long lastMoveRightRepeat = 0;

    private long lastSoftDrop = 0;
    private static final long SDF = 40_000_000L;   // 40ms 下押し高速落下

    // --- ロック遅延 ---
    private boolean isGrounded = false;
    private long groundStartTime = 0;
    private static final long LOCK_DELAY = 500_000_000L; // 500ms くらい

    // ==========================
    //   スコア・ライン・接地ブロック数
    // ==========================
    private int score = 0;
    private int totalLines = 0;
    private int placedMinoCount = 0;

    public int getScore()             { return score; }
    public int getLineCount()         { return totalLines; }
    public int getPlacedMinoCount()   { return placedMinoCount; }
    public int getWorldRotateCount()  { return worldRotateCount; }
    public int getWorldRotateStep()   { return worldRotateStep; }
    public int getWorldRotateLoopCount() { return worldRotateLoopCount; }
    public Tetromino getNext()        { return next; }
    public void rotateWorldClockwise() { rotateWorldAndCount(); }

    // ==========================
    //   SRS キックテーブル
    // ==========================

    // JLSTZ 共通（簡易 SRS）: [fromRot][0=CW/1=CCW] -> { (dx, dy)... }
    private static final int[][][][] KICK_NORMAL = {
        // from 0
        { { {0,0},{-1,0},{-1,1},{0,-2},{-1,-2} },   // 0 -> 1 (CW)
          { {0,0},{1,0},{1,1},{0,-2},{1,-2}  } },    // 0 -> 3 (CCW)

        // from 1
        { { {0,0},{1,0},{1,-1},{0,2},{1,2} },        // 1 -> 2 (CW)
          { {0,0},{1,0},{1,-1},{0,2},{1,2} } },      // 1 -> 0 (CCW)

        // from 2
        { { {0,0},{1,0},{1,1},{0,-2},{1,-2} },       // 2 -> 3 (CW)
          { {0,0},{-1,0},{-1,1},{0,-2},{-1,-2} } },  // 2 -> 1 (CCW)

        // from 3
        { { {0,0},{-1,0},{-1,-1},{0,2},{-1,2} },     // 3 -> 0 (CW)
          { {0,0},{-1,0},{-1,-1},{0,2},{-1,2} } }    // 3 -> 2 (CCW)
    };

    // I ミノ用（簡易 SRS）
    private static final int[][][][] KICK_I = {
        // from 0
        { { {0,0},{-2,0},{1,0},{-2,-1},{1,2} },      // 0 -> 1 (CW)
          { {0,0},{-1,0},{2,0},{-1,2},{2,-1} } },    // 0 -> 3 (CCW)

        // from 1
        { { {0,0},{-1,0},{2,0},{-1,2},{2,-1} },      // 1 -> 2 (CW)
          { {0,0},{2,0},{-1,0},{2,1},{-1,-2} } },    // 1 -> 0 (CCW)

        // from 2
        { { {0,0},{2,0},{-1,0},{2,1},{-1,-2} },      // 2 -> 3 (CW)
          { {0,0},{1,0},{-2,0},{1,-2},{-2,1} } },    // 2 -> 1 (CCW)

        // from 3
        { { {0,0},{1,0},{-2,0},{1,-2},{-2,1} },      // 3 -> 0 (CW)
          { {0,0},{-2,0},{1,0},{-2,-1},{1,2} } }     // 3 -> 2 (CCW)
    };

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

    /** 自然落下 / ソフトドロップ共通 */
    public boolean softDrop() {
        if (board.canMoveDown(current)) {

            current.setRow(current.getRow() + 1);

            // 接地解除
            isGrounded = false;
            groundStartTime = 0;

            return true;

        } else {
            // 接地した瞬間
            if (!isGrounded) {
                isGrounded = true;
                groundStartTime = System.nanoTime();
                return true;  // まだ固定しない
            }

            // 接地状態が続いているならロック遅延を判定
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
            frameEvents.add(SeEvent.MOVE);
        }
    }

    public void moveRight() {
        if (board.canMove(current, 0, 1)) {
            current.setCol(current.getCol() + 1);
            resetGroundIfLifted();
            frameEvents.add(SeEvent.MOVE);
        }
    }

    // ---- SRS 回転の公開API ----
    public void rotateRight() {
        rotateSRS(true);
    }

    public void rotateLeft() {
        rotateSRS(false);
    }

    // --- ハードドロップ ---
    public void hardDrop() {
        while (board.canMoveDown(current)) {
            current.setRow(current.getRow() + 1);
        }
        frameEvents.add(SeEvent.HARD_DROP);
        lockPiece();
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

    public void updateInput(Set<KeyCode> keys, long now) {

        boolean left  = keys.contains(KeyCode.LEFT);
        boolean right = keys.contains(KeyCode.RIGHT);
        boolean down  = keys.contains(KeyCode.DOWN);
        boolean up    = keys.contains(KeyCode.UP);
        boolean z     = keys.contains(KeyCode.Z);
        boolean x     = keys.contains(KeyCode.X);
        boolean space = keys.contains(KeyCode.SPACE);

        // ====================================
        // 横移動（DAS / ARR 押しっぱ対応）
        // ====================================

        if (left && right) {
            left = false;
            right = false;
        }

        // ---- 左 ----
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

        // ---- 右 ----
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

        // ====================================
        // 回転（単発入力）
        // ====================================
        if (z && !prevZ) {
            rotateLeft();
        }
        if (x && !prevX) {
            rotateRight();
        }
        if (up && !prevUp) {
            rotateRight();
        }

        // ====================================
        // ハードドロップ（単発）
        // ====================================
        if (space && !prevSpace) {
            hardDrop();
        }

        // ====================================
        // ソフトドロップ（押しっぱOK）
        // ====================================
        if (down) {
            if (now - lastSoftDrop > SDF) {
                softDrop();
                lastSoftDrop = now;
            }
        }

        // ====================================
        // 前フレーム入力を保存
        // ====================================
        prevLeft = left;
        prevRight = right;
        prevZ = z;
        prevX = x;
        prevUp = up;
        prevSpace = space;
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
    // ==========================
    //   フレームイベント
    // ==========================
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

    private void lockPiece() {

        // ピースを盤面に固定
        board.fixToBoard(current);

        score += current.countBlocks() * 5;
        // ライン数の差分を取ってスコア更新
        int before = board.getTotalClearedLines();
        board.clearCompletedLines();
        int after  = board.getTotalClearedLines();
        int cleared = after - before;

        if (cleared > 0) {
            totalLines += cleared;
            addScore(cleared);
            frameEvents.add(SeEvent.LINE_CLEAR);
        }

        // ライン閾値による「盤面回転」
        if (board.getTotalClearedLines() >= nextRotateThreshold) {
            rotateWorldAndCount();
            nextRotateThreshold += LINE_ROTATE_INTERVAL;
        }

        // 次ミノに交代
        current = next;
        next = getNextTetromino();

        // 出現即死チェック
        if (!board.canMove(current, 0, 0)) {
            gameOverStreak++;
            System.out.println("TEMP GAME OVER (" + gameOverStreak + "/4)");

            rotateWorldAndCount();

            if (gameOverStreak >= MAX_GAME_OVER_STREAK) {
                trueGameOver = true;
                return;
            }

            current = getNextTetromino();
            return;
        }

        gameOverStreak = 0;
        isGrounded = false;
        groundStartTime = 0;
        placedMinoCount++;
        System.out.println(placedMinoCount);
        frameEvents.add(SeEvent.LOCK);
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


    // スコア加算（シンプル版）
    private void addScore(int cleared) {
        switch (cleared) {
            case 1 -> score += 100;
            case 2 -> score += 300;
            case 3 -> score += 500;
            case 4 -> score += 800;
        }
    }

    // 横移動・回転で「浮いたら」ロック遅延解除
    private void resetGroundIfLifted() {
        if (board.canMoveDown(current)) {
            isGrounded = false;
            groundStartTime = 0;
        }
    }

    // ==================================================
    //                     SRS 本体
    // ==================================================

    private int[][] rotateShape(int[][] src, boolean clockwise) {
        int[][] dst = new int[4][4];
        if (clockwise) {
            for (int r = 0; r < 4; r++)
                for (int c = 0; c < 4; c++)
                    dst[c][3 - r] = src[r][c];
        } else {
            for (int r = 0; r < 4; r++)
                for (int c = 0; c < 4; c++)
                    dst[3 - c][r] = src[r][c];
        }
        return dst;
    }

    private int toIndex(int rot) {
        return (rot % 4 + 4) % 4;
    }

    private void rotateSRS(boolean clockwise) {
        Tetromino t = current;

        int oldRot = toIndex(t.getRotation());
        int newRot = clockwise ? toIndex(oldRot + 1) : toIndex(oldRot - 1);

        int[][] rotatedShape = rotateShape(t.getShape(), clockwise);

        int[][][][] table = (t.getType() == ShapeType.I) ? KICK_I : KICK_NORMAL;
        int[][] kicks = table[oldRot][clockwise ? 0 : 1];

        for (int[] k : kicks) {
            int newCol = t.getCol() + k[0];
            int newRow = t.getRow() + k[1];

            if (board.canPlace(rotatedShape, newRow, newCol)) {
                t.setShape(rotatedShape);
                t.setCol(newCol);
                t.setRow(newRow);
                t.setRotation(newRot);

                resetGroundIfLifted();
                frameEvents.add(SeEvent.ROTATE);
                return;
            }
        }
        // すべて失敗 → 回転しない
    }
}
