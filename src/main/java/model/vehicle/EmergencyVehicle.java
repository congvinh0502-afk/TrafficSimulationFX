package model.vehicle;

import javafx.scene.paint.Color;

public class EmergencyVehicle extends Vehicle {
    public EmergencyVehicle(double x, double y, double angle) {
        super(x, y, angle);
        this.width = 45;
        this.height = 22;
        this.color = Color.web("#ecf0f1");
        this.speed = 4.5;
        this.maxSpeed = 4.5;
    }
}