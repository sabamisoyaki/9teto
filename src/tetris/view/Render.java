package tetris.view;

import java.util.List;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import tetris.model.Board;
import tetris.model.Tetromino;

public class Render {

    private final int cellSize;

    public Render(int cellSize) {
        this.cellSize = cellSize;
    }

    public int getCellSize() {
        return cellSize;
    }

    // ===========================
    //     全描画まとめ
    // ===========================
    public void drawAll(GraphicsContext gc, Board board, Tetromino current, Tetromino ghost) {
        gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
        drawBoard(gc, board);
        drawGhost(gc, ghost);
        drawTetromino(gc, current);
    }

    // ===========================
    //        Nextミノ描画
    // ===========================
    public void drawNext(GraphicsContext gc, Tetromino next, int offsetX, int offsetY) {
        drawNext(gc, next, offsetX, offsetY, false);
    }

    public void drawNext(GraphicsContext gc, Tetromino next, int offsetX, int offsetY, boolean grayed) {
        gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());

        if (next == null) return;

        gc.setFill(grayed ? Color.GRAY : next.getColor());

        int[][] shape = next.getShape();
        int minR = 4, maxR = 0, minC = 4, maxC = 0;

        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (shape[r][c] == 1) {
                    minR = Math.min(minR, r);
                    maxR = Math.max(maxR, r);
                    minC = Math.min(minC, c);
                    maxC = Math.max(maxC, c);
                }
            }
        }

        int shapeWidth  = (maxC - minC + 1);
        int shapeHeight = (maxR - minR + 1);

        int baseX = offsetX + (int) ((gc.getCanvas().getWidth() - offsetX - shapeWidth * cellSize) / 2);
        int baseY = offsetY + (int) ((gc.getCanvas().getHeight() - offsetY - shapeHeight * cellSize) / 2);

        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (shape[r][c] == 1) {
                    gc.fillRect(
                        baseX + (c - minC) * cellSize,
                        baseY + (r - minR) * cellSize,
                        cellSize - 1,
                        cellSize - 1
                    );
                }
            }
        }
    }

    // ===========================
    //         盤面描画
    // ===========================
    public void drawBoard(GraphicsContext gc, Board board) {
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                Color color = board.getColor(r, c);
                gc.setFill(color != null ? color : Color.BLACK);
                gc.fillRect(
                    c * cellSize,
                    r * cellSize,
                    cellSize - 1,
                    cellSize - 1
                );
            }
        }
    }

    // ===========================
    //       現在ミノ描画
    // ===========================
    public void drawTetromino(GraphicsContext gc, Tetromino t) {
        if (t == null) return;

        gc.setFill(t.getColor());

        int[][] shape = t.getShape();
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (shape[r][c] == 1) {
                    drawCell(gc, t.getRow() + r, t.getCol() + c);
                }
            }
        }
    }


    // ===========================
    //       ゴースト描画（輪郭のみ）
    // ===========================
    public void drawGhost(GraphicsContext gc, Tetromino g) {
        if (g == null) return;

        Color ghostColor = g.getColor().deriveColor(0, 0.8, 1.4, 0.75);
        gc.setStroke(ghostColor);
        gc.setLineWidth(2.0);

        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (g.getShape()[r][c] == 1) {
                    int x = (g.getCol() + c) * cellSize;
                    int y = (g.getRow() + r) * cellSize;
                    gc.strokeRect(x + 1, y + 1, cellSize - 3, cellSize - 3);
                }
            }
        }
    }

    // ===========================
    //      パーティクル描画
    // ===========================
    public void drawParticles(GraphicsContext gc, List<Particle> particles) {
        for (Particle p : particles) {
            double alpha = p.alpha();
            double size = cellSize * 0.15 * alpha + 1.5;
            gc.setGlobalAlpha(alpha);
            gc.setFill(p.color);
            gc.fillRect(p.x - size / 2, p.y - size / 2, size, size);
        }
        gc.setGlobalAlpha(1.0);
    }

    // 汎用セル描画
    private void drawCell(GraphicsContext gc, int r, int c) {
        gc.fillRect(
            c * cellSize,
            r * cellSize,
            cellSize - 1,
            cellSize - 1
        );
    }
}
