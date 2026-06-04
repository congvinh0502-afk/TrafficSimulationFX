package model.vehicle;

import javafx.scene.paint.Color;
import util.TurnType;
import java.util.List;

public abstract class Vehicle {
    protected double x, y;
    protected double width, height;
    protected double speed;
    protected double maxSpeed = 3.0; // Tốc độ tối đa
    protected double angle; 
    protected Color color;

    // --- BIẾN BEZIER (RẼ CUA) ---
    protected boolean isTurning = false;  
    protected TurnType turnType = TurnType.STRAIGHT; 
    protected double bezierT = 0;         
    protected double p0x, p0y, p1x, p1y, p2x, p2y; 
    
    // --- BIẾN NHƯỜNG ĐƯỜNG & CHUYỂN LÀN ---
    protected int yieldTimer = 0; 
    protected boolean isChangingLane = false;
    protected double laneChangeProgress = 0;  
    protected double laneChangeDirection = 0; 
    protected double laneOffset = 45; 
    
    // --- TRẠNG THÁI & STRATEGY ---
    protected boolean isBroken = false;
    protected model.strategy.DrivingStrategy strategy;

    public Vehicle(double x, double y, double angle) {
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.speed = maxSpeed; 
    }

    // =======================================================
    // BỘ NÃO DI CHUYỂN (TỰ ĐỘNG TÍNH TOÁN TỌA ĐỘ)
    // =======================================================
    public void update(List<Vehicle> allVehicles) {
        if (isBroken) {
            this.speed = 0;
            return;
        }

        // 1. ĐANG BỊ ÉP NHƯỜNG ĐƯỜNG (Dạt sang 1 bên và giảm tốc)
        if (yieldTimer > 0 && !isTurning) {
            yieldTimer--;
            double swerveDirection = (laneOffset >= 30) ? 90 : -90;
            double swerveRad = Math.toRadians(angle + swerveDirection);
            this.x += Math.cos(swerveRad) * 0.4;
            this.y += Math.sin(swerveRad) * 0.4;
            this.speed = Math.max(0, this.speed - 0.15); // Phanh từ từ
        }
        // 2. ĐANG RẼ TẠI NGÃ TƯ (Chạy theo đường cong Bezier)
        else if (isTurning) {
            double step = 0.02; // Tốc độ rẽ (Càng nhỏ càng mượt)
            bezierT += step;
            if (bezierT >= 1.0) {
                isTurning = false;
                this.x = p2x;
                this.y = p2y;
                // Chuẩn hóa góc quay về 0-360 độ
                double finalAngle = Math.toDegrees(Math.atan2(p2y - p1y, p2x - p1x));
                this.angle = (finalAngle + 360) % 360;
            } else {
                double oneMinusT = 1 - bezierT;
                this.x = oneMinusT * oneMinusT * p0x + 2 * oneMinusT * bezierT * p1x + bezierT * bezierT * p2x;
                this.y = oneMinusT * oneMinusT * p0y + 2 * oneMinusT * bezierT * p1y + bezierT * bezierT * p2y;
                // Xe tự động xoay đầu theo đường cong
                this.angle = Math.toDegrees(Math.atan2(
                    2 * oneMinusT * (p1y - p0y) + 2 * bezierT * (p2y - p1y),
                    2 * oneMinusT * (p1x - p0x) + 2 * bezierT * (p2x - p1x)
                ));
            }
        }
        // 3. ĐI THẲNG BÌNH THƯỜNG & PHANH DỰ ĐOÁN
        else {
            // Radar quét xe phía trước
            Vehicle frontVehicle = detectVehicleAhead(allVehicles, 100);
            if (frontVehicle != null) {
                double dist = Math.hypot(frontVehicle.getX() - this.x, frontVehicle.getY() - this.y);
                if (dist < 80) {
                    this.speed = Math.max(0, this.speed * 0.95); // Rà phanh mượt
                }
            } else {
                if (this.speed < maxSpeed) this.speed += 0.05; // Tăng tốc dần
            }

            // Logic chuyển làn lạng lách (nếu có)
            if (isChangingLane) {
                double slideSpeed = 1.0; 
                laneChangeProgress += slideSpeed;
                double perpRad = Math.toRadians(angle) + (laneChangeDirection * Math.PI / 2);
                this.x += slideSpeed * Math.cos(perpRad);
                this.y += slideSpeed * Math.sin(perpRad);
                if (laneChangeProgress >= 30) isChangingLane = false;
            }

            // Tiến lên phía trước theo góc hiện tại
            double angleRad = Math.toRadians(angle);
            this.x += speed * Math.cos(angleRad);
            this.y += speed * Math.sin(angleRad);
        }
    }

