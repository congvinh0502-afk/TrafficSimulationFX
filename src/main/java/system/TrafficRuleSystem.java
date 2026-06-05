package system;

import java.util.List;

import model.map.IntersectionNode;
import model.traffic.TrafficLight;
import model.vehicle.Vehicle;

public class TrafficRuleSystem {

    public void applyRules(List<Vehicle> vehicles, List<IntersectionNode> nodes) {
        for (Vehicle v : vehicles) {

            if (!v.getStrategy().obeysTrafficLight()) continue;

            IntersectionNode targetNode = null;
            double minDist = 180;

            // 1. Tìm ngã tư gần nhất trước mặt
            for (IntersectionNode node : nodes) {
                double dx = node.getX() - v.getX();
                double dy = node.getY() - v.getY();

                double rad    = Math.toRadians(v.getAngle());
                double dirX   = Math.cos(rad);
                double dirY   = Math.sin(rad);
                double axial  = dx * dirX + dy * dirY;
                double lateral = Math.abs(dx * (-dirY) + dy * dirX);

                if (axial > 0 && axial < minDist && lateral < 100) {
                    minDist    = axial;
                    targetNode = node;
                }
            }

            if (targetNode == null) continue;

            TrafficLight lightToObey = null;

            if (targetNode.getType() == IntersectionNode.NodeType.FIVE_WAY) {
                // Ngã 5: tìm nhánh có góc gần nhất với hướng xe đang chạy
                // branchAngles[] = {270, 342, 54, 126, 198} — khớp với IntersectionLayout
                double[] branchAngles = { 270, 342, 54, 126, 198 };
                TrafficLight[] branchLights = {
                    targetNode.getLight270(), targetNode.getLight342(),
                    targetNode.getLight54(),  targetNode.getLight126(),
                    targetNode.getLight198()
                };

                double vAngle = v.getAngle() % 360;
                if (vAngle < 0) vAngle += 360;

                double bestDiff = Double.MAX_VALUE;
                for (int i = 0; i < branchAngles.length; i++) {
                    double diff = Math.abs(vAngle - branchAngles[i]) % 360;
                    if (diff > 180) diff = 360 - diff;
                    if (diff < bestDiff) {
                        bestDiff     = diff;
                        lightToObey  = branchLights[i];
                    }
                }

                // Chỉ áp đèn khi xe đi đúng hướng nhánh (tolerance ±30°)
                // Nếu lệch quá → xe đang rẽ trong bùng binh, không bắt dừng
                if (bestDiff > 30) {
                    lightToObey = null;
                }

            } else {
                // THREE_WAY / FOUR_WAY: nhìn đèn theo hướng cardinal
                double angle = v.getAngle() % 360;
                if (angle < 0) angle += 360;

                if      (angle >= 315 || angle <  45)  lightToObey = targetNode.getLightWest();
                else if (angle >=  45 && angle < 135)  lightToObey = targetNode.getLightNorth();
                else if (angle >= 135 && angle < 225)  lightToObey = targetNode.getLightEast();
                else                                   lightToObey = targetNode.getLightSouth();
            }

            if (lightToObey == null) continue;

            // 2. Phanh khi gặp đèn Đỏ hoặc Vàng
            double stopDist = config.Constants.ROAD_WIDTH / 2 + 15 + v.getWidth() / 2;
            if (lightToObey.getPhase() == TrafficLight.Phase.RED
                    || lightToObey.getPhase() == TrafficLight.Phase.YELLOW) {
                if (minDist <= stopDist + 5) {
                    v.setSpeed(0);
                } else if (minDist < 160) {
                    double brakeStrength = Math.sqrt(Math.max(0, 2 * 0.08 * (minDist - stopDist)));
                    v.setSpeed(Math.min(v.getSpeed(), brakeStrength));
                }
            }
        }
    }
}