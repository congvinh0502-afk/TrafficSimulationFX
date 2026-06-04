package engine;

import config.Constants;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import model.map.CityMap;
import model.map.IntersectionNode;
import model.map.RoadEdge;
import java.util.ArrayList;
import java.util.List;
import model.traffic.TrafficLight;
import model.vehicle.Vehicle;
import system.MovementSystem;
import system.SpawnSystem;
import system.CollisionSystem;
import system.TrafficRuleSystem;
import view.VehicleRenderer;

public class SimulationEngine extends AnimationTimer {
    
    private Canvas canvas;
    private GraphicsContext gc;
    private CityMap cityMap;
    
    // --- CAMERA & VIEW ---
    private double cameraX = 0;
    private double cameraY = 0;
    private double zoomScale = 1.0;
    private Vehicle lockedVehicle = null; 

    // --- SYSTEMS ---
    private List<Vehicle> vehicles = new ArrayList<>();
    private SpawnSystem spawnSystem = new SpawnSystem();
    private MovementSystem movementSystem = new MovementSystem();
    private CollisionSystem collisionSystem = new CollisionSystem();
    private TrafficRuleSystem trafficRuleSystem = new TrafficRuleSystem(); 

    // --- ENVIRONMENT ---
    private boolean isPaused = false;
    private boolean isDebugMode = false;
    private double timeOfDay = 12.0; 
    private double[] rainX = new double[300];
    private double[] rainY = new double[300];
   

    public SimulationEngine(Canvas canvas) {
        this.canvas = canvas;
        this.gc = canvas.getGraphicsContext2D();
        this.cityMap = new CityMap(); 
        
        // Khởi tạo hạt mưa
        for (int i = 0; i < 300; i++) {
            rainX[i] = Math.random() * Constants.WINDOW_WIDTH;
            rainY[i] = Math.random() * Constants.WINDOW_HEIGHT;
        }

        // ---> QUAN TRỌNG: Gọi hàm đổi Map ngay lúc bật app để Camera nhảy đúng vào giữa đường!
        changeMap("Mạng lưới");
    }

    // --- CÁC HÀM GETTER/SETTER CHO GIAO DIỆN ---
    public void togglePause() { this.isPaused = !this.isPaused; }
    public void setDebugMode(boolean debug) { this.isDebugMode = debug; }
    public SpawnSystem getSpawnSystem() { return spawnSystem; }
    public double getZoomScale() { return zoomScale; }

    /** Chế độ cảnh sát: đèn tất cả đỏ, sau đó cho từng hướng xanh lần lượt */
    public void togglePoliceMode() {
        config.Constants.AUTO_LIGHTS = false;
        for (model.map.IntersectionNode node : cityMap.getNodes()) {
            node.manualToggle();
        }
    }
    
    public void zoomCamera(double factor) {
        this.zoomScale *= factor;
        if(this.zoomScale < 0.3) this.zoomScale = 0.3; 
        if(this.zoomScale > 3.0) this.zoomScale = 3.0; 
    }
    
    public void moveCamera(double dx, double dy) {
        this.cameraX -= dx;
        this.cameraY -= dy;
    }
    public void unlockCamera() { this.lockedVehicle = null; }

    // --- HÀM ĐỔI BẢN ĐỒ VÀ CĂN GIỮA CAMERA ---
    public void changeMap(String mapType) {
        vehicles.clear();         
        cityMap.loadMap(mapType); 
        zoomScale = 1.0;          
        unlockCamera();           
        
        // Thuật toán: Tìm Ngã tư đầu tiên (Ngã tư trung tâm) để chĩa Camera vào
        if (!cityMap.getNodes().isEmpty()) {
            IntersectionNode centerNode = cityMap.getNodes().get(0);
            cameraX = centerNode.getX() - (canvas.getWidth() / 2);
            cameraY = centerNode.getY() - (canvas.getHeight() / 2);
        }
    }

