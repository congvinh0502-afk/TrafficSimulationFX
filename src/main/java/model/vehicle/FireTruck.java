package model.vehicle;

import javafx.scene.paint.Color;

public class FireTruck extends EmergencyVehicle {
    public FireTruck(double x, double y, double angle) {
        super(x, y, angle);
        this.width = 65;
        this.height = 25;
        this.color = Color.web("#c0392b");
        this.speed = 4.0;
        this.maxSpeed = 4.0; 
    }
}