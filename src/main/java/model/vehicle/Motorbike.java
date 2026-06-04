package model.vehicle;

import javafx.scene.paint.Color;

public class Motorbike extends Vehicle {
    public Motorbike(double x, double y, double angle) {
        super(x, y, angle);
        this.width = 18;  // Ngắn hơn ô tô
        this.height = 10; // Nhỏ bé hơn
        this.color = Color.web("#f1c40f"); // Màu vàng cam nổi bật
        this.speed = 3.5; // Lanh lẹ hơn ô tô (3.0)
    }
}