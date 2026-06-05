package model.map;

import java.util.ArrayList;
import java.util.List;

public class CityMap {
    private List<IntersectionNode> nodes = new ArrayList<>();
    private List<RoadEdge> roads = new ArrayList<>();

    public CityMap() { loadMap("Mạng lưới"); }

    public void loadMap(String mapType) {
        nodes.clear();
        roads.clear();

        if      (mapType.equals("Ngã Ba")   || mapType.equals("Ngã 3"))      { buildThreeWay();   }
        else if (mapType.equals("Ngã Tư")   || mapType.equals("Ngã 4"))      { buildFourWay();    }
        else if (mapType.equals("Ngã Năm")  || mapType.equals("Bùng binh")) { buildRoundabout(); }
        else                                                                  { buildDefaultCity();}
        finalizeConnections();
    }

    // ============================================================
    // NGÃ 3
    // ============================================================
    private void buildThreeWay() {
        double cx = 640, cy = 450;
        IntersectionNode center = new IntersectionNode("Ngã 3", cx, cy, IntersectionNode.NodeType.THREE_WAY);
        nodes.add(center);

        IntersectionNode wSpawn = new IntersectionNode("W", cx - 900, cy,       IntersectionNode.NodeType.THREE_WAY, true);
        IntersectionNode eSpawn = new IntersectionNode("E", cx + 900, cy,       IntersectionNode.NodeType.THREE_WAY, true);
        IntersectionNode sSpawn = new IntersectionNode("S", cx,       cy + 900, IntersectionNode.NodeType.THREE_WAY, true);

        roads.add(new RoadEdge(wSpawn, center, RoadEdge.RoadType.AVENUE));
        roads.add(new RoadEdge(eSpawn, center, RoadEdge.RoadType.AVENUE));
        roads.add(new RoadEdge(sSpawn, center, RoadEdge.RoadType.AVENUE));
    }

    // ============================================================
    // NGÃ 4
    // ============================================================
    private void buildFourWay() {
        double cx = 640, cy = 400;
        IntersectionNode center = new IntersectionNode("Ngã 4", cx, cy, IntersectionNode.NodeType.FOUR_WAY);
        nodes.add(center);

        IntersectionNode wSpawn = new IntersectionNode("W", cx - 900, cy,       IntersectionNode.NodeType.THREE_WAY, true);
        IntersectionNode eSpawn = new IntersectionNode("E", cx + 900, cy,       IntersectionNode.NodeType.THREE_WAY, true);
        IntersectionNode nSpawn = new IntersectionNode("N", cx,       cy - 900, IntersectionNode.NodeType.THREE_WAY, true);
        IntersectionNode sSpawn = new IntersectionNode("S", cx,       cy + 900, IntersectionNode.NodeType.THREE_WAY, true);

        roads.add(new RoadEdge(wSpawn, center, RoadEdge.RoadType.AVENUE));
        roads.add(new RoadEdge(eSpawn, center, RoadEdge.RoadType.AVENUE));
        roads.add(new RoadEdge(nSpawn, center, RoadEdge.RoadType.AVENUE));
        roads.add(new RoadEdge(sSpawn, center, RoadEdge.RoadType.AVENUE));
    }

    // ============================================================
    // NGÃ 5 - Bùng binh 5 cánh
    // Spawn nodes đặt đúng theo branchAngles[] = {270, 342, 54, 126, 198}
    // Khoảng cách spawn = 900px (đủ ngoài màn hình)
    // ============================================================
    private void buildRoundabout() {
        double cx = 640, cy = 450;
        IntersectionNode center = new IntersectionNode("Ngã 5", cx, cy, IntersectionNode.NodeType.FIVE_WAY);
        nodes.add(center);

        double dist = 900.0;
        double[] angles = { 270, 342, 54, 126, 198 };
        String[] names  = { "270", "342", "54", "126", "198" };

        for (int i = 0; i < 5; i++) {
            double rad   = Math.toRadians(angles[i]);
            double spawnX = cx + Math.cos(rad) * dist;
            double spawnY = cy + Math.sin(rad) * dist;
            IntersectionNode spawn = new IntersectionNode(
                    names[i], spawnX, spawnY,
                    IntersectionNode.NodeType.THREE_WAY, true);
            roads.add(new RoadEdge(spawn, center, RoadEdge.RoadType.AVENUE));
        }
    }

    // ============================================================
    // MẠNG LƯỚI RỘNG 3×3
    // ============================================================
    private void buildDefaultCity() {
        double cx = 640, cy = 400;
        double sp = 300;

        IntersectionNode nw     = new IntersectionNode("NW", cx-sp, cy-sp, IntersectionNode.NodeType.THREE_WAY);
        IntersectionNode ne     = new IntersectionNode("NE", cx+sp, cy-sp, IntersectionNode.NodeType.THREE_WAY);
        IntersectionNode sw     = new IntersectionNode("SW", cx-sp, cy+sp, IntersectionNode.NodeType.THREE_WAY);
        IntersectionNode se     = new IntersectionNode("SE", cx+sp, cy+sp, IntersectionNode.NodeType.THREE_WAY);
        IntersectionNode nc     = new IntersectionNode("N",  cx,    cy-sp, IntersectionNode.NodeType.FOUR_WAY);
        IntersectionNode wc     = new IntersectionNode("W",  cx-sp, cy,    IntersectionNode.NodeType.FOUR_WAY);
        IntersectionNode ec     = new IntersectionNode("E",  cx+sp, cy,    IntersectionNode.NodeType.FOUR_WAY);
        IntersectionNode sc     = new IntersectionNode("S",  cx,    cy+sp, IntersectionNode.NodeType.FOUR_WAY);
        IntersectionNode center = new IntersectionNode("C",  cx,    cy,    IntersectionNode.NodeType.FIVE_WAY);

        nodes.add(center);
        nodes.add(nw); nodes.add(nc); nodes.add(ne);
        nodes.add(wc); nodes.add(ec);
        nodes.add(sw); nodes.add(sc); nodes.add(se);

        addBidirectional(nw, nc); addBidirectional(nc, ne);
        addBidirectional(wc, center); addBidirectional(center, ec);
        addBidirectional(sw, sc); addBidirectional(sc, se);
        addBidirectional(nw, wc); addBidirectional(wc, sw);
        addBidirectional(nc, center); addBidirectional(center, sc);
        addBidirectional(ne, ec); addBidirectional(ec, se);

        addSpawnRoad(cx-sp, cy-sp-400, nw); addSpawnRoad(cx, cy-sp-400, nc); addSpawnRoad(cx+sp, cy-sp-400, ne);
        addSpawnRoad(cx-sp, cy+sp+400, sw); addSpawnRoad(cx, cy+sp+400, sc); addSpawnRoad(cx+sp, cy+sp+400, se);
        addSpawnRoad(cx-sp-400, cy-sp,  nw); addSpawnRoad(cx-sp-400, cy, wc); addSpawnRoad(cx-sp-400, cy+sp, sw);
        addSpawnRoad(cx+sp+400, cy-sp,  ne); addSpawnRoad(cx+sp+400, cy, ec); addSpawnRoad(cx+sp+400, cy+sp, se);
    }

    // ---- Helpers ----

    private void addBidirectional(IntersectionNode a, IntersectionNode b) {
        roads.add(new RoadEdge(a, b, RoadEdge.RoadType.AVENUE));
        roads.add(new RoadEdge(b, a, RoadEdge.RoadType.AVENUE));
    }

    private void addSpawnRoad(double spawnX, double spawnY, IntersectionNode to) {
        IntersectionNode spawn = new IntersectionNode("SPAWN", spawnX, spawnY,
                IntersectionNode.NodeType.THREE_WAY, true);
        roads.add(new RoadEdge(spawn, to, RoadEdge.RoadType.AVENUE));
    }

    /**
     * Scan roads → set connected directions cho từng node.
     *
     * FIVE_WAY: nhận diện theo 5 góc thực {270, 342, 54, 126, 198} với
     *           tolerance ±20° → gọi setConnectedDirectionsFiveWay().
     * THREE_WAY / FOUR_WAY: nhận diện cardinal 4 hướng → gọi setConnectedDirections().
     */
    private void finalizeConnections() {
        for (IntersectionNode node : nodes) {
            if (node.isSpawnNode()) continue;

            if (node.getType() == IntersectionNode.NodeType.FIVE_WAY) {
                finalizeFiveWay(node);
            } else {
                finalizeCardinal(node);
            }
        }
    }

    /** Nhận diện hướng cardinal cho THREE_WAY / FOUR_WAY */
    private void finalizeCardinal(IntersectionNode node) {
        boolean hasN = false, hasS = false, hasE = false, hasW = false;

        for (RoadEdge road : roads) {
            IntersectionNode nb = neighborOf(road, node);
            if (nb == null) continue;

            double a = angleTo(node, nb);

            if      (a >= 337.5 || a <  22.5) hasE = true;
            else if (a >=  22.5 && a <  67.5) { hasE = true; hasS = true; }
            else if (a >=  67.5 && a < 112.5) hasS = true;
            else if (a >= 112.5 && a < 157.5) { hasS = true; hasW = true; }
            else if (a >= 157.5 && a < 202.5) hasW = true;
            else if (a >= 202.5 && a < 247.5) { hasW = true; hasN = true; }
            else if (a >= 247.5 && a < 292.5) hasN = true;
            else                               { hasN = true; hasE = true; }
        }
        node.setConnectedDirections(hasN, hasS, hasE, hasW);
    }

    /**
     * Nhận diện 5 nhánh góc thực cho FIVE_WAY.
     * branchAngles[] = {270, 342, 54, 126, 198} — tolerance ±20°.
     */
    private void finalizeFiveWay(IntersectionNode node) {
        boolean has270 = false, has342 = false, has54  = false,
                has126 = false, has198 = false;

        double[] targets  = { 270, 342, 54, 126, 198 };
        double   tolerance = 20.0;

        for (RoadEdge road : roads) {
            IntersectionNode nb = neighborOf(road, node);
            if (nb == null) continue;

            double a = angleTo(node, nb);

            for (int i = 0; i < targets.length; i++) {
                if (withinAngle(a, targets[i], tolerance)) {
                    switch (i) {
                        case 0 -> has270 = true;
                        case 1 -> has342 = true;
                        case 2 -> has54  = true;
                        case 3 -> has126 = true;
                        case 4 -> has198 = true;
                    }
                }
            }
        }
        node.setConnectedDirectionsFiveWay(has270, has342, has54, has126, has198);
    }

    // ---- Angle utilities ----

    /** Trả về node còn lại trong road nếu node này là một đầu, ngược lại null */
    private IntersectionNode neighborOf(RoadEdge road, IntersectionNode node) {
        if (road.getStartNode() == node) return road.getEndNode();
        if (road.getEndNode()   == node) return road.getStartNode();
        return null;
    }

    /** Góc từ node → neighbor, [0, 360) */
    private double angleTo(IntersectionNode from, IntersectionNode to) {
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double a  = Math.toDegrees(Math.atan2(dy, dx));
        return a < 0 ? a + 360 : a;
    }

    /**
     * Kiểm tra góc a có nằm trong tolerance của target (xử lý wrap 0/360).
     */
    private boolean withinAngle(double a, double target, double tolerance) {
        double diff = Math.abs(a - target) % 360;
        if (diff > 180) diff = 360 - diff;
        return diff <= tolerance;
    }

    public List<IntersectionNode> getNodes() { return nodes; }
    public List<RoadEdge> getRoads()         { return roads; }
}