    // Hàm Radar siêu nhẹ, chỉ quét các xe ở gần
    private Vehicle detectVehicleAhead(List<Vehicle> allVehicles, double range) {
        for (Vehicle v : allVehicles) {
            if (v == this || v.isBroken()) continue;
            double dist = Math.hypot(v.getX() - this.x, v.getY() - this.y);
            // Xe phải ở phía trước (khoảng cách nhỏ hơn range) và nằm cùng làn (để tránh quét nhầm làn ngược chiều)
            if (dist < range) {
                // Tính góc tương đối để chắc chắn xe kia đang ở trước mặt
                double angleToTarget = Math.toDegrees(Math.atan2(v.getY() - this.y, v.getX() - this.x));
                double angleDiff = Math.abs((angleToTarget - this.angle + 360) % 360);
                if (angleDiff < 45 || angleDiff > 315) return v;
            }
        }
        return null;
    }

    // =======================================================
    // CÁC HÀM GETTER / SETTER
    // =======================================================
    public double getX() { return x; } public void setX(double x) { this.x = x; }
    public double getY() { return y; } public void setY(double y) { this.y = y; }
    public double getWidth() { return width; } public double getHeight() { return height; }
    public double getAngle() { return angle; } public void setAngle(double angle) { this.angle = angle; }
    public Color getColor() { return color; }
    public double getSpeed() { return speed; } public void setSpeed(double speed) { this.speed = speed; }
    public boolean isTurning() { return isTurning; } public void setTurning(boolean isTurning) { this.isTurning = isTurning; }
    public TurnType getTurnType() { return turnType; } public void setTurnType(TurnType turnType) { this.turnType = turnType; }
    public double getBezierT() { return bezierT; } public void setBezierT(double bezierT) { this.bezierT = bezierT; }
    public void setBezierPoints(double p0x, double p0y, double p1x, double p1y, double p2x, double p2y) {
        this.p0x = p0x; this.p0y = p0y; this.p1x = p1x; this.p1y = p1y; this.p2x = p2x; this.p2y = p2y;
    }
    public double getP0x() { return p0x; } public double getP0y() { return p0y; }
    public double getP1x() { return p1x; } public double getP1y() { return p1y; }
    public double getP2x() { return p2x; } public double getP2y() { return p2y; }
    public int getYieldTimer() { return yieldTimer; } public void setYieldTimer(int yieldTimer) { this.yieldTimer = yieldTimer; }
    public boolean isChangingLane() { return isChangingLane; } public void setChangingLane(boolean isChangingLane) { this.isChangingLane = isChangingLane; }
    public double getLaneChangeProgress() { return laneChangeProgress; } public void setLaneChangeProgress(double laneChangeProgress) { this.laneChangeProgress = laneChangeProgress; }
    public double getLaneChangeDirection() { return laneChangeDirection; } public void setLaneChangeDirection(double laneChangeDirection) { this.laneChangeDirection = laneChangeDirection; }
    public double getLaneOffset() { return laneOffset; } public void setLaneOffset(double laneOffset) { this.laneOffset = laneOffset; }
    public boolean isBroken() { return isBroken; } public void setBroken(boolean broken) { this.isBroken = broken; }
    public model.strategy.DrivingStrategy getStrategy() { return strategy; } public void setStrategy(model.strategy.DrivingStrategy strategy) { this.strategy = strategy; }
}