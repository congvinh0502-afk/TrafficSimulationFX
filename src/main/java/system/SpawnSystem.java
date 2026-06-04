package system;

import config.Constants;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import model.map.CityMap;
import model.map.RoadEdge;
import model.vehicle.*;

public class SpawnSystem {
    private Random random = new Random();
    private int spawnDelay = 20;
    private boolean spawnEnabled = true;
    private int totalFrames = 0;
    private static final int EMERGENCY_UNLOCK_FRAMES = 480; // ~8 giây ở 60fps

    public void setSpawnDelay(int delay) { this.spawnDelay = delay; }
    public void setSpawnEnabled(boolean enabled) { this.spawnEnabled = enabled; }
    public void resetTimer() { totalFrames = 0; } // Gọi khi đổi map

    public void spawnRandom(List<Vehicle> vehicles, CityMap cityMap) {
        if (!spawnEnabled) return;
        totalFrames++;
        int currentDelay = Math.max(1, spawnDelay);
        if (random.nextInt(currentDelay) != 0) return;

        // Chỉ chọn các đường bắt đầu từ spawn node (ngoài rìa màn hình)
        List<RoadEdge> spawnableRoads = new ArrayList<>();
        for (RoadEdge road : cityMap.getRoads()) {
            if (road.getStartNode().isSpawnNode()) {
                spawnableRoads.add(road);
            }
        }
        // Fallback nếu không có spawn node nào
        if (spawnableRoads.isEmpty()) spawnableRoads = cityMap.getRoads();
        if (spawnableRoads.isEmpty()) return;

        RoadEdge road = spawnableRoads.get(random.nextInt(spawnableRoads.size()));
        double sx = road.getStartNode().getX();
        double sy = road.getStartNode().getY();
        double ex = road.getEndNode().getX();
        double ey = road.getEndNode().getY();
        double angleRad = Math.atan2(ey - sy, ex - sx);

        // Phân phối loại xe: 3% Ambulance, 2% FireTruck, 10% Bicycle, 25% Motorbike, 60% Car
        int randType = random.nextInt(100);

        // Tính offset làn: xe thường dùng 1 trong 2 làn (15px hoặc 45px từ tim đường)
        double offset;
        if (randType < 5) {
            offset = 30; // Xe ưu tiên chạy giữa vạch tim đường
        } else {
            offset = random.nextBoolean() ? 15 : 45;
        }

        double spawnX = sx + Math.cos(angleRad + Math.PI / 2) * offset;
        double spawnY = sy + Math.sin(angleRad + Math.PI / 2) * offset;
        double angle  = Math.toDegrees(angleRad);

        // Xe ưu tiên chỉ xuất hiện sau EMERGENCY_UNLOCK_FRAMES frames (~8 giây)
        boolean emergencyAllowed = totalFrames >= EMERGENCY_UNLOCK_FRAMES;
        if (randType < 5 && !emergencyAllowed) randType = 50; // downgrade thành xe thường

        Vehicle newVehicle;
        if (randType < 3) {
            newVehicle = new EmergencyVehicle(spawnX, spawnY, angle);
        } else if (randType < 5) {
            newVehicle = new FireTruck(spawnX, spawnY, angle);
        } else if (randType < 15) {
            newVehicle = new Bicycle(spawnX, spawnY, angle);
        } else if (randType < 40) {
            newVehicle = new Motorbike(spawnX, spawnY, angle);
        } else {
            newVehicle = new Car(spawnX, spawnY, angle);
        }

        newVehicle.setLaneOffset(offset);

        // Gắn bộ não lái xe (Strategy)
        if (newVehicle instanceof EmergencyVehicle) {
            newVehicle.setStrategy(new model.strategy.EmergencyDriver());
        } else if (newVehicle instanceof Bicycle) {
            newVehicle.setStrategy(new model.strategy.NormalDriver()); // Xe đạp luôn tuân thủ luật
        } else {
            if (random.nextInt(100) < 20) {
                newVehicle.setStrategy(new model.strategy.AggressiveDriver());
            } else {
                newVehicle.setStrategy(new model.strategy.NormalDriver());
            }
        }

        vehicles.add(newVehicle);
    }
}