    // --- XỬ LÝ CLICK CHUỘT TƯƠNG TÁC ---
    public void handleMouseClick(double mouseX, double mouseY, boolean isRightClick) {
        double worldX = (mouseX - canvas.getWidth()/2) / zoomScale + canvas.getWidth()/2 + cameraX;
        double worldY = (mouseY - canvas.getHeight()/2) / zoomScale + canvas.getHeight()/2 + cameraY;
        
        // 1. Check click vào Đèn Giao Thông (Đổi màu thủ công)
        if (!config.Constants.AUTO_LIGHTS) {
            for (IntersectionNode node : cityMap.getNodes()) {
                if (node.getType() != IntersectionNode.NodeType.FIVE_WAY) {
                    if (Math.abs(node.getX() - worldX) < 50 && Math.abs(node.getY() - worldY) < 50) {
                        node.manualToggle(); 
                        return; 
                    }
                }
            }
        }

        // 2. Check click vào Xe (Phá lốp / Flycam bám đuổi)
        boolean clickedOnCar = false;
        for (Vehicle v : vehicles) {
            if (Math.abs(v.getX() - worldX) < 25 && Math.abs(v.getY() - worldY) < 25) {
                clickedOnCar = true;
                if (isRightClick) lockedVehicle = v; 
                else {
                    v.setBroken(!v.isBroken()); 
                    lockedVehicle = null; 
                }
                break;
            }
        }
        if (isRightClick && !clickedOnCar) lockedVehicle = null;
    }

    @Override
    public void handle(long now) {
        update();
        render();
    }

    private void update() {
        if (isPaused) return; 

        // 1. Logic thời gian 
        if (config.Constants.TIME_MODE == 0) {
            timeOfDay += 0.005; 
            if (timeOfDay >= 24) timeOfDay = 0;
        } else if (config.Constants.TIME_MODE == 1) {
            timeOfDay = 12.0; 
        } else {
            timeOfDay = 0.0;  
        }

        // 2. Spawn xe và kiểm tra còi hú
        spawnSystem.spawnRandom(vehicles, cityMap);
        
        boolean hasAmbulance = false;
        boolean hasFiretruck = false;
        for (Vehicle v : vehicles) {
            if (v instanceof model.vehicle.FireTruck) hasFiretruck = true;
            else if (v instanceof model.vehicle.EmergencyVehicle) hasAmbulance = true;
        }
        if (hasFiretruck) util.SoundManager.playFiretruck();
        if (hasAmbulance) util.SoundManager.playAmbulance();
        
        // 3. Cập nhật đèn đỏ
        for (IntersectionNode node : cityMap.getNodes()) {
            node.updateLights();
        }
        
        // 4. Các System xử lý tính toán CẤP DỮ LIỆU
        collisionSystem.update(vehicles);
        trafficRuleSystem.applyRules(vehicles, cityMap.getNodes());
        movementSystem.updatePositions(vehicles, cityMap); // Gắn cờ bẻ lái Bezier
        
        // 5. CHO TỪNG CHIẾC XE TỰ CHẠY BẰNG BỘ NÃO MỚI CỦA CHÚNG NÓ
        for (Vehicle v : vehicles) {
            v.update(vehicles); 
        }

        // 6. Dọn dẹp xe đi quá xa
        vehicles.removeIf(v -> v.getX() < -2000 || v.getX() > 6000 || v.getY() < -2000 || v.getY() > 6000);

        // 7. Cập nhật Flycam (Bám theo xe)
        if (lockedVehicle != null) {
            if (!vehicles.contains(lockedVehicle)) {
                lockedVehicle = null; 
            } else {
                cameraX = lockedVehicle.getX() - canvas.getWidth() / 2;
                cameraY = lockedVehicle.getY() - canvas.getHeight() / 2;
            }
        }
    }

    private Color getColorForPhase(TrafficLight.Phase phase) {
        if (phase == TrafficLight.Phase.RED)    return Color.RED;
        if (phase == TrafficLight.Phase.YELLOW) return Color.YELLOW;
        return Color.LIMEGREEN;
    }

    /** Vẽ trụ đèn giao thông tại (x, y) gồm hộp đen + 1 đèn màu */
    private void drawTrafficLight(GraphicsContext gc, TrafficLight light, double x, double y) {
        // Hộp đèn màu đen
        gc.setFill(Color.web("#2c3e50"));
        gc.fillRoundRect(x, y, 16, 22, 4, 4);
        // Đèn màu bên trong
        gc.setFill(getColorForPhase(light.getPhase()));
        gc.fillOval(x + 3, y + 3, 10, 10);
        // Halo nhỏ khi xanh
        if (light.getPhase() == TrafficLight.Phase.GREEN) {
            gc.setFill(Color.LIMEGREEN.deriveColor(0, 1, 1, 0.25));
            gc.fillOval(x - 2, y - 2, 20, 20);
        }
    }

