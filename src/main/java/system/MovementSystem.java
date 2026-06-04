package system;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import model.map.CityMap;
import model.map.IntersectionNode;
import model.map.RoadEdge;
import model.vehicle.Vehicle;
import util.TurnType;

public class MovementSystem {
    private Random random = new Random();

    public void updatePositions(List<Vehicle> vehicles, CityMap cityMap) {
        for (Vehicle v : vehicles) {
            // Chỉ kiểm tra rẽ khi xe đang đi thẳng và không bị hỏng
            if (v.isBroken() || v.isTurning()) continue;

            for (IntersectionNode node : cityMap.getNodes()) {
                double dist = Math.hypot(v.getX() - node.getX(), v.getY() - node.getY());
                if (dist < 75) { 
                    startTurning(v, node, cityMap); // Gắn cờ chuẩn bị rẽ
                    break;
                }
            }
        }
    }

    private void startTurning(Vehicle v, IntersectionNode node, CityMap cityMap) {
        // BƯỚC A: Quét Radar tìm đường
        boolean[] hasRoad = new boolean[4]; // [0:Đông, 1:Nam, 2:Tây, 3:Bắc]
        for (RoadEdge road : cityMap.getRoads()) {
            IntersectionNode neighbor = null;
            if (road.getStartNode() == node) neighbor = road.getEndNode();
            else if (road.getEndNode() == node) neighbor = road.getStartNode();
            
            if (neighbor != null) {
                double dx = neighbor.getX() - node.getX();
                double dy = neighbor.getY() - node.getY();
                if (Math.abs(dx) > Math.abs(dy)) {
                    if (dx > 0) hasRoad[0] = true; else hasRoad[2] = true;
                } else {
                    if (dy > 0) hasRoad[1] = true; else hasRoad[3] = true;
                }
            }
        }

        // BƯỚC B & C: Quyết định hướng rẽ hợp lệ
        int currentDir = (int) Math.round(v.getAngle() / 90.0) % 4;
        if (currentDir < 0) currentDir += 4;

        List<TurnType> validTurns = new ArrayList<>();
        if (hasRoad[currentDir]) validTurns.add(TurnType.STRAIGHT);          
        if (hasRoad[(currentDir + 1) % 4]) validTurns.add(TurnType.RIGHT);   
        if (hasRoad[(currentDir + 3) % 4]) validTurns.add(TurnType.LEFT);    
        if (validTurns.isEmpty()) validTurns.add(TurnType.STRAIGHT); 

        TurnType turn = validTurns.get(random.nextInt(validTurns.size()));

        // BƯỚC D: Tính 3 điểm Bezier
        double cx     = node.getX();
        double cy     = node.getY();
        double offset = v.getLaneOffset();
        double bound  = 85;

        // Bán kính vòng xuyến (phải lớn hơn đảo cỏ = 40px)
        double R = 55;

        double p0x = v.getX(), p0y = v.getY();
        double p1x = cx,       p1y = cy;
        double p2x = cx,       p2y = cy;

        boolean isRoundabout = node.getType() == model.map.IntersectionNode.NodeType.FIVE_WAY;

        if (isRoundabout) {
            // Xe PHẢI ôm vòng xuyến: điểm control P1 nằm NGOÀI đảo cỏ (R=55)
            // Quy tắc giao thông VN: xe đi ngược chiều kim đồng hồ quanh đảo
            switch (currentDir) {
                case 0: // Từ Đông sang → đi qua mặt Nam đảo
                    if (turn == TurnType.RIGHT) { p1x = cx + R; p1y = cy + R; p2x = cx + offset; p2y = cy + bound; }
                    else if (turn == TurnType.LEFT) { p1x = cx + R; p1y = cy + R; p2x = cx - offset; p2y = cy - bound; }
                    else { p1x = cx;    p1y = cy + R;      p2x = cx + bound; p2y = cy + offset; } break;
                case 1: // Từ Nam xuống → đi qua mặt Tây đảo
                    if (turn == TurnType.RIGHT) { p1x = cx - R; p1y = cy + R; p2x = cx - bound; p2y = cy + offset; }
                    else if (turn == TurnType.LEFT) { p1x = cx - R; p1y = cy + R; p2x = cx + bound; p2y = cy + offset; }
                    else { p1x = cx - R;   p1y = cy;          p2x = cx - offset; p2y = cy + bound; } break;
                case 2: // Từ Tây sang → đi qua mặt Bắc đảo
                    if (turn == TurnType.RIGHT) { p1x = cx - R; p1y = cy - R; p2x = cx - offset; p2y = cy - bound; }
                    else if (turn == TurnType.LEFT) { p1x = cx - R; p1y = cy - R; p2x = cx + offset; p2y = cy + bound; }
                    else { p1x = cx;    p1y = cy - R;      p2x = cx - bound; p2y = cy - offset; } break;
                case 3: // Từ Bắc xuống → đi qua mặt Đông đảo
                    if (turn == TurnType.RIGHT) { p1x = cx + R; p1y = cy - R; p2x = cx + bound; p2y = cy - offset; }
                    else if (turn == TurnType.LEFT) { p1x = cx + R; p1y = cy - R; p2x = cx - bound; p2y = cy - offset; }
                    else { p1x = cx + R;   p1y = cy;          p2x = cx + offset; p2y = cy - bound; } break;
            }
        } else {
            // Ngã thường: Bezier qua tâm ngã tư
            switch (currentDir) {
                case 0: // ĐÔNG
                    if (turn == TurnType.RIGHT) { p1x = cx - offset; p1y = cy + offset; p2x = cx - offset; p2y = cy + bound; }
                    else if (turn == TurnType.LEFT) { p1x = cx + offset; p1y = cy + offset; p2x = cx + offset; p2y = cy - bound; }
                    else { p1x = cx; p1y = cy + offset; p2x = cx + bound; p2y = cy + offset; } break;
                case 1: // NAM
                    if (turn == TurnType.RIGHT) { p1x = cx - offset; p1y = cy - offset; p2x = cx - bound; p2y = cy - offset; }
                    else if (turn == TurnType.LEFT) { p1x = cx - offset; p1y = cy + offset; p2x = cx + bound; p2y = cy + offset; }
                    else { p1x = cx - offset; p1y = cy; p2x = cx - offset; p2y = cy + bound; } break;
                case 2: // TÂY
                    if (turn == TurnType.RIGHT) { p1x = cx + offset; p1y = cy - offset; p2x = cx + offset; p2y = cy - bound; }
                    else if (turn == TurnType.LEFT) { p1x = cx - offset; p1y = cy - offset; p2x = cx - offset; p2y = cy + bound; }
                    else { p1x = cx; p1y = cy - offset; p2x = cx - bound; p2y = cy - offset; } break;
                case 3: // BẮC
                    if (turn == TurnType.RIGHT) { p1x = cx + offset; p1y = cy + offset; p2x = cx + bound; p2y = cy + offset; }
                    else if (turn == TurnType.LEFT) { p1x = cx + offset; p1y = cy - offset; p2x = cx - bound; p2y = cy - offset; }
                    else { p1x = cx + offset; p1y = cy; p2x = cx + offset; p2y = cy - bound; } break;
            }
        }

        v.setBezierPoints(p0x, p0y, p1x, p1y, p2x, p2y);
        v.setTurning(true);
        v.setBezierT(0);
        v.setTurnType(turn);
    }
}