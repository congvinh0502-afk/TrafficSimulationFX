package engine;

import java.util.ArrayList;
import java.util.List;

import config.Constants;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import model.map.CityMap;
import model.map.IntersectionNode;
import model.map.RoadEdge;
import model.traffic.TrafficLight;
import model.vehicle.Vehicle;
import system.CollisionSystem;
import system.MovementSystem; // <--- THÊM DÒNG NÀY VÀO
import system.SpawnSystem;
import system.TrafficRuleSystem;
import view.VehicleRenderer;

public class SimulationEngine extends AnimationTimer {
    
    private Canvas canvas;
    private GraphicsContext gc;
    private CityMap cityMap;
    
    private double cameraX = 0;
    private double cameraY = 0;
    
    // --- CÁC THÀNH PHẦN MỚI ---
    private List<Vehicle> vehicles = new ArrayList<>();
    private SpawnSystem spawnSystem = new SpawnSystem();
    private MovementSystem movementSystem = new MovementSystem();
    // Thêm hệ thống này vào cùng chỗ với SpawnSystem và MovementSystem
    private CollisionSystem collisionSystem = new CollisionSystem();
    private TrafficLight vLight = new TrafficLight(300); // Đèn trục dọc
    private TrafficLight hLight = new TrafficLight(300); // Đèn trục ngang
    private TrafficRuleSystem ruleSystem = new TrafficRuleSystem();

    public SimulationEngine(Canvas canvas) {
        this.canvas = canvas;
        this.gc = canvas.getGraphicsContext2D();
        this.cityMap = new CityMap(); // Tải bản đồ thành phố lên
    }
    

    @Override
    public void handle(long now) {
        update();
        render();
    }

    // --- CẬP NHẬT LOGIC ---
    private void update() {
        spawnSystem.spawnRandom(vehicles, cityMap);
        // 1. Check va chạm để xem xét đạp ga hay đạp phanh
        collisionSystem.update(vehicles);
        // 2. Sau khi có tốc độ chuẩn rồi mới di chuyển xe
        movementSystem.updatePositions(vehicles);
        
        // Tạm thời xóa các xe đã chạy vượt quá giới hạn bản đồ (Tránh tràn bộ nhớ)
        vehicles.removeIf(v -> v.getX() < -1000 || v.getX() > 3000 || v.getY() < -1000 || v.getY() > 3000);
        vLight.update();
        hLight.update();
    }

    private void render() {
        // 1. Phủ màu nền cỏ toàn thành phố
        gc.setFill(Color.web("#27ae60")); 
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        
        gc.save();
        gc.translate(-cameraX, -cameraY);

        // 2. VẼ ĐƯỜNG NỐI (Road Edges)
        for (RoadEdge road : cityMap.getRoads()) {
            double sx = road.getStartNode().getX();
            double sy = road.getStartNode().getY();
            double ex = road.getEndNode().getX();
            double ey = road.getEndNode().getY();

            // Vẽ lớp đường nhựa xám
            gc.setStroke(Color.web("#34495e")); 
            gc.setLineWidth(Constants.ROAD_WIDTH);
            gc.setLineCap(StrokeLineCap.BUTT);
            gc.strokeLine(sx, sy, ex, ey);

            // Vẽ vạch đôi màu vàng ở tim đường
            gc.setStroke(Color.web("#f1c40f"));
            gc.setLineWidth(2);
            gc.strokeLine(sx, sy, ex, ey);

            // Vẽ vạch đứt nét màu trắng chia làn
            drawDashedLine(sx, sy, ex, ey, Constants.ROAD_WIDTH / 4);  
            drawDashedLine(sx, sy, ex, ey, -Constants.ROAD_WIDTH / 4); 
        }

        // 3. VẼ GIAO LỘ (Intersection Nodes)
        for (IntersectionNode node : cityMap.getNodes()) {
            // Đổ thêm một khối nhựa đường vuông ở giữa để che lấp các vạch kẻ đường bị chéo nhau
            gc.setFill(Color.web("#34495e"));
            gc.fillRect(node.getX() - Constants.ROAD_WIDTH/2, node.getY() - Constants.ROAD_WIDTH/2, 
                        Constants.ROAD_WIDTH, Constants.ROAD_WIDTH);

            // ĐẶC BIỆT: Nếu là ngã 5 thì vẽ thêm Bùng binh (Vòng xuyến)
            if (node.getType() == IntersectionNode.NodeType.FIVE_WAY) {
                double radius = 40; // Bán kính đảo giao thông
                gc.setFill(Color.web("#2ecc71")); // Cỏ giữa bùng binh
                gc.fillOval(node.getX() - radius, node.getY() - radius, radius * 2, radius * 2);
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(2);
                gc.strokeOval(node.getX() - radius, node.getY() - radius, radius * 2, radius * 2);
            }
        }
        // --- GỌI RENDERER ĐỂ VẼ XE VÀO BẢN ĐỒ ---
        for (Vehicle v : vehicles) {
            VehicleRenderer.draw(gc, v);
        }
        
        gc.restore();
    }

    // Hàm tiện ích: Tự động vẽ vạch kẻ đứt nét tịnh tiến song song với tim đường
    private void drawDashedLine(double sx, double sy, double ex, double ey, double offset) {
        double angle = Math.atan2(ey - sy, ex - sx);
        double p1x = sx + Math.cos(angle + Math.PI / 2) * offset;
        double p1y = sy + Math.sin(angle + Math.PI / 2) * offset;
        double p2x = ex + Math.cos(angle + Math.PI / 2) * offset;
        double p2y = ey + Math.sin(angle + Math.PI / 2) * offset;

        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.setLineDashes(15, 15); // Chiều dài mỗi vạch đứt
        gc.strokeLine(p1x, p1y, p2x, p2y);
        gc.setLineDashes(null);   // Xóa hiệu ứng đứt nét cho các lần vẽ sau
    }
    // Thêm hàm này vào class SimulationEngine để thay đổi tọa độ Camera
    public void moveCamera(double dx, double dy) {
        this.cameraX -= dx;
        this.cameraY -= dy;
    }
}