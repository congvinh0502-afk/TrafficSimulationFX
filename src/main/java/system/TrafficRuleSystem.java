package system;

import java.util.List;
import model.map.IntersectionNode;
import model.traffic.TrafficLight;
import model.vehicle.Vehicle;

public class TrafficRuleSystem {

    public void applyRules(List<Vehicle> vehicles, List<IntersectionNode> nodes) {
        for (Vehicle v : vehicles) {

            // ===== FIX QUAN TRỌNG =====
            // Xe đang bám waypoint = đang LƯU THÔNG TRONG VÒNG XUYẾN.
            // Tuyệt đối không để đèn đỏ khoá tốc độ về 0 giữa vòng
            // (đây chính là nguyên nhân xe "dừng hẳn" ở ngã 5).
            if (v.hasWaypoints()) continue;

            // ĐẶC QUYỀN: Hỏi xem bộ não của tài xế này có tuân thủ đèn giao thông không?
            if (!v.getStrategy().obeysTrafficLight()) {
                continue;
            }

            IntersectionNode targetNode = null;
            double minDist = 280; // phat hien som hon de FIVE_WAY co du khoang dung

            // 1. Tim nga tu gan nhat truoc mat
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

            if (targetNode == null) continue;

            TrafficLight lightToObey = null;

            // 2. Chuyen doi goc sang Huong de nhin dung den
            double angle = v.getAngle() % 360;
            if (angle < 0) angle += 360;

            // ===== FIX: cánh chéo Tây-Bắc của ngã 5 =====
            // Xe từ cánh NW chạy hướng Đông-Nam (~45°) trước đây bị map
            // nhầm vào lightNorth → đứng im suốt pha 4 (pha NW xanh).
            if (targetNode.getType() == IntersectionNode.NodeType.FIVE_WAY
                    && angle >= 25 && angle < 65) {
                lightToObey = targetNode.getLightNW();
            } else if (angle >= 315 || angle < 45) {
                lightToObey = targetNode.getLightWest();
            } else if (angle >= 45 && angle < 135) {
                lightToObey = targetNode.getLightNorth();
            } else if (angle >= 135 && angle < 225) {
                lightToObey = targetNode.getLightEast();
            } else {
                lightToObey = targetNode.getLightSouth();
            }

            if (lightToObey == null) continue;

            // 3. Phanh khi gap den Do hoac Vang
            // FIVE_WAY: vach dung tai ROUNDABOUT_RADIUS + 69 tu tam
            double stopDist = (targetNode.getType() == IntersectionNode.NodeType.FIVE_WAY)
                ? config.Constants.ROUNDABOUT_RADIUS + 69 + v.getWidth() / 2
                : config.Constants.ROAD_WIDTH / 2 + 15 + v.getWidth() / 2;

            if (lightToObey.getPhase() == TrafficLight.Phase.RED
                    || lightToObey.getPhase() == TrafficLight.Phase.YELLOW) {
                if (minDist <= stopDist + 5) {
                    v.setSpeed(0);
                } else if (minDist < stopDist + 65) {
                    double brakeStrength = Math.sqrt(Math.max(0, 2 * 0.08 * (minDist - stopDist)));
                    v.setSpeed(Math.min(v.getSpeed(), brakeStrength));
                }
            }
        }
    }
}