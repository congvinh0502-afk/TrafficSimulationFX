package model.vehicle;

import javafx.scene.paint.Color;

public class Bicycle extends Vehicle {
    public Bicycle(double x, double y, double angle) {
        super(x, y, angle);
        this.width = 15;
        this.height = 6;
        this.color = Color.web("#2ecc71");
        this.speed = 1.2;
        this.maxSpeed = 1.2; // Xe đạp không vượt 1.2
    }
}