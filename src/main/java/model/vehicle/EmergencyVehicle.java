package model.vehicle;

import javafx.scene.paint.Color;

public class EmergencyVehicle extends Vehicle {
    public EmergencyVehicle(double x, double y, double angle) {
        super(x, y, angle);
        this.width = 45;
        this.height = 22;
        this.color = Color.web("#ecf0f1");
        this.speed = config.Constants.BASE_AMBULANCE;
        this.maxSpeed = config.Constants.BASE_AMBULANCE;
    }
}