    private void render() {
        // TẦNG 1: VẼ ĐỊA HÌNH
        gc.setFill(Color.web("#27ae60")); 
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        
        gc.save();
        // Phóng to / Thu nhỏ xoay quanh tâm màn hình
        gc.translate(canvas.getWidth()/2, canvas.getHeight()/2);
        gc.scale(zoomScale, zoomScale);
        gc.translate(-canvas.getWidth()/2, -canvas.getHeight()/2);
        gc.translate(-cameraX, -cameraY);

        // 1.1 VẼ ĐƯỜNG NHỰA VÀ VẠCH KẺ LÀN (Đã hỗ trợ phân cấp Đại lộ, Phố, Ngõ)
        for (RoadEdge road : cityMap.getRoads()) {
            double sx = road.getStartNode().getX(); double sy = road.getStartNode().getY();
            double ex = road.getEndNode().getX(); double ey = road.getEndNode().getY();
            
            double roadW = road.getWidth(); // Lấy độ rộng linh hoạt từ class RoadEdge
            
            // Vẽ nền đường nhựa màu xám
            gc.setStroke(Color.web("#34495e")); 
            gc.setLineWidth(roadW); 
            gc.setLineCap(StrokeLineCap.BUTT); 
            gc.strokeLine(sx, sy, ex, ey);
            
            // Vẽ vạch kẻ đường (Sẽ không vẽ vạch nếu đó là ALLEY - Ngõ hẻm)
            if (road.getType() != RoadEdge.RoadType.ALLEY) {
                gc.setStroke(Color.web("#f1c40f")); // Màu vàng
                gc.setLineWidth(2);
                
                if (road.getType() == RoadEdge.RoadType.AVENUE) {
                    // Cấp Đại lộ (AVENUE): Vẽ vạch đôi liền màu vàng (Tim đường)
                    gc.setLineDashes(null);
                    gc.strokeLine(sx + 2, sy + 2, ex + 2, ey + 2);
                    gc.strokeLine(sx - 2, sy - 2, ex - 2, ey - 2);
                    
                    // Vẽ vạch đứt phân làn 4 xe
                    drawDashedLine(sx, sy, ex, ey, roadW / 4);  
                    drawDashedLine(sx, sy, ex, ey, -roadW / 4); 
                } else {
                    // Cấp Đường phố (STREET): Vẽ 1 vạch đứt màu vàng ở giữa
                    gc.setLineDashes(15, 15);
                    gc.strokeLine(sx, sy, ex, ey);
                    gc.setLineDashes(null);
                }
            }
        
        }

        // 1.2 VẼ CHI TIẾT NGÃ TƯ (Vạch đi bộ, Vạch dừng, Vạch dẫn hướng)
        for (IntersectionNode node : cityMap.getNodes()) {
            double nX = node.getX(); double nY = node.getY();
            double halfW = Constants.ROAD_WIDTH / 2;

            // Xóa các vạch kẻ đường thô bạo đâm xuyên vào giữa ngã tư
            gc.setFill(Color.web("#34495e"));
            gc.fillRect(nX - halfW, nY - halfW, Constants.ROAD_WIDTH, Constants.ROAD_WIDTH);
            
            if (node.getType() == IntersectionNode.NodeType.FIVE_WAY) {
                // --- ĐỒ HỌA BÙNG BINH (NGÃ 5) ---
                double radius = 40; 
                // Vạch đi bộ quanh bùng binh (Nét đứt vòng tròn)
                gc.setStroke(Color.WHITE); gc.setLineWidth(5); gc.setLineDashes(4, 8);
                gc.strokeOval(nX - radius - 15, nY - radius - 15, (radius + 15) * 2, (radius + 15) * 2);
                gc.setLineDashes(null);
                
                // Đảo cỏ xanh ở giữa
                gc.setFill(Color.web("#2ecc71")); gc.fillOval(nX - radius, nY - radius, radius * 2, radius * 2);
                gc.setStroke(Color.WHITE); gc.setLineWidth(2); gc.strokeOval(nX - radius, nY - radius, radius * 2, radius * 2);
            } else {
                // --- ĐỒ HỌA NGÃ 3, NGÃ 4 (THÔNG MINH theo hướng đường) ---
                boolean hasNorth = false, hasSouth = false, hasWest = false, hasEast = false;
                for (RoadEdge r : cityMap.getRoads()) {
                    if (r.getStartNode() == node || r.getEndNode() == node) {
                        IntersectionNode neighbor = (r.getStartNode() == node) ? r.getEndNode() : r.getStartNode();
                        if (neighbor.getY() < node.getY() - 10) hasNorth = true;
                        if (neighbor.getY() > node.getY() + 10) hasSouth = true;
                        if (neighbor.getX() < node.getX() - 10) hasWest = true;
                        if (neighbor.getX() > node.getX() + 10) hasEast = true;
                    }
                }

                // a. Vạch kẻ đường đi bộ (Zebra Crossing) - chỉ vẽ ở nơi có đường
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(6);
                gc.setLineDashes(4, 6);
                if (hasNorth || hasSouth) {
                    gc.strokeLine(nX - halfW + 5, nY - halfW - 15, nX + halfW - 5, nY - halfW - 15);
                    gc.strokeLine(nX - halfW + 5, nY + halfW + 15, nX + halfW - 5, nY + halfW + 15);
                }
                if (hasWest || hasEast) {
                    gc.strokeLine(nX - halfW - 15, nY - halfW + 5, nX - halfW - 15, nY + halfW - 5);
                    gc.strokeLine(nX + halfW + 15, nY - halfW + 5, nX + halfW + 15, nY + halfW - 5);
                }
                gc.setLineDashes(null);

                // b. Vạch dừng chờ đèn đỏ (Stop line) - chỉ vẽ ở nơi có đường
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(3);
                if (hasNorth) gc.strokeLine(nX, nY - halfW - 5, nX + halfW, nY - halfW - 5);
                if (hasSouth) gc.strokeLine(nX - halfW, nY + halfW + 5, nX, nY + halfW + 5);
                if (hasWest)  gc.strokeLine(nX - halfW - 5, nY - halfW, nX - halfW - 5, nY);
                if (hasEast)  gc.strokeLine(nX + halfW + 5, nY, nX + halfW + 5, nY + halfW);

                // c. Vạch dẫn hướng ôm cua (Guide Arcs) - vàng mờ
                gc.setStroke(Color.rgb(241, 196, 15, 0.4));
                gc.setLineWidth(1.5);
                gc.setLineDashes(5, 10);
                gc.strokeArc(nX - halfW, nY - halfW, halfW, halfW, 270, 90, javafx.scene.shape.ArcType.OPEN);
                gc.strokeArc(nX, nY - halfW, halfW, halfW, 180, 90, javafx.scene.shape.ArcType.OPEN);
                gc.strokeArc(nX - halfW, nY, halfW, halfW, 0, 90, javafx.scene.shape.ArcType.OPEN);
                gc.strokeArc(nX, nY, halfW, halfW, 90, 90, javafx.scene.shape.ArcType.OPEN);
                gc.setLineDashes(null);
            }
        }

        // TẦNG 2: VẼ THÂN XE 
        for (Vehicle v : vehicles) {
            VehicleRenderer.drawCarBody(gc, v);
        }

        // TẦNG 3: TÍNH TOÁN VÀ PHỦ MÀN ĐÊM (Shading Pass)
        double darkness = 0;
        if (timeOfDay >= 18 || timeOfDay <= 6) darkness = 0.75; 
        else if (timeOfDay > 16 && timeOfDay < 18) darkness = 0.75 * ((timeOfDay - 16) / 2.0); 
        else if (timeOfDay > 6 && timeOfDay < 8) darkness = 0.75 * (1 - ((timeOfDay - 6) / 2.0)); 

        if (darkness > 0) {
            gc.setFill(Color.rgb(10, 15, 30, darkness)); 
            // Tính toán vùng che phủ rộng hơn để không bị lỗi màn đêm khi zoom out
            gc.fillRect(cameraX - 5000, cameraY - 5000, 15000, 15000);
        }

        // TẦNG 4: VẼ ĐÈN PHA & ĐÈN HẬU 
        for (Vehicle v : vehicles) {
            VehicleRenderer.drawLights(gc, v, darkness);
        }

        // TẦNG 5: VẼ ĐÈN GIAO THÔNG (chỉ vẽ ở hướng có đường)
        for (IntersectionNode node : cityMap.getNodes()) {
            if (node.isSpawnNode()) continue;
            double nX = node.getX(), nY = node.getY();
            double off = Constants.ROAD_WIDTH / 2 + 8; // khoảng cách từ tâm đến trụ đèn

            if (node.getType() == IntersectionNode.NodeType.FIVE_WAY) {
                // 5 đèn: N, S, E, W + NW diagonal
                drawTrafficLight(gc, node.getLightNorth(), nX,        nY - off - 10);
                drawTrafficLight(gc, node.getLightSouth(), nX,        nY + off);
                drawTrafficLight(gc, node.getLightEast(),  nX + off,  nY);
                drawTrafficLight(gc, node.getLightWest(),  nX - off - 20, nY);
                drawTrafficLight(gc, node.getLightNW(),    nX - off,  nY - off);
            } else {
                // Chỉ vẽ đèn ở hướng có đường thực sự
                if (node.isHasNorth()) drawTrafficLight(gc, node.getLightNorth(), nX - 12, nY - off - 10);
                if (node.isHasSouth()) drawTrafficLight(gc, node.getLightSouth(), nX + 12, nY + off);
                if (node.isHasEast())  drawTrafficLight(gc, node.getLightEast(),  nX + off, nY - 12);
                if (node.isHasWest())  drawTrafficLight(gc, node.getLightWest(),  nX - off - 20, nY + 12);
            }

            // Đồng hồ đếm ngược (hiển thị ở tâm ngã tư)
            int remain = (int) Math.ceil(node.getRemainingTime());
            boolean showText = node.getLightMode() == IntersectionNode.LightMode.COUNTDOWN
                    || (node.getLightMode() == IntersectionNode.LightMode.SMART_COUNTDOWN && remain <= 10);
            if (showText) {
                gc.setFill(Color.WHITE);
                gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 16));
                gc.fillText(String.valueOf(remain), nX - 8, nY + 6);
            }
        }

        // TẦNG 6: GIAO DIỆN GỠ LỖI (Tia Bezier)
        if (isDebugMode) {
            for (Vehicle v : vehicles) {
                gc.setStroke(Color.CYAN); gc.setLineWidth(1); gc.strokeRect(v.getX() - v.getWidth()/2, v.getY() - v.getHeight()/2, v.getWidth(), v.getHeight());
                if (v.isTurning()) {
                    gc.setStroke(Color.MAGENTA);
                    gc.strokeLine(v.getP0x(), v.getP0y(), v.getP1x(), v.getP1y()); gc.strokeLine(v.getP1x(), v.getP1y(), v.getP2x(), v.getP2y());
                }
            }
        }
        
        gc.restore(); 

        // TẦNG 7: VẼ MƯA RƠI (Bám theo màn hình kính lái)
        if (config.Constants.IS_RAINING) {
            gc.setStroke(Color.rgb(200, 220, 255, 0.6)); 
            gc.setLineWidth(1.5);
            for (int i = 0; i < 300; i++) {
                gc.strokeLine(rainX[i], rainY[i], rainX[i] - 3, rainY[i] + 15);
                rainY[i] += 25; 
                rainX[i] -= 5;
                if (rainY[i] > canvas.getHeight()) {
                    rainY[i] = -20;
                    rainX[i] = Math.random() * canvas.getWidth() + 100;
                }
            }
        }
    }

    private void drawDashedLine(double sx, double sy, double ex, double ey, double offset) {
        double angle = Math.atan2(ey - sy, ex - sx);
        double p1x = sx + Math.cos(angle + Math.PI / 2) * offset;
        double p1y = sy + Math.sin(angle + Math.PI / 2) * offset;
        double p2x = ex + Math.cos(angle + Math.PI / 2) * offset;
        double p2y = ey + Math.sin(angle + Math.PI / 2) * offset;

        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);
        gc.setLineDashes(15, 15); 
        gc.strokeLine(p1x, p1y, p2x, p2y);
        gc.setLineDashes(null); 
    }
    public javafx.scene.canvas.Canvas getCanvas() { return canvas; }
    public void clearAllVehicles() { vehicles.clear(); }
    public void resetCamera() { 
            zoomScale = 1.0; 
            if (!cityMap.getNodes().isEmpty()) {
                IntersectionNode centerNode = cityMap.getNodes().get(0);
                cameraX = centerNode.getX() - (canvas.getWidth() / 2);
                cameraY = centerNode.getY() - (canvas.getHeight() / 2);
            }
        }
    //đổi chế độ đèn
    public void setTrafficLightMode(int modeIndex) {
        IntersectionNode.LightMode mode = IntersectionNode.LightMode.NORMAL; // 0: Không đếm
        if (modeIndex == 1) mode = IntersectionNode.LightMode.COUNTDOWN;     // 1: Đếm toàn thời gian
        else if (modeIndex == 2) mode = IntersectionNode.LightMode.SMART_COUNTDOWN; // 2: Đếm khi <=10s

        for (IntersectionNode node : cityMap.getNodes()) {
            node.setLightMode(mode);
        }
    }
}