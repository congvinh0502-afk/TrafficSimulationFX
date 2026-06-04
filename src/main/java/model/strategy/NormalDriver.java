package model.strategy;

public class NormalDriver implements DrivingStrategy {
    @Override
    public double calculateMaxSpeed(double baseSpeed) {
        return baseSpeed; 
    }

    @Override
    public boolean obeysTrafficLight() {
        return true; 
    }

    @Override
    public double getSafeGap() {
        return config.Constants.IS_RAINING ? 90 : 50; 
    }
}