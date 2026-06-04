package model.vehicle;

import javafx.scene.paint.Color;

public class Car extends Vehicle {
    // Palette màu xe đa dạng (đỏ, cam, xanh dương, tím, xanh lá)
    private static final Color[] CAR_COLORS = {
        Color.web("#e74c3c"), // đỏ
        Color.web("#e67e22"), // cam
        Color.web("#3498db"), // xanh dương
        Color.web("#9b59b6"), // tím
        Color.web("#1abc9c"), // xanh ngọc
        Color.web("#e91e63"), // hồng
        Color.web("#ff9800"), // vàng cam
        Color.web("#607d8b"), // xám xanh
    };
    private static int colorIndex = 0;

    public Car(double x, double y, double angle) {
        super(x, y, angle);
        this.width = 40;
        this.height = 20;
        this.color = CAR_COLORS[colorIndex % CAR_COLORS.length];
        colorIndex++;
        this.speed = config.Constants.BASE_CAR;
        this.maxSpeed = config.Constants.BASE_CAR;
    }
}
