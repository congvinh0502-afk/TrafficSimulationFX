package model;

import javafx.scene.paint.Color;

public class Pedestrian {
    private double x, y, angle, speed;
    private double startX, startY, endX, endY;
    private int animStep = 0;
    private final Color shirtColor;

    private static final Color[] SHIRT_COLORS = {
        Color.web("#e74c3c"), Color.web("#3498db"), Color.web("#2ecc71"),
        Color.web("#9b59b6"), Color.web("#f39c12"), Color.web("#1abc9c"),
        Color.web("#e91e63"), Color.web("#ff5722"), Color.web("#00bcd4")
    };

    public Pedestrian(double sx, double sy, double ex, double ey, int colorIdx) {
        this.startX = sx; this.startY = sy;
        this.endX   = ex; this.endY   = ey;
        this.x = sx; this.y = sy;
        this.speed = 0.28 + (colorIdx % 4) * 0.07;
        this.shirtColor = SHIRT_COLORS[Math.abs(colorIdx) % SHIRT_COLORS.length];
        this.angle = Math.toDegrees(Math.atan2(ey - sy, ex - sx));
    }

    public void update() {
        animStep++;
        double dx = endX - x, dy = endY - y;
        if (Math.hypot(dx, dy) < 4) {
            double tx = startX, ty = startY;
            startX = endX; startY = endY;
            endX = tx; endY = ty;
            angle = Math.toDegrees(Math.atan2(endY - startY, endX - startX));
        } else {
            double rad = Math.toRadians(angle);
            x += speed * Math.cos(rad);
            y += speed * Math.sin(rad);
        }
    }

    public double getX()          { return x; }
    public double getY()          { return y; }
    public double getAngle()      { return angle; }
    public int    getAnimStep()   { return animStep; }
    public Color  getShirtColor() { return shirtColor; }
}
