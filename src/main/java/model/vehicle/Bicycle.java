package model.vehicle;

import javafx.scene.paint.Color;

public class Bicycle extends Vehicle {
    public Bicycle(double x, double y, double angle) {
        super(x, y, angle);
        this.width = 15;  
        this.height = 6;  
        this.color = Color.web("#2ecc71"); // Xe đạp màu xanh lá
        this.speed = 1.2; // Đi chậm
    }
}