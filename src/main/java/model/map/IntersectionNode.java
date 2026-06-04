package model.map;

import java.util.Random;
import model.traffic.TrafficLight;

public class IntersectionNode {
    public enum NodeType { THREE_WAY, FOUR_WAY, FIVE_WAY }
    // 3 Chế độ hiển thị yêu cầu của Đề tài
    public enum LightMode { NORMAL, COUNTDOWN, SMART_COUNTDOWN } 

    private String id;
    private double x, y;
    private NodeType type;
    private LightMode lightMode;
    private boolean isSpawnNode = false; // Node ngoài rìa map, chỉ dùng để spawn xe
    
    private TrafficLight lightNorth = new TrafficLight(TrafficLight.Phase.GREEN);
    private TrafficLight lightSouth = new TrafficLight(TrafficLight.Phase.GREEN);
    private TrafficLight lightEast = new TrafficLight(TrafficLight.Phase.RED);
    private TrafficLight lightWest = new TrafficLight(TrafficLight.Phase.RED);

    private double phaseTimer = 0;
    private int currentPhase = 0; 
    
    private static final double GREEN_DURATION = 18.0;
    private static final double YELLOW_DURATION = 3.0;

    public IntersectionNode(String id, double x, double y, NodeType type) {
        this.id = id; this.x = x; this.y = y; this.type = type;
        applyPhaseStates();
        this.lightMode = LightMode.SMART_COUNTDOWN;
    }

    public IntersectionNode(String id, double x, double y, NodeType type, boolean isSpawnNode) {
        this(id, x, y, type);
        this.isSpawnNode = isSpawnNode;
        
    }
    
    public void updateLights() {
        if (type == NodeType.FIVE_WAY) return;
        if (!config.Constants.AUTO_LIGHTS) return; // Nếu Tắt tự động -> Đóng băng thời gian

        double dt = 1.0 / 60.0;
        phaseTimer += dt;
        double duration = (currentPhase == 0 || currentPhase == 2) ? GREEN_DURATION : YELLOW_DURATION;
        
        if (phaseTimer >= duration) {
            phaseTimer = 0;
            currentPhase = (currentPhase + 1) % 4;
            applyPhaseStates();
        }
    }

    // ---> MỚI: HÀM ĐỔI ĐÈN THỦ CÔNG KHI NGƯỜI DÙNG CLICK
    public void manualToggle() {
        if (type == NodeType.FIVE_WAY) return;
        currentPhase = (currentPhase + 1) % 4;
        phaseTimer = 0;
        applyPhaseStates();
    }

    // Lấy thời gian đếm ngược còn lại
    public double getRemainingTime() {
        double duration = (currentPhase == 0 || currentPhase == 2) ? GREEN_DURATION : YELLOW_DURATION;
        return duration - phaseTimer;
    }

    private void applyPhaseStates() {
        switch (currentPhase) {
            case 0 -> { lightNorth.setPhase(TrafficLight.Phase.GREEN); lightSouth.setPhase(TrafficLight.Phase.GREEN); lightEast.setPhase(TrafficLight.Phase.RED); lightWest.setPhase(TrafficLight.Phase.RED); }
            case 1 -> { lightNorth.setPhase(TrafficLight.Phase.YELLOW); lightSouth.setPhase(TrafficLight.Phase.YELLOW); lightEast.setPhase(TrafficLight.Phase.RED); lightWest.setPhase(TrafficLight.Phase.RED); }
            case 2 -> { lightNorth.setPhase(TrafficLight.Phase.RED); lightSouth.setPhase(TrafficLight.Phase.RED); lightEast.setPhase(TrafficLight.Phase.GREEN); lightWest.setPhase(TrafficLight.Phase.GREEN); }
            case 3 -> { lightNorth.setPhase(TrafficLight.Phase.RED); lightSouth.setPhase(TrafficLight.Phase.RED); lightEast.setPhase(TrafficLight.Phase.YELLOW); lightWest.setPhase(TrafficLight.Phase.YELLOW); }
        }
    }

    public boolean isSpawnNode() { return isSpawnNode; }
    public TrafficLight getLightNorth() { return lightNorth; } public TrafficLight getLightSouth() { return lightSouth; }
    public TrafficLight getLightEast() { return lightEast; } public TrafficLight getLightWest() { return lightWest; }
    public String getId() { return id; } public double getX() { return x; } public double getY() { return y; }
    public NodeType getType() { return type; } public LightMode getLightMode() { return lightMode; }
    // ---> THÊM HÀM NÀY VÀO CUỐI CLASS (Trước các hàm get)
    public void setLightMode(LightMode mode) {
        this.lightMode = mode;
    }
}