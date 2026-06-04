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
                if (dist < 75) {
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
    // ROUNDABOUT: Tính waypoints ôm theo vòng xuyến
    //
    // Xe đi theo luật giao thông VN (đường phải):
    //   vào vòng → rẽ phải dọc theo vành ngoài → thoát ra cánh mong muốn
    //
    // Thứ tự CCW trên màn hình (Y-down): W → S → E → N → W
    //   ring[0]=West, ring[1]=South, ring[2]=East, ring[3]=North
    // ============================================================
    private void computeRoundaboutWaypoints(Vehicle v, IntersectionNode node, CityMap cityMap) {
        double cx = node.getX(), cy = node.getY();
        double R   = 62;   // Bán kính làn vòng xuyến (> đảo cỏ 40px)
        double off = 15;   // Offset làn phải cố định

        // Hướng xe đang đi (0=Đông,1=Nam,2=Tây,3=Bắc)
        int entryDir = (int) Math.round(v.getAngle() / 90.0) % 4;
        if (entryDir < 0) entryDir += 4;

        // Tìm các cánh exit hợp lệ
        boolean[] hasExit = new boolean[4];
        for (RoadEdge road : cityMap.getRoads()) {
            IntersectionNode nb = null;
            if (road.getStartNode() == node) nb = road.getEndNode();
            else if (road.getEndNode() == node) nb = road.getStartNode();
            if (nb == null) continue;
            double dx = nb.getX() - cx, dy = nb.getY() - cy;
            if (Math.abs(dx) > Math.abs(dy)) {
                if (dx > 0) hasExit[0] = true; else hasExit[2] = true;
            } else {
                if (dy > 0) hasExit[1] = true; else hasExit[3] = true;
            }
        }

        // Chọn exit ngẫu nhiên (không U-turn = opposite dir)
        List<Integer> exits = new ArrayList<>();
        for (int d = 0; d < 4; d++) {
            if (d == (entryDir + 2) % 4) continue; // bỏ U-turn
            if (hasExit[d]) exits.add(d);
        }
        if (exits.isEmpty()) {
            // Fallback: tiếp tục thẳng
            exits.add(entryDir);
        }
        int exitDir = exits.get(random.nextInt(exits.size()));

        // --- Điểm entry/exit trên vành vòng xuyến (có offset làn) ---
        // Entry: Xe đến từ phía nào thì điểm entry nằm ở phía đó của ring
        double[] entryPt = ringEntryPoint(cx, cy, R, off, entryDir);
        double[] exitPt  = ringExitPoint(cx, cy, R, off, exitDir);

        // --- Các điểm ring trung gian (CCW: W→S→E→N→W) ---
        // ring[0]=West, ring[1]=South, ring[2]=East, ring[3]=North
        double[][] ringPts = {
            {cx - R, cy},   // 0: West
            {cx, cy + R},   // 1: South
            {cx + R, cy},   // 2: East
            {cx, cy - R},   // 3: North
        };

        // Vị trí của entryDir trên vòng CCW
        int[] entryRingPos = {0, 3, 2, 1}; // dir→ ring position (dir=0/East→pos0/West,...)
        int[] exitRingPos  = {2, 1, 0, 3}; // dir→ ring position (dir=0/East→pos2/East,...)
        int ePos = entryRingPos[entryDir];
        int xPos = exitRingPos[exitDir];

        List<double[]> waypoints = new ArrayList<>();
        waypoints.add(entryPt);

        // Duyệt CCW từ ePos+1 đến xPos
        int pos = (ePos + 1) % 4;
        int maxSteps = 4;
        while (pos != xPos && maxSteps-- > 0) {
            waypoints.add(ringPts[pos]);
            pos = (pos + 1) % 4;
        }

        // Nếu không có điểm trung gian (góc gần), thêm 1 điểm giữa để mượt hơn
        if (waypoints.size() == 1) {
            double midAngle = Math.atan2((entryPt[1] + exitPt[1]) / 2 - cy,
                                         (entryPt[0] + exitPt[0]) / 2 - cx);
            waypoints.add(new double[]{cx + R * Math.cos(midAngle), cy + R * Math.sin(midAngle)});
        }

        waypoints.add(exitPt);

        // Điểm cuối xa trên đường ra (đủ xa để xe thoát khỏi ring)
        double[] roadEnd = roadExitPoint(cx, cy, R + 90, exitDir, off);
        waypoints.add(roadEnd);

        v.setWaypoints(waypoints);
    }

    /** Điểm xe VÀO ring: bên phải (right-hand traffic) của cánh entry */
    private double[] ringEntryPoint(double cx, double cy, double R, double off, int dir) {
        return switch (dir) {
            case 0 -> new double[]{cx - R, cy + off}; // Đến từ W, đi Đông → lane Nam
            case 1 -> new double[]{cx - off, cy - R}; // Đến từ N, đi Nam  → lane Tây
            case 2 -> new double[]{cx + R, cy - off}; // Đến từ E, đi Tây  → lane Bắc
            case 3 -> new double[]{cx + off, cy + R}; // Đến từ S, đi Bắc  → lane Đông
            default -> new double[]{cx, cy};
        };
    }

    /** Điểm xe RA ring: bên phải của cánh exit */
    private double[] ringExitPoint(double cx, double cy, double R, double off, int dir) {
        return switch (dir) {
            case 0 -> new double[]{cx + R, cy + off}; // Ra Đông  → lane Nam
            case 1 -> new double[]{cx - off, cy + R}; // Ra Nam   → lane Tây
            case 2 -> new double[]{cx - R, cy - off}; // Ra Tây   → lane Bắc
            case 3 -> new double[]{cx + off, cy - R}; // Ra Bắc   → lane Đông
            default -> new double[]{cx, cy};
        };
    }

    /** Điểm trên đường ra, đủ xa khỏi ring để xe tiếp tục đi thẳng */
    private double[] roadExitPoint(double cx, double cy, double dist, int dir, double off) {
        return switch (dir) {
            case 0 -> new double[]{cx + dist, cy + off};
            case 1 -> new double[]{cx - off, cy + dist};
            case 2 -> new double[]{cx - dist, cy - off};
            case 3 -> new double[]{cx + off, cy - dist};
            default -> new double[]{cx, cy};
        };
    }
}
