package tetris.view;

import java.util.List;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import tetris.model.Board;
import tetris.model.Tetromino;

public class Render {

    private final int cellSize;
    private final double offsetX;
    private final double offsetY;

    private static final Color EMPTY_CELL_COLOR = Color.rgb(0, 0, 0, 0.25);
    private static final Color GRID_LINE_COLOR  = Color.rgb(255, 255, 255, 0.10);

    public Render(int cellSize, double canvasWidth, double canvasHeight) {
        this.cellSize = cellSize;
        this.offsetX = (canvasWidth  - (double) cellSize * Board.COLS) / 2.0;
        this.offsetY = (canvasHeight - (double) cellSize * Board.ROWS) / 2.0;
    }

    public int getCellSize() {
        return cellSize;
    }

    // 盤面の実描画範囲。Canvas は正方形でもセルサイズの切り捨てぶん余るので、
    // 枠線をこの矩形へ揃えるために公開する（DeviceFramePane.alignFrame）
    public double getBoardX() { return offsetX; }
    public double getBoardY() { return offsetY; }
    public double getBoardWidth()  { return (double) cellSize * Board.COLS; }
    public double getBoardHeight() { return (double) cellSize * Board.ROWS; }

    /** 盤面の列番号 → Canvas 上の X 座標（センタリングオフセット込み） */
    public double toPixelX(int col) {
        return offsetX + col * cellSize;
    }

    /** 盤面の行番号 → Canvas 上の Y 座標（センタリングオフセット込み） */
    public double toPixelY(int row) {
        return offsetY + row * cellSize;
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
    //     プレビュー描画（NEXT / HOLD）
    // ===========================

    /** プレビュー枠に入れるセルサイズの上限。大きくしすぎると盤面のミノより目立つ */
    private static final int PREVIEW_MAX_CELL = 56;
    /** プレビューが想定するミノの最大寸法（I ミノの 4×1、O ミノの 2×2） */
    private static final int PREVIEW_GRID_COLS = 4;
    private static final int PREVIEW_GRID_ROWS = 2;

    /**
     * 指定した枠の中央にミノを描く。Canvas は毎回クリアする。
     *
     * セルサイズは盤面のものを使わず枠から毎回決める。盤面のセル（33px）のままだと
     * プレビュー枠に対して小さすぎて形が読み取れないため。ミノごとに大きさが変わって
     * 見えないよう、基準は常に 4×2 のグリッドに取り、それを超える形（回転済みの
     * HOLD など）のときだけ実寸に合わせて縮める。
     */
    public void drawPreview(GraphicsContext gc, Tetromino piece,
                            double areaX, double areaY, double areaW, double areaH,
                            boolean grayed) {
        gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());

        if (piece == null || areaW <= 0 || areaH <= 0) return;

        int[][] shape = piece.getShape();
        int minR = 4, maxR = -1, minC = 4, maxC = -1;
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
        if (maxR < minR) return; // 空の形状

        int shapeCols = maxC - minC + 1;
        int shapeRows = maxR - minR + 1;
        int gridCols = Math.max(PREVIEW_GRID_COLS, shapeCols);
        int gridRows = Math.max(PREVIEW_GRID_ROWS, shapeRows);

        int cell = (int) Math.min(PREVIEW_MAX_CELL,
                Math.min(areaW / gridCols, areaH / gridRows));
        if (cell < 4) return;

        double baseX = areaX + (areaW - (double) shapeCols * cell) / 2.0;
        double baseY = areaY + (areaH - (double) shapeRows * cell) / 2.0;

        Color base = grayed ? Color.GRAY : piece.getColor();
        for (int r = minR; r <= maxR; r++) {
            for (int c = minC; c <= maxC; c++) {
                if (shape[r][c] == 1) {
                    drawCellAt(gc,
                        baseX + (c - minC) * (double) cell,
                        baseY + (r - minR) * (double) cell,
                        base, cell);
                }
            }
        }
    }

    // ===========================
    //         盤面描画
    // ===========================
    public void drawBoard(GraphicsContext gc, Board board) {
        // 空セルは薄い半透明にして背景画像（playfield-bg.png）を透かす
        gc.setFill(EMPTY_CELL_COLOR);
        gc.fillRect(offsetX, offsetY,
                (double) cellSize * Board.COLS, (double) cellSize * Board.ROWS);

        // 薄いグリッド線で視認性を確保
        gc.setStroke(GRID_LINE_COLOR);
        gc.setLineWidth(1);
        for (int r = 0; r <= Board.ROWS; r++) {
            gc.strokeLine(toPixelX(0), toPixelY(r), toPixelX(Board.COLS), toPixelY(r));
        }
        for (int c = 0; c <= Board.COLS; c++) {
            gc.strokeLine(toPixelX(c), toPixelY(0), toPixelX(c), toPixelY(Board.ROWS));
        }

        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                Color color = board.getColor(r, c);
                if (color != null) {
                    drawCell(gc, r, c, color);
                }
            }
        }
    }

    // ===========================
    //       現在ミノ描画
    // ===========================
    public void drawTetromino(GraphicsContext gc, Tetromino t) {
        if (t == null) return;

        int[][] shape = t.getShape();
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (shape[r][c] == 1) {
                    drawCell(gc, t.getRow() + r, t.getCol() + c, t.getColor());
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
                    gc.strokeRect(
                        toPixelX(g.getCol() + c) + 1.5,
                        toPixelY(g.getRow() + r) + 1.5,
                        cellSize - 4,
                        cellSize - 4);
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

    // 汎用セル描画（盤面座標）
    private void drawCell(GraphicsContext gc, int r, int c, Color base) {
        drawCellAt(gc, toPixelX(c), toPixelY(r), base, cellSize);
    }

    // 汎用セル描画（ピクセル座標）: 本体 → 上左ハイライト → 下右シャドウ の3層ベベル
    private void drawCellAt(GraphicsContext gc, double x, double y, Color base, int size) {
        double s = size - 1;
        double edge = Math.max(2, size * 0.12);

        gc.setFill(base);
        gc.fillRect(x, y, s, s);

        gc.setFill(base.deriveColor(0, 1.0, 1.45, 1.0));
        gc.fillRect(x, y, s, edge);
        gc.fillRect(x, y, edge, s);

        gc.setFill(base.deriveColor(0, 1.0, 0.55, 1.0));
        gc.fillRect(x, y + s - edge, s, edge);
        gc.fillRect(x + s - edge, y, edge, s);
    }
}
