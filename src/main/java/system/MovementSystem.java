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

    // ===== Tham số vòng xuyến =====
    private static final double RING_RADIUS  = 62;   // bán kính làn chạy (> đảo cỏ 40, < nền nhựa 100)
    private static final double LANE_OFF     = 15;   // độ lệch làn phải
    private static final double WP_STEP_RAD  = Math.toRadians(10); // mật độ waypoint: 10°/điểm → cong mượt
    private static final double TRIGGER_5WAY = 150;  // khoảng cách bắt đầu bám vòng xuyến (< vạch dừng ~190)
    private static final double TRIGGER_OTHER = 75;
    private static final double EXIT_DIST    = RING_RADIUS + 120; // điểm cuối PHẢI > TRIGGER_5WAY
                                                                  // để xe vừa thoát không bị kích hoạt lại

    public void updatePositions(List<Vehicle> vehicles, CityMap cityMap) {
        for (Vehicle v : vehicles) {
            if (v.isBroken() || v.isTurning() || v.hasWaypoints()) continue;

            for (IntersectionNode node : cityMap.getNodes()) {
                if (node.isSpawnNode()) continue;
                double dist = Math.hypot(v.getX() - node.getX(), v.getY() - node.getY());

                boolean isFiveWay = node.getType() == IntersectionNode.NodeType.FIVE_WAY;
                double trigger = isFiveWay ? TRIGGER_5WAY : TRIGGER_OTHER;

                if (dist < trigger) {
                    // NHƯỜNG ĐƯỜNG: nếu ngay cửa vào đang có xe chạy trong vòng xuyến → chờ
                    if (isFiveWay && ringBusyNear(v, vehicles)) {
                        v.setSpeed(Math.max(0, v.getSpeed() - 0.25));
                        break; // chưa cấp waypoint, frame sau kiểm tra lại
                    }
                    startTurning(v, node, cityMap);
                    break;
                }
            }
        }
    }

    /** Có xe nào đang lưu thông trong vòng xuyến ở gần cửa vào của v không? */
    private boolean ringBusyNear(Vehicle v, List<Vehicle> vehicles) {
        for (Vehicle other : vehicles) {
            if (other == v || !other.hasWaypoints()) continue;
            double d = Math.hypot(other.getX() - v.getX(), other.getY() - v.getY());
            if (d < 80) return true;
        }
        return false;
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
        // Ngã thường: Bezier (GIỮ NGUYÊN)
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
    // ROUNDABOUT (PHIÊN BẢN MỚI): Sinh waypoint DÀY theo CUNG TRÒN
    //
    // Khác bản cũ:
    //  - Không dùng 4 hướng cứng (E/S/W/N) nữa → cánh chéo Tây-Bắc
    //    được xử lý tự nhiên như mọi cánh khác (theo vector).
    //  - Thay vì 4 điểm cách nhau 90° (xe đi gãy khúc), sinh điểm
    //    mỗi 10° dọc cung tròn → xe ôm cua tròn thật sự.
    //
    // Chiều lưu thông (luật VN, màn hình Y-down): GÓC GIẢM DẦN
    //   W(180°) → S(90°) → E(0°) → N(-90°) → NW(-135°) → W ...
    // ============================================================
    private void computeRoundaboutWaypoints(Vehicle v, IntersectionNode node, CityMap cityMap) {
        double cx = node.getX(), cy = node.getY();
        double R   = RING_RADIUS;
        double off = LANE_OFF;

        // --- 1. Gom các cánh nối vào node dưới dạng VECTOR đơn vị (tâm → ngoài) ---
        List<double[]> arms = new ArrayList<>();
        for (RoadEdge road : cityMap.getRoads()) {
            IntersectionNode nb = null;
            if (road.getStartNode() == node) nb = road.getEndNode();
            else if (road.getEndNode() == node) nb = road.getStartNode();
            if (nb == null) continue;
            double dx = nb.getX() - cx, dy = nb.getY() - cy;
            double len = Math.hypot(dx, dy);
            if (len < 1) continue;
            double ux = dx / len, uy = dy / len;
            // bỏ trùng lặp (đường 2 chiều của cùng 1 cánh)
            boolean dup = false;
            for (double[] a : arms) {
                if (a[0] * ux + a[1] * uy > 0.95) { dup = true; break; }
            }
            if (!dup) arms.add(new double[]{ux, uy});
        }
        if (arms.isEmpty()) return;

        // --- 2. Cánh ENTRY = cánh ngược với hướng xe đang chạy ---
        double hRad = Math.toRadians(v.getAngle());
        double hx = Math.cos(hRad), hy = Math.sin(hRad);
        double[] entryArm = arms.get(0);
        double best = -2;
        for (double[] a : arms) {
            double dot = -(a[0] * hx + a[1] * hy); // càng ngược hướng chạy càng lớn
            if (dot > best) { best = dot; entryArm = a; }
        }

        // --- 3. Chọn cánh EXIT ngẫu nhiên (loại cánh entry = cấm quay đầu) ---
        List<double[]> exitCandidates = new ArrayList<>();
        for (double[] a : arms) if (a != entryArm) exitCandidates.add(a);
        double[] exitArm = exitCandidates.isEmpty()
                ? entryArm
                : exitCandidates.get(random.nextInt(exitCandidates.size()));

        // --- 4. Góc entry/exit trên vành (lệch về làn phải) ---
        double laneShift = Math.atan2(off, R); // ~13.6°
        double aEntry = Math.atan2(entryArm[1], entryArm[0]) - laneShift;
        double aExit  = Math.atan2(exitArm[1],  exitArm[0])  + laneShift;

        // chuẩn hoá: lưu thông theo chiều GIẢM góc → cần aExit < aEntry
        while (aExit >= aEntry) aExit -= 2 * Math.PI;

        // --- 5. Sinh waypoint dày dọc cung tròn ---
        List<double[]> wps = new ArrayList<>();
        for (double a = aEntry; a > aExit; a -= WP_STEP_RAD) {
            wps.add(new double[]{cx + R * Math.cos(a), cy + R * Math.sin(a)});
        }
        wps.add(new double[]{cx + R * Math.cos(aExit), cy + R * Math.sin(aExit)});

        // --- 6. Điểm thoát trên đường ra: dọc cánh exit + lệch làn phải ---
        // pháp tuyến bên phải hướng ra (Y-down): (ux,uy) → (-uy, ux)
        double px = -exitArm[1], py = exitArm[0];
        wps.add(new double[]{
            cx + exitArm[0] * EXIT_DIST + px * off,
            cy + exitArm[1] * EXIT_DIST + py * off
        });

        v.setWaypoints(wps);
    }
}