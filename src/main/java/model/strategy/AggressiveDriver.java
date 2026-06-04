package model.strategy;

public class AggressiveDriver implements DrivingStrategy {
    @Override
    public double calculateMaxSpeed(double baseSpeed) {
        return baseSpeed * 1.4; // Đạp ga phóng nhanh hơn 40% so với tốc độ thiết kế của xe
    }

    @Override
    public boolean obeysTrafficLight() {
        return true; // Vẫn sợ phạt nguội nên không dám vượt đèn đỏ
    }

    @Override
    public double getSafeGap() {
        return config.Constants.IS_RAINING ? 40 : 15; // Điền vào chỗ trống, bám đuôi sát sạt
    }
}