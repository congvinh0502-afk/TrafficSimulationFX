package model.strategy;

public class EmergencyDriver implements DrivingStrategy {
    @Override
    public double calculateMaxSpeed(double baseSpeed) {
        return baseSpeed * 1.5; 
    }

    @Override
    public boolean obeysTrafficLight() {
        return false; // Lệnh bài miễn tử: Bỏ qua đèn giao thông!
    }

    @Override
    public double getSafeGap() {
        return 10; // Không có thời gian để giữ khoảng cách
    }
}