package model.vehicle;

import javafx.scene.paint.Color;

public class Motorbike extends Vehicle {
    public Motorbike(double x, double y, double angle) {
        super(x, y, angle);
        this.width = 18;
        this.height = 10;
        this.color = Color.web("#f1c40f");
        this.speed = config.Constants.BASE_MOTORBIKE;
        this.maxSpeed = config.Constants.BASE_MOTORBIKE;
    }
}