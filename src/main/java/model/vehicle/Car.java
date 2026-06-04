package model.vehicle;

import javafx.scene.paint.Color;

public class Car extends Vehicle {
    public Car(double x, double y, double angle) {
        super(x, y, angle);
        this.width = 40;  // Chiều dài vật lý của xe
        this.height = 20; // Chiều rộng vật lý của xe
        this.color = Color.web("#e74c3c"); // Xe ô tô màu đỏ rực rỡ
    }
}