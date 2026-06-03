package tetris.view;

import javafx.scene.paint.Color;

public class Particle {
    public double x, y;
    public double vx, vy;
    public final Color color;
    public int life;
    public final int maxLife;

    public Particle(double x, double y, double vx, double vy, Color color, int life) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.color = color;
        this.life = this.maxLife = life;
    }

    public void update() {
        x += vx;
        y += vy;
        vy += 0.25;
        vx *= 0.96;
        life--;
    }

    public boolean isDead() {
        return life <= 0;
    }

    public double alpha() {
        return (double) life / maxLife;
    }
}
