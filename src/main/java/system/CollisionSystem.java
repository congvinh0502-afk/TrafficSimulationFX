package system;

import java.util.List;
import model.vehicle.Vehicle;

public class CollisionSystem {
    private static final double SAFE_DISTANCE = 50; 

    public void update(List<Vehicle> vehicles) {
        
        // [PHA 1] XE CỨU THƯƠNG PHÁT SÓNG "DẸP ĐƯỜNG"
        for (Vehicle current : vehicles) {
            if (current instanceof model.vehicle.EmergencyVehicle) {
                for (Vehicle other : vehicles) {
                    // Không quét chính mình hoặc các xe cứu thương khác
                    if (current == other || other instanceof model.vehicle.EmergencyVehicle) continue;
                    
                    // Quét các xe chạy cùng hướng
                    if (Math.abs(current.getAngle() - other.getAngle()) < 20) {
                        double dx = other.getX() - current.getX();
                        double dy = other.getY() - current.getY();
                        double rad = Math.toRadians(current.getAngle());
                        double dirX = Math.cos(rad);
                        double dirY = Math.sin(rad);

                        double axialDist = dx * dirX + dy * dirY;
                        double lateralDist = Math.abs(dx * (-dirY) + dy * dirX);

                        // Nếu xe thường đang ở PHÍA TRƯỚC (trong 250px) và cùng làn đường
                        if (axialDist > 0 && axialDist < 250 && lateralDist < 40) {
                            other.setYieldTimer(60); // Bật trạng thái dạt lề trong 60 frame (~1 giây)
                        }
                    }
                }
            }
        }

        // [PHA 2] LOGIC CHỐNG ĐÈ XE BÌNH THƯỜNG
        for (Vehicle current : vehicles) {
            Vehicle blocker = null;
            double minDist = 120; 

            for (Vehicle other : vehicles) {
                if (current == other) continue;

                if (Math.abs(current.getAngle() - other.getAngle()) < 10) {
                    double dx = other.getX() - current.getX();
                    double dy = other.getY() - current.getY();
                    double rad = Math.toRadians(current.getAngle());
                    double dirX = Math.cos(rad);
                    double dirY = Math.sin(rad);

                    double axialDist = dx * dirX + dy * dirY;
                    double lateralDist = Math.abs(dx * (-dirY) + dy * dirX);

                    // ĐẶC QUYỀN: Xe cứu thương nhắm mắt ngó lơ, không phanh lại sau đuôi những xe đang dạt lề!
                    if (current instanceof model.vehicle.EmergencyVehicle && other.getYieldTimer() > 0) {
                        continue; 
                    }

                    if (axialDist > 0 && axialDist < minDist && lateralDist < 25) {
                        minDist = axialDist;
                        blocker = other;
                    }
                }
            }

            if (blocker != null) {
                double gap = minDist - (current.getWidth() / 2 + blocker.getWidth() / 2);
                
                // MỚI: Mỗi tài xế có một giới hạn giữ khoảng cách an toàn khác nhau
                double safeDist = current.getStrategy().getSafeGap();
                
                if (gap < 8) {
                    current.setSpeed(0); 
                } else if (gap < safeDist) { // Thay vì SAFE_DISTANCE cứng nhắc
                    current.setSpeed(Math.min(current.getSpeed(), blocker.getSpeed()));
                    
                    // ---> THUẬT TOÁN VƯỢT XE: Nếu bị chậm lại, thử tìm cách chuyển làn!
                    // Chỉ chuyển khi đang đi thẳng, chưa chuyển làn, và không phải đang nhường đường xe cứu thương
                    if (!current.isTurning() && !current.isChangingLane() && current.getYieldTimer() == 0) {
                        
                        double currentOffset = current.getLaneOffset();
                        // Tính làn mục tiêu (Nếu đang ở 45 thì sang 15, và ngược lại)
                        double targetOffset = (currentOffset == 45) ? 15 : 45;
                        // Hướng trượt ngang: 45->15 là sang Trái (-1), 15->45 là sang Phải (1)
                        double direction = (currentOffset == 45) ? -1 : 1; 
                        
                        boolean canChange = true;
                        
                        // Quét xem làn bên kia có xe nào đang chắn không?
                        for (Vehicle check : vehicles) {
                            if (check == current) continue;
                            if (Math.abs(current.getAngle() - check.getAngle()) < 10) { // Cùng hướng
                                // Nếu xe kia đang ở làn mục tiêu, hoặc cũng đang chuyển vào làn đó
                                if (check.getLaneOffset() == targetOffset || check.isChangingLane()) {
                                    double dx2 = check.getX() - current.getX();
                                    double dy2 = check.getY() - current.getY();
                                    double rad2 = Math.toRadians(current.getAngle());
                                    // Chiếu lên trục dọc để xem khoảng cách
                                    double axialDist2 = dx2 * Math.cos(rad2) + dy2 * Math.sin(rad2);
                                    
                                    // Yêu cầu khoảng trống an toàn: 60px phía sau và 100px phía trước mặt
                                    if (axialDist2 > -60 && axialDist2 < 100) {
                                        canChange = false; // Có xe cản -> Hủy ý định vượt
                                        break;
                                    }
                                }
                            }
                        }
                        
                        // Làn bên cạnh hoàn toàn trống -> Quyết định đánh lái vượt!
                        if (canChange) {
                            current.setChangingLane(true);
                            current.setLaneChangeProgress(0);
                            current.setLaneChangeDirection(direction);
                            current.setLaneOffset(targetOffset); // "Xí" chỗ làn mới ngay lập tức
                            util.SoundManager.playSignal(); // 🔊 BẬT XI-NHAN
                        }
                    }
                }
            } else {
                // Tốc độ cơ sở × hệ số từ thanh trượt UI
                double baseSpeed = config.Constants.BASE_CAR * config.Constants.SPEED_CAR;
                if (current instanceof model.vehicle.FireTruck)
                    baseSpeed = config.Constants.BASE_FIRETRUCK * config.Constants.SPEED_FIRETRUCK;
                else if (current instanceof model.vehicle.EmergencyVehicle)
                    baseSpeed = config.Constants.BASE_AMBULANCE * config.Constants.SPEED_AMBULANCE;
                else if (current instanceof model.vehicle.Motorbike)
                    baseSpeed = config.Constants.BASE_MOTORBIKE * config.Constants.SPEED_MOTORBIKE;
                else if (current instanceof model.vehicle.Bicycle)
                    baseSpeed = config.Constants.BASE_BICYCLE * config.Constants.SPEED_BICYCLE;

                // 2. Chuyển qua cho "Bộ não" tính toán xem tài xế này muốn phóng bao nhiêu
                double maxSpeed = current.getStrategy().calculateMaxSpeed(baseSpeed);
                
                // 3. Thực hiện tăng ga từ từ
                if (current.getSpeed() < maxSpeed) {
                    current.setSpeed(Math.min(maxSpeed, current.getSpeed() + 0.15));
                }
            }
        }
    }
}