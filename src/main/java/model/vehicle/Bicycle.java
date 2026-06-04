package model.vehicle;

import javafx.scene.paint.Color;

public class Bicycle extends Vehicle {
    public Bicycle(double x, double y, double angle) {
        super(x, y, angle);
        this.width = 15;
        this.height = 6;
        this.color = Color.web("#2ecc71");
        this.speed = config.Constants.BASE_BICYCLE;
        this.maxSpeed = config.Constants.BASE_BICYCLE;
    }
}