package tetris.model;

import javafx.scene.paint.Color;

public class Tetromino {

    private final ShapeType type;
    private int[][] shape;
    private int row;
    private int col;
    private int rotation;
    private final Color color;

    public Tetromino(ShapeType type) {
        this.type = type;
        this.shape = type.getShape();
        this.color = type.getColor();
        this.row = 0;
        this.col = 3;
        this.rotation = 0;
    }

    public ShapeType getType()   { return type; }
    public int[][] getShape()    { return shape; }
    public int getRow()          { return row; }
    public int getCol()          { return col; }
    public int getRotation()     { return rotation; }
    public Color getColor()      { return color; }

    public void setShape(int[][] shape) { this.shape = shape; }
    public void setRow(int row)         { this.row = row; }
    public void setCol(int col)         { this.col = col; }
    public void setRotation(int rot)    { this.rotation = rot; }

    public Tetromino copy() {
        Tetromino t = new Tetromino(this.type);
        t.row = this.row;
        t.col = this.col;
        t.rotation = this.rotation;
        t.shape = deepCopy(this.shape);
        return t;
    }

    public int countBlocks() {
        int count = 0;
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (this.shape[r][c] == 1) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int[][] deepCopy(int[][] src) {
        int[][] dst = new int[src.length][];
        for (int i = 0; i < src.length; i++) {
            dst[i] = src[i].clone();
        }
        return dst;
    }
}
