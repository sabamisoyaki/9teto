package tetris.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.scene.paint.Color;

public class Board {

    public static final int ROWS = 25;
    public static final int COLS = 25;

    private Color[][] board = new Color[ROWS][COLS];
    private int totalClearedLines = 0;

    private final List<Integer> lastClearedRows = new ArrayList<>();
    private final List<Color[]> lastClearedColors = new ArrayList<>();

    public Board() {}

    public Color getColor(int r, int c) {
        return board[r][c];
    }

    public int getTotalClearedLines() {
        return totalClearedLines;
    }

    public int getRows() {
        return ROWS;
    }

    public int getCols() {
        return COLS;
    }


    // --- 衝突判定 ---
    public boolean canMove(Tetromino t, int dRow, int dCol) {
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (t.getShape()[r][c] == 1) {
                    int newR = t.getRow() + r + dRow;
                    int newC = t.getCol() + c + dCol;

                    if (newC < 0 || newC >= COLS) {
                        return false;
                    }
                    if (newR >= ROWS) {
                        return false;
                    }
                    if (newR >= 0 && board[newR][newC] != null) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public boolean canMoveDown(Tetromino t) {
        return canMove(t, 1, 0);
    }


    // --- 固定 ---
    public void fixToBoard(Tetromino t) {
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (t.getShape()[r][c] == 1) {
                    int br = t.getRow() + r;
                    int bc = t.getCol() + c;
                    if (br < 0 || br >= ROWS || bc < 0 || bc >= COLS) continue;
                    board[br][bc] = t.getColor();
                }
            }
        }
    }

    /** 固定ブロック全体を90°回転させる（右回転） */
    public void rotateClockwise() {

        Color[][] rotated = new Color[ROWS][COLS];

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (board[r][c] != null) {
                    rotated[c][(ROWS - 1) - r] = board[r][c];
                }
            }
        }

        // CW回転後、右端の列(col=24)は元のrow0（スポーン行＝常に空）に対応するため必ず空になる。
        // 全体を1列右シフトして空き列を左端に移すことで右端の隙間をなくす。
        // col24（元row0の内容＝常に0）は切り捨てられるがデータ損失はない。
        board = new Color[ROWS][COLS];
        for (int r = 0; r < ROWS; r++) {
            for (int c = 1; c < COLS; c++) {
                board[r][c] = rotated[r][c - 1];
            }
            // board[r][0] = null（デフォルト初期化済み）
        }

    }

    // --- ライン消去 ---
    private boolean isLineFull(int row) {
        for (int c = 0; c < COLS; c++) {
            if (board[row][c] == null) {
                return false;
            }
        }
        return true;
    }

    private void clearLine(int row) {
        for (int r = row; r > 0; r--) {
            System.arraycopy(board[r - 1], 0, board[r], 0, COLS);
        }
        for (int c = 0; c < COLS; c++) {
            board[0][c] = null;
        }
    }

    public int clearCompletedLines() {
        lastClearedRows.clear();
        lastClearedColors.clear();
        int count = 0;
        for (int r = ROWS - 1; r >= 0; r--) {
            if (isLineFull(r)) {
                lastClearedRows.add(r);
                lastClearedColors.add(Arrays.copyOf(board[r], COLS));
                clearLine(r);
                count++;
                r++;
            }
        }
        totalClearedLines += count;
        return count;
    }

    public void pollLastClearedLines(List<Integer> outRows, List<Color[]> outColors) {
        outRows.addAll(lastClearedRows);
        outColors.addAll(lastClearedColors);
        lastClearedRows.clear();
        lastClearedColors.clear();
    }

    public boolean isBlocked(int row, int col) {
        if (row < 0 || row >= ROWS || col < 0 || col >= COLS) return true;
        return board[row][col] != null;
    }

    public boolean canPlace(int[][] shape, int row, int col) {
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (shape[r][c] == 1) {
                    int br = row + r;
                    int bc = col + c;

                    if (br >= ROWS || bc < 0 || bc >= COLS) {
                        return false;
                    }

                    if (br >= 0 && board[br][bc] != null) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
