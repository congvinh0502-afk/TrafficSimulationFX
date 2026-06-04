package model.strategy;

public interface DrivingStrategy {
    double calculateMaxSpeed(double baseSpeed); // Tốc độ tối đa muốn đi
    boolean obeysTrafficLight();                // Có tuân thủ đèn giao thông không?
    double getSafeGap();                        // Khoảng cách an toàn với xe phía trước
}