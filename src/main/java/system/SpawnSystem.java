package system;

import config.Constants;
import java.util.List;
import java.util.Random;
import model.map.CityMap;
import model.map.RoadEdge;
import model.vehicle.Car;
import model.vehicle.Vehicle;

public class SpawnSystem {
    private Random random = new Random();
    private int spawnDelay = 20; // Tốc độ sinh xe mặc định (Càng nhỏ xe càng đông)

    public void setSpawnDelay(int delay) {
        this.spawnDelay = delay;
    }

    public void spawnRandom(List<Vehicle> vehicles, CityMap cityMap) {
        int currentDelay = Math.max(1, spawnDelay); 
        
        if (random.nextInt(currentDelay) != 0) return;
        if (cityMap.getRoads().isEmpty()) return;
        
        RoadEdge road = cityMap.getRoads().get(random.nextInt(cityMap.getRoads().size()));
        double sx = road.getStartNode().getX();
        double sy = road.getStartNode().getY();
        double ex = road.getEndNode().getX();
        double ey = road.getEndNode().getY();
        
        double angleRad = Math.atan2(ey - sy, ex - sx);
        
        // --- 1. BỐC THĂM LOẠI XE VÀ CHỌN LÀN ĐƯỜNG ---
        int randType = random.nextInt(100);
        double offset;
        
        if (randType < 5) {
            // Xe cứu thương đi cực gắt: Chạy đè vạch phân làn ở giữa (30px)
            offset = 30; 
        } else {
            // Xe thường: Bốc thăm Làn 1 (45px) hoặc Làn 2 (15px)
            offset = random.nextBoolean() ? 45 : 15; 
        }
        
        // --- 2. TÍNH TỌA ĐỘ VÀ TẠO XE ---
        double spawnX = sx + Math.cos(angleRad + Math.PI/2) * offset;
        double spawnY = sy + Math.sin(angleRad + Math.PI/2) * offset;
        
        Vehicle newVehicle;
        if (randType < 5) {
            newVehicle = new model.vehicle.EmergencyVehicle(spawnX, spawnY, Math.toDegrees(angleRad));
        } else if (randType < 35) {
            newVehicle = new model.vehicle.Motorbike(spawnX, spawnY, Math.toDegrees(angleRad));
        } else {
            newVehicle = new Car(spawnX, spawnY, Math.toDegrees(angleRad));
        }
        
        newVehicle.setLaneOffset(offset);
        // ---> MỚI: LẮP RÁP BỘ NÃO (STRATEGY) CHO TÀI XẾ
        if (newVehicle instanceof model.vehicle.EmergencyVehicle) {
            newVehicle.setStrategy(new model.strategy.EmergencyDriver()); // Xe ưu tiên
        } else {
            // Ô tô/Xe máy bình thường: 20% xác suất gặp tài xế "hổ báo", 80% là tài xế ngoan
            if (random.nextInt(100) < 20) {
                newVehicle.setStrategy(new model.strategy.AggressiveDriver());
            } else {
                newVehicle.setStrategy(new model.strategy.NormalDriver());
            }
        }
        vehicles.add(newVehicle);
    }
}