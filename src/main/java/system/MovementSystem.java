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
            if (v.isBroken() || v.isTurning() || v.hasWaypoints()) continue;

            for (IntersectionNode node : cityMap.getNodes()) {
                if (node.isSpawnNode()) continue;
                double dist = Math.hypot(v.getX() - node.getX(), v.getY() - node.getY());

                // Vòng xuyến (FIVE_WAY) trigger ở khoảng cách 100, ngã thường trigger ở 75
                double threshold = (node.getType() == IntersectionNode.NodeType.FIVE_WAY) ? 100 : 75;

                if (dist < threshold) {
                    startTurning(v, node, cityMap);
                    break;
                }
            }
        }
    }

    private void startTurning(Vehicle v, IntersectionNode node, CityMap cityMap) {

        // ============================================================
        // FIVE_WAY: Sử dụng waypoints ôm vòng xuyến
        // ============================================================
        if (node.getType() == IntersectionNode.NodeType.FIVE_WAY) {
            computeRoundaboutWaypoints(v, node, cityMap);
            return;
        }

        // ============================================================
        // Ngã thường: Bezier
        // ============================================================

        // Bước A: Quét Radar tìm đường
        boolean[] hasRoad = new boolean[4]; // [0:Đông, 1:Nam, 2:Tây, 3:Bắc]
        for (RoadEdge road : cityMap.getRoads()) {
            IntersectionNode neighbor = null;
            if (road.getStartNode() == node) neighbor = road.getEndNode();
            else if (road.getEndNode() == node) neighbor = road.getStartNode();
            if (neighbor == null) continue;
            double dx = neighbor.getX() - node.getX();
            double dy = neighbor.getY() - node.getY();
            if (Math.abs(dx) > Math.abs(dy)) {
                if (dx > 0) hasRoad[0] = true; else hasRoad[2] = true;
            } else {
                if (dy > 0) hasRoad[1] = true; else hasRoad[3] = true;
            }
        }

        // Bước B: Quyết định hướng rẽ hợp lệ
        int currentDir = (int) Math.round(v.getAngle() / 90.0) % 4;
        if (currentDir < 0) currentDir += 4;

        List<TurnType> validTurns = new ArrayList<>();
        if (hasRoad[currentDir])              validTurns.add(TurnType.STRAIGHT);
        if (hasRoad[(currentDir + 1) % 4])    validTurns.add(TurnType.RIGHT);
        if (hasRoad[(currentDir + 3) % 4])    validTurns.add(TurnType.LEFT);
        if (validTurns.isEmpty())             validTurns.add(TurnType.STRAIGHT);

        TurnType turn = validTurns.get(random.nextInt(validTurns.size()));

        // Bước C: Tính Bezier cho ngã thường
        double cx = node.getX(), cy = node.getY();
        double offset = v.getLaneOffset();
        double bound  = 85;
        double p0x = v.getX(), p0y = v.getY();
        double p1x = cx, p1y = cy, p2x = cx, p2y = cy;

        switch (currentDir) {
            case 0: // ĐÔNG
                if (turn == TurnType.RIGHT)      { p1x=cx-offset; p1y=cy+offset; p2x=cx-offset; p2y=cy+bound; }
                else if (turn == TurnType.LEFT)  { p1x=cx+offset; p1y=cy+offset; p2x=cx+offset; p2y=cy-bound; }
                else                             { p1x=cx;        p1y=cy+offset; p2x=cx+bound;  p2y=cy+offset; } break;
            case 1: // NAM
                if (turn == TurnType.RIGHT)      { p1x=cx-offset; p1y=cy-offset; p2x=cx-bound;  p2y=cy-offset; }
                else if (turn == TurnType.LEFT)  { p1x=cx-offset; p1y=cy+offset; p2x=cx+bound;  p2y=cy+offset; }
                else                             { p1x=cx-offset; p1y=cy;        p2x=cx-offset; p2y=cy+bound;  } break;
            case 2: // TÂY
                if (turn == TurnType.RIGHT)      { p1x=cx+offset; p1y=cy-offset; p2x=cx+offset; p2y=cy-bound; }
                else if (turn == TurnType.LEFT)  { p1x=cx-offset; p1y=cy-offset; p2x=cx-offset; p2y=cy+bound; }
                else                             { p1x=cx;        p1y=cy-offset; p2x=cx-bound;  p2y=cy-offset; } break;
            case 3: // BẮC
                if (turn == TurnType.RIGHT)      { p1x=cx+offset; p1y=cy+offset; p2x=cx+bound;  p2y=cy+offset; }
                else if (turn == TurnType.LEFT)  { p1x=cx+offset; p1y=cy-offset; p2x=cx-bound;  p2y=cy-offset; }
                else                             { p1x=cx+offset; p1y=cy;        p2x=cx+offset; p2y=cy-bound;  } break;
        }

        v.setBezierPoints(p0x, p0y, p1x, p1y, p2x, p2y);
        v.setTurning(true);
        v.setBezierT(0);
        v.setTurnType(turn);
    }

    // ============================================================
    // ROUNDABOUT: Tính waypoints ôm theo vòng xuyến theo góc thực
    // ============================================================
    private void computeRoundaboutWaypoints(Vehicle v, IntersectionNode node, CityMap cityMap) {
        double cx = node.getX(), cy = node.getY();
        double R   = 62;  // bán kính làn vòng xuyến
        double off = 15;  // offset làn phải

        // 5 nhánh góc thực, khớp với branchAngles[] trong IntersectionLayout
        double[] branchAngles = { 270, 342, 54, 126, 198 };

        // Tìm nhánh xe đang đi vào (góc gần nhất với hướng xe)
        double vAngle = v.getAngle() % 360;
        if (vAngle < 0) vAngle += 360;

        int entryBranch = 0;
        double bestDiff = Double.MAX_VALUE;
        for (int i = 0; i < branchAngles.length; i++) {
            double diff = Math.abs(vAngle - branchAngles[i]) % 360;
            if (diff > 180) diff = 360 - diff;
            if (diff < bestDiff) { bestDiff = diff; entryBranch = i; }
        }

        // Tìm các nhánh exit hợp lệ (có đường thực sự kết nối)
        // Tính góc từ center đến từng neighbor để khớp với branchAngles[]
        boolean[] hasExit = new boolean[5];
        for (RoadEdge road : cityMap.getRoads()) {
            IntersectionNode nb = null;
            if (road.getStartNode() == node) nb = road.getEndNode();
            else if (road.getEndNode() == node) nb = road.getStartNode();
            if (nb == null) continue;

            double dx = nb.getX() - cx;
            double dy = nb.getY() - cy;
            double a  = Math.toDegrees(Math.atan2(dy, dx)) % 360;
            if (a < 0) a += 360;

            // Gắn neighbor vào nhánh gần nhất (tolerance 30°)
            for (int i = 0; i < branchAngles.length; i++) {
                double diff = Math.abs(a - branchAngles[i]) % 360;
                if (diff > 180) diff = 360 - diff;
                if (diff < 30) { hasExit[i] = true; break; }
            }
        }

        // Chọn exit ngẫu nhiên — không U-turn (nhánh đối diện ~180°)
        List<Integer> exits = new ArrayList<>();
        for (int i = 0; i < branchAngles.length; i++) {
            if (i == entryBranch) continue; // không quay đầu lại

            // Bỏ nhánh đối diện hoàn toàn (diff ~180°)
            double diff = Math.abs(branchAngles[i] - branchAngles[entryBranch]) % 360;
            if (diff > 180) diff = 360 - diff;
            if (diff > 150) continue; // U-turn

            if (hasExit[i]) exits.add(i);
        }

        if (exits.isEmpty()) exits.add(entryBranch); // fallback
        int exitBranch = exits.get(random.nextInt(exits.size()));

        // --- Tính điểm entry/exit trên vành ring ---
        // Xe đi VÀO từ nhánh entryBranch: điểm entry nằm ở phía nhánh đó
        // Offset lệch 90° CW (làn phải theo chiều xe đi)
        double entryAngleRad = Math.toRadians(branchAngles[entryBranch]);
        double entryPerpRad  = entryAngleRad - Math.PI / 2; // CW 90°
        double[] entryPt = {
            cx + Math.cos(entryAngleRad) * R + Math.cos(entryPerpRad) * off,
            cy + Math.sin(entryAngleRad) * R + Math.sin(entryPerpRad) * off
        };

        double exitAngleRad = Math.toRadians(branchAngles[exitBranch]);
        double exitPerpRad  = exitAngleRad + Math.PI / 2; // CCW 90° (ra đúng làn)
        double[] exitPt = {
            cx + Math.cos(exitAngleRad) * R + Math.cos(exitPerpRad) * off,
            cy + Math.sin(exitAngleRad) * R + Math.sin(exitPerpRad) * off
        };

        // --- Điểm ring trung gian: đi CCW từ entry → exit ---
        // Tính góc thực của entryPt và exitPt trên vành ring
        double entryTheta = Math.atan2(entryPt[1] - cy, entryPt[0] - cx);
        double exitTheta  = Math.atan2(exitPt[1]  - cy, exitPt[0]  - cx);

        // Normalize để đi CCW (tăng dần góc)
        if (exitTheta <= entryTheta) exitTheta += 2 * Math.PI;

        List<double[]> waypoints = new ArrayList<>();
        waypoints.add(entryPt);

        // Thêm điểm trung gian mỗi ~90° thay vì 45°
        double theta = entryTheta + Math.PI / 2;
        while (theta < exitTheta - Math.PI / 4) {
            waypoints.add(new double[]{
                cx + R * Math.cos(theta),
                cy + R * Math.sin(theta)
            });
            theta += Math.PI / 2;
        }

        waypoints.add(exitPt);

        // Điểm thoát xa trên đường ra (đủ để xe rời ring)
        double[] roadEnd = {
            cx + Math.cos(exitAngleRad) * (R + 90) + Math.cos(exitPerpRad) * off,
            cy + Math.sin(exitAngleRad) * (R + 90) + Math.sin(exitPerpRad) * off
        };
        waypoints.add(roadEnd);

        v.setWaypoints(waypoints);
    }
}