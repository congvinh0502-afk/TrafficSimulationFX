package model.vehicle;

import javafx.scene.paint.Color;

public class EmergencyVehicle extends Vehicle {
    public EmergencyVehicle(double x, double y, double angle) {
        super(x, y, angle);
        this.width = 45;  // Xe cứu thương dài hơn xe con một chút
        this.height = 22;
        this.color = Color.web("#ecf0f1"); // Màu trắng ngà
        this.speed = 4.5; // Chạy nhanh hơn các xe khác (Xe thường speed = 3.0)
    }
}