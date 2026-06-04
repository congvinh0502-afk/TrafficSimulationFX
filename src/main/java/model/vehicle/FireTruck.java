package model.vehicle;

import javafx.scene.paint.Color;

public class FireTruck extends EmergencyVehicle {
    public FireTruck(double x, double y, double angle) {
        super(x, y, angle);
        this.width = 65;  // To và dài nhất
        this.height = 25;
        this.color = Color.web("#c0392b"); // Đỏ sẫm
        this.speed = 4.0; 
    }
}