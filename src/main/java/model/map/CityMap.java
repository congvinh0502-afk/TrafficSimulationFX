package model.map;

import java.util.ArrayList;
import java.util.List;

public class CityMap {
    private List<IntersectionNode> nodes = new ArrayList<>();
    private List<RoadEdge> roads = new ArrayList<>();

    public CityMap() { loadMap("Mạng lưới rộng"); }

    public void loadMap(String mapType) {
        nodes.clear();
        roads.clear();

        // CHỨC NĂNG LỰA CHỌN MAP TỪ GIAO DIỆN
        if (mapType.equals("Ngã 3")) { buildThreeWay(); return; }
        if (mapType.equals("Ngã 4")) { buildFourWay(); return; }
        if (mapType.equals("Bùng binh")) { buildRoundabout(); return; }
        if (mapType.equals("Mạng lưới rộng")) { buildDefaultCity(); return; }

        // ========================================================
        // NẾU CHỌN "Thành Phố Thông Minh" (Hoặc làm map mặc định)
        // ========================================================
        
        // 1. TẠO CÁC NÚT GIAO
        IntersectionNode center = new IntersectionNode("CENTER", 600, 600, IntersectionNode.NodeType.FIVE_WAY);
        IntersectionNode west = new IntersectionNode("W", 100, 600, IntersectionNode.NodeType.FOUR_WAY);
        IntersectionNode east = new IntersectionNode("E", 1100, 600, IntersectionNode.NodeType.FOUR_WAY);
        IntersectionNode north = new IntersectionNode("N", 600, 100, IntersectionNode.NodeType.FOUR_WAY);
        IntersectionNode south = new IntersectionNode("S", 600, 1100, IntersectionNode.NodeType.FOUR_WAY);
        
        IntersectionNode nw = new IntersectionNode("NW", 100, 100, IntersectionNode.NodeType.THREE_WAY);
        IntersectionNode ne = new IntersectionNode("NE", 1100, 100, IntersectionNode.NodeType.THREE_WAY);
        IntersectionNode sw = new IntersectionNode("SW", 100, 1100, IntersectionNode.NodeType.THREE_WAY);
        IntersectionNode se = new IntersectionNode("SE", 1100, 1100, IntersectionNode.NodeType.THREE_WAY);

        // Nút giao cho Ngõ hẻm
        IntersectionNode shortcut1 = new IntersectionNode("SC1", 350, 600, IntersectionNode.NodeType.THREE_WAY);
        IntersectionNode shortcut2 = new IntersectionNode("SC2", 350, 100, IntersectionNode.NodeType.THREE_WAY);

        nodes.add(center); nodes.add(west); nodes.add(east); nodes.add(north); nodes.add(south);
        nodes.add(nw); nodes.add(ne); nodes.add(sw); nodes.add(se);
        nodes.add(shortcut1); nodes.add(shortcut2);

        // 2. TẠO ĐƯỜNG ĐẠI LỘ (AVENUE) 
        // ---> FIX 1: CHIA ĐÔI ĐƯỜNG TỪ WEST ĐẾN CENTER ĐỂ NHÉT SC1 VÀO GIỮA
        roads.add(new RoadEdge(west, shortcut1, RoadEdge.RoadType.AVENUE));
        roads.add(new RoadEdge(shortcut1, center, RoadEdge.RoadType.AVENUE));
        
        roads.add(new RoadEdge(center, east, RoadEdge.RoadType.AVENUE));
        roads.add(new RoadEdge(north, center, RoadEdge.RoadType.AVENUE));
        roads.add(new RoadEdge(center, south, RoadEdge.RoadType.AVENUE));

        // 3. TẠO ĐƯỜNG PHỐ (STREET)
        // ---> FIX 1: CHIA ĐÔI ĐƯỜNG TỪ NW ĐẾN NORTH ĐỂ NHÉT SC2 VÀO GIỮA
        roads.add(new RoadEdge(nw, shortcut2, RoadEdge.RoadType.STREET));
        roads.add(new RoadEdge(shortcut2, north, RoadEdge.RoadType.STREET));
        
        roads.add(new RoadEdge(north, ne, RoadEdge.RoadType.STREET));
        roads.add(new RoadEdge(ne, east, RoadEdge.RoadType.STREET));
        roads.add(new RoadEdge(east, se, RoadEdge.RoadType.STREET));
        roads.add(new RoadEdge(se, south, RoadEdge.RoadType.STREET));
        roads.add(new RoadEdge(south, sw, RoadEdge.RoadType.STREET));
        roads.add(new RoadEdge(sw, west, RoadEdge.RoadType.STREET));
        roads.add(new RoadEdge(west, nw, RoadEdge.RoadType.STREET));

        // 4. TẠO NGÕ HẺM (ALLEY) NỐI 2 NÚT SC LẠI VỚI NHAU
        roads.add(new RoadEdge(shortcut2, shortcut1, RoadEdge.RoadType.ALLEY));
    }

    // NGÃ 3 CHUẨN XÁC, ĐƯỜNG KÉO SIÊU DÀI
    private void buildThreeWay() {
        IntersectionNode center = new IntersectionNode("Ngã 3", 1000, 1000, IntersectionNode.NodeType.THREE_WAY);
        nodes.add(center);
        IntersectionNode nWest = new IntersectionNode("W", -1000, 1000, IntersectionNode.NodeType.THREE_WAY);
        IntersectionNode nEast = new IntersectionNode("E", 3000, 1000, IntersectionNode.NodeType.THREE_WAY);
        IntersectionNode nSouth = new IntersectionNode("S", 1000, 3000, IntersectionNode.NodeType.THREE_WAY);
        roads.add(new RoadEdge(nWest, center)); roads.add(new RoadEdge(center, nEast)); roads.add(new RoadEdge(center, nSouth));
    }

    // NGÃ 4 CHUẨN XÁC
    private void buildFourWay() {
        IntersectionNode center = new IntersectionNode("Ngã 4", 1000, 1000, IntersectionNode.NodeType.FOUR_WAY);
        nodes.add(center);
        IntersectionNode nWest = new IntersectionNode("W", -1000, 1000, IntersectionNode.NodeType.THREE_WAY);
        IntersectionNode nEast = new IntersectionNode("E", 3000, 1000, IntersectionNode.NodeType.THREE_WAY);
        IntersectionNode nNorth = new IntersectionNode("N", 1000, -1000, IntersectionNode.NodeType.THREE_WAY);
        IntersectionNode nSouth = new IntersectionNode("S", 1000, 3000, IntersectionNode.NodeType.THREE_WAY);
        roads.add(new RoadEdge(nWest, center)); roads.add(new RoadEdge(center, nEast));
        roads.add(new RoadEdge(nNorth, center)); roads.add(new RoadEdge(center, nSouth));
    }

    // NGÃ 5 BÙNG BINH
    private void buildRoundabout() {
        IntersectionNode center = new IntersectionNode("Bùng binh", 1000, 1000, IntersectionNode.NodeType.FIVE_WAY);
        nodes.add(center);
        roads.add(new RoadEdge(new IntersectionNode("W", -1000, 1000, IntersectionNode.NodeType.THREE_WAY), center));
        roads.add(new RoadEdge(center, new IntersectionNode("E", 3000, 1000, IntersectionNode.NodeType.THREE_WAY)));
        roads.add(new RoadEdge(new IntersectionNode("N", 1000, -1000, IntersectionNode.NodeType.THREE_WAY), center));
        roads.add(new RoadEdge(center, new IntersectionNode("S", 1000, 3000, IntersectionNode.NodeType.THREE_WAY)));
    }

    // MẠNG LƯỚI KHỔNG LỒ (4000x4000 pixel)
    private void buildDefaultCity() {
        IntersectionNode center = new IntersectionNode("Ngã 5 Trung Tâm", 1500, 1500, IntersectionNode.NodeType.FIVE_WAY);
        IntersectionNode north  = new IntersectionNode("Ngã 4 Bắc", 1500, 500, IntersectionNode.NodeType.FOUR_WAY);
        IntersectionNode south  = new IntersectionNode("Ngã 4 Nam", 1500, 2500, IntersectionNode.NodeType.FOUR_WAY);
        IntersectionNode west   = new IntersectionNode("Ngã 4 Tây", 500, 1500, IntersectionNode.NodeType.FOUR_WAY);
        IntersectionNode east   = new IntersectionNode("Ngã 4 Đông", 2500, 1500, IntersectionNode.NodeType.FOUR_WAY);
        nodes.add(center); nodes.add(north); nodes.add(south); nodes.add(west); nodes.add(east);

        // Nối các ngã tư với nhau
        roads.add(new RoadEdge(north, center)); roads.add(new RoadEdge(center, south));
        roads.add(new RoadEdge(west, center)); roads.add(new RoadEdge(center, east));
        
        // Mở rộng ra các hướng (Tạo cổng sinh xe)
        roads.add(new RoadEdge(new IntersectionNode("W_OUT", -1000, 1500, IntersectionNode.NodeType.THREE_WAY), west));
        roads.add(new RoadEdge(east, new IntersectionNode("E_OUT", 4000, 1500, IntersectionNode.NodeType.THREE_WAY)));
        roads.add(new RoadEdge(new IntersectionNode("N_OUT", 1500, -1000, IntersectionNode.NodeType.THREE_WAY), north));
        roads.add(new RoadEdge(south, new IntersectionNode("S_OUT", 1500, 4000, IntersectionNode.NodeType.THREE_WAY)));
    }

    public List<IntersectionNode> getNodes() { return nodes; }
    public List<RoadEdge> getRoads() { return roads; }
}