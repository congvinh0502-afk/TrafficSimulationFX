package system;

import java.util.List;
import model.map.IntersectionNode;
import model.traffic.TrafficLight;
import model.vehicle.Vehicle;

public class TrafficRuleSystem {
    
    public void applyRules(List<Vehicle> vehicles, List<IntersectionNode> nodes) {
        for (Vehicle v : vehicles) {
            
            // ĐẶC QUYỀN: Hỏi xem bộ não của tài xế này có tuân thủ đèn giao thông không?
        if (!v.getStrategy().obeysTrafficLight()) {
            continue; 
        }
            IntersectionNode targetNode = null;
            double minDist = 180; 

            // 1. Tìm ngã tư gần nhất trước mặt
            for (IntersectionNode node : nodes) {
                double dx = node.getX() - v.getX();
                double dy = node.getY() - v.getY();
                
                double rad = Math.toRadians(v.getAngle());
                double dirX = Math.cos(rad);
                double dirY = Math.sin(rad);

                double axialDist = dx * dirX + dy * dirY; 
                double lateralDist = Math.abs(dx * (-dirY) + dy * dirX); 

                if (axialDist > 0 && axialDist < minDist && lateralDist < 100) {
                    minDist = axialDist;
                    targetNode = node;
                }
            }

            // Nếu đường thoáng hoặc là Ngã 5 (Bùng binh) thì cứ đi tiếp, không bắt dừng đèn
            if (targetNode == null || targetNode.getType() == IntersectionNode.NodeType.FIVE_WAY) continue; 

            TrafficLight lightToObey = null;
            
            // 2. Chuyển đổi góc (Angle) sang Hướng để nhìn đúng đèn
            double angle = v.getAngle() % 360;
            if (angle < 0) angle += 360;
            
            if (angle >= 315 || angle < 45) { // Góc ~0: Xe đi từ Tây sang Đông -> Nhìn đèn Tây
                lightToObey = targetNode.getLightWest();
            } else if (angle >= 45 && angle < 135) { // Góc ~90: Xe đi từ Bắc xuống Nam -> Nhìn đèn Bắc
                lightToObey = targetNode.getLightNorth();
            } else if (angle >= 135 && angle < 225) { // Góc ~180: Xe đi từ Đông sang Tây -> Nhìn đèn Đông
                lightToObey = targetNode.getLightEast();
            } else { // Góc ~270: Xe đi từ Nam lên Bắc -> Nhìn đèn Nam
                lightToObey = targetNode.getLightSouth();
            }

            if (lightToObey == null) continue;

            // 3. Phanh khi gặp đèn Đỏ hoặc Vàng
            if (lightToObey.getPhase() == TrafficLight.Phase.RED || lightToObey.getPhase() == TrafficLight.Phase.YELLOW) {
                if (minDist > 70 && minDist < 150) {
                    double safeSpeed = Math.sqrt(2 * 0.05 * (minDist - 70));
                    v.setSpeed(Math.min(v.getSpeed(), safeSpeed));
                } 
                else if (minDist <= 70) {
                    v.setSpeed(0);
                }
            }
        }
    }
}