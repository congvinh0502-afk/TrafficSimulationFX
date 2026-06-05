package model.map;

import model.traffic.TrafficLight;
import model.traffic.TrafficLight.Phase;

public class IntersectionNode {
    public enum NodeType  { THREE_WAY, FOUR_WAY, FIVE_WAY }
    public enum LightMode { NORMAL, COUNTDOWN, SMART_COUNTDOWN }

    private String id;
    private double x, y;
    private NodeType  type;
    private LightMode lightMode  = LightMode.SMART_COUNTDOWN;
    private boolean   isSpawnNode = false;

    // Đèn cho THREE_WAY / FOUR_WAY
    private TrafficLight lightNorth = new TrafficLight(Phase.GREEN);
    private TrafficLight lightSouth = new TrafficLight(Phase.GREEN);
    private TrafficLight lightEast  = new TrafficLight(Phase.RED);
    private TrafficLight lightWest  = new TrafficLight(Phase.RED);

    // Đèn cho FIVE_WAY — 5 nhánh theo góc thực tế trong IntersectionLayout
    // Thứ tự khớp với branchAngles[] = {270, 342, 54, 126, 198}
    private TrafficLight light270 = new TrafficLight(Phase.GREEN); // nhánh 0 (SOUTH inbound)
    private TrafficLight light342 = new TrafficLight(Phase.RED);   // nhánh 1 (FW_IN_342)
    private TrafficLight light54  = new TrafficLight(Phase.RED);   // nhánh 2 (FW_IN_54)
    private TrafficLight light126 = new TrafficLight(Phase.RED);   // nhánh 3 (FW_IN_126)
    private TrafficLight light198 = new TrafficLight(Phase.RED);   // nhánh 4 (FW_IN_198)

    // Hướng kết nối cho THREE_WAY / FOUR_WAY
    private boolean hasNorth, hasSouth, hasEast, hasWest;

    // Hướng kết nối cho FIVE_WAY (tương ứng 5 nhánh góc)
    private boolean has270, has342, has54, has126, has198;

    private double phaseTimer   = 0;
    private int    currentPhase = 0;

    private static final double GREEN_DURATION  = 15.0;
    private static final double YELLOW_DURATION = 3.0;

    // -------- Constructors --------

    public IntersectionNode(String id, double x, double y, NodeType type) {
        this.id = id; this.x = x; this.y = y; this.type = type;
        applyPhaseStates();
    }

    public IntersectionNode(String id, double x, double y, NodeType type, boolean isSpawnNode) {
        this(id, x, y, type);
        this.isSpawnNode = isSpawnNode;
    }

    // -------- Direction setup --------

    /**
     * Gọi bởi CityMap cho THREE_WAY / FOUR_WAY.
     * Với FIVE_WAY hãy dùng setConnectedDirectionsFiveWay().
     */
    public void setConnectedDirections(boolean n, boolean s, boolean e, boolean w) {
        hasNorth = n; hasSouth = s; hasEast = e; hasWest = w;
        phaseTimer   = 0;
        currentPhase = 0;
        applyPhaseStates();
    }

    /**
     * Gọi bởi CityMap cho FIVE_WAY.
     * Thứ tự tham số khớp với branchAngles[] = {270, 342, 54, 126, 198}.
     */
    public void setConnectedDirectionsFiveWay(
            boolean b270, boolean b342, boolean b54,
            boolean b126, boolean b198) {
        has270 = b270; has342 = b342; has54 = b54;
        has126 = b126; has198 = b198;
        phaseTimer   = 0;
        currentPhase = 0;
        applyPhaseStates();
    }

    // -------- Light update --------

    public void updateLights() {
        if (!config.Constants.AUTO_LIGHTS) return;

        double dt = 1.0 / 60.0;
        phaseTimer += dt;

        if (type == NodeType.FIVE_WAY) {
            // 5 pha xoay vòng, mỗi pha = GREEN_DURATION
            if (phaseTimer >= GREEN_DURATION) {
                phaseTimer   = 0;
                currentPhase = (currentPhase + 1) % 5;
                applyPhaseStates();
            }
        } else {
            // THREE_WAY / FOUR_WAY: 4 pha (green/yellow x2)
            double duration = (currentPhase % 2 == 0) ? GREEN_DURATION : YELLOW_DURATION;
            if (phaseTimer >= duration) {
                phaseTimer   = 0;
                currentPhase = (currentPhase + 1) % 4;
                applyPhaseStates();
            }
        }
    }

    public void manualToggle() {
        int maxPhases = (type == NodeType.FIVE_WAY) ? 5 : 4;
        currentPhase = (currentPhase + 1) % maxPhases;
        phaseTimer   = 0;
        applyPhaseStates();
    }

    public double getRemainingTime() {
        if (type == NodeType.FIVE_WAY) return GREEN_DURATION - phaseTimer;
        double duration = (currentPhase % 2 == 0) ? GREEN_DURATION : YELLOW_DURATION;
        return duration - phaseTimer;
    }

    // -------- Phase state machines --------

    private void applyPhaseStates() {
        switch (type) {
            case FIVE_WAY  -> applyFiveWayPhase();
            case THREE_WAY -> applyThreeWayPhase();
            default        -> applyFourWayPhase();
        }
    }

    /**
     * Mỗi pha: đúng 1 nhánh GREEN, 4 nhánh còn lại RED.
     * Thứ tự pha khớp với branchAngles[] trong IntersectionLayout:
     *   pha 0 → 270° (light270)
     *   pha 1 → 342° (light342)
     *   pha 2 →  54° (light54)
     *   pha 3 → 126° (light126)
     *   pha 4 → 198° (light198)
     */
    private void applyFiveWayPhase() {
        light270.setPhase((currentPhase == 0 && has270) ? Phase.GREEN : Phase.RED);
        light342.setPhase((currentPhase == 1 && has342) ? Phase.GREEN : Phase.RED);
        light54 .setPhase((currentPhase == 2 && has54)  ? Phase.GREEN : Phase.RED);
        light126.setPhase((currentPhase == 3 && has126) ? Phase.GREEN : Phase.RED);
        light198.setPhase((currentPhase == 4 && has198) ? Phase.GREEN : Phase.RED);
    }

    private void applyThreeWayPhase() {
        boolean vertArm = hasNorth || hasSouth;
        switch (currentPhase) {
            case 0 -> {
                lightWest .setPhase(hasWest  ? Phase.GREEN  : Phase.RED);
                lightEast .setPhase(hasEast  ? Phase.GREEN  : Phase.RED);
                lightNorth.setPhase(Phase.RED);
                lightSouth.setPhase(Phase.RED);
            }
            case 1 -> {
                lightWest .setPhase(hasWest  ? Phase.YELLOW : Phase.RED);
                lightEast .setPhase(hasEast  ? Phase.YELLOW : Phase.RED);
                lightNorth.setPhase(Phase.RED);
                lightSouth.setPhase(Phase.RED);
            }
            case 2 -> {
                lightWest .setPhase(Phase.RED);
                lightEast .setPhase(Phase.RED);
                lightNorth.setPhase(hasNorth && vertArm ? Phase.GREEN  : Phase.RED);
                lightSouth.setPhase(hasSouth && vertArm ? Phase.GREEN  : Phase.RED);
            }
            case 3 -> {
                lightWest .setPhase(Phase.RED);
                lightEast .setPhase(Phase.RED);
                lightNorth.setPhase(hasNorth && vertArm ? Phase.YELLOW : Phase.RED);
                lightSouth.setPhase(hasSouth && vertArm ? Phase.YELLOW : Phase.RED);
            }
        }
    }

    private void applyFourWayPhase() {
        switch (currentPhase) {
            case 0 -> { lightNorth.setPhase(Phase.GREEN);  lightSouth.setPhase(Phase.GREEN);
                        lightEast .setPhase(Phase.RED);    lightWest .setPhase(Phase.RED);   }
            case 1 -> { lightNorth.setPhase(Phase.YELLOW); lightSouth.setPhase(Phase.YELLOW);
                        lightEast .setPhase(Phase.RED);    lightWest .setPhase(Phase.RED);   }
            case 2 -> { lightNorth.setPhase(Phase.RED);    lightSouth.setPhase(Phase.RED);
                        lightEast .setPhase(Phase.GREEN);  lightWest .setPhase(Phase.GREEN); }
            case 3 -> { lightNorth.setPhase(Phase.RED);    lightSouth.setPhase(Phase.RED);
                        lightEast .setPhase(Phase.YELLOW); lightWest .setPhase(Phase.YELLOW);}
        }
    }

    // -------- Getters --------

    public boolean isSpawnNode()    { return isSpawnNode; }

    // THREE_WAY / FOUR_WAY
    public boolean isHasNorth()     { return hasNorth; }
    public boolean isHasSouth()     { return hasSouth; }
    public boolean isHasEast()      { return hasEast; }
    public boolean isHasWest()      { return hasWest; }
    public TrafficLight getLightNorth() { return lightNorth; }
    public TrafficLight getLightSouth() { return lightSouth; }
    public TrafficLight getLightEast()  { return lightEast; }
    public TrafficLight getLightWest()  { return lightWest; }

    // FIVE_WAY — 5 nhánh góc thực
    public boolean isHas270() { return has270; }
    public boolean isHas342() { return has342; }
    public boolean isHas54()  { return has54;  }
    public boolean isHas126() { return has126; }
    public boolean isHas198() { return has198; }
    public TrafficLight getLight270() { return light270; }
    public TrafficLight getLight342() { return light342; }
    public TrafficLight getLight54()  { return light54;  }
    public TrafficLight getLight126() { return light126; }
    public TrafficLight getLight198() { return light198; }

    public String    getId()        { return id; }
    public double    getX()         { return x; }
    public double    getY()         { return y; }
    public NodeType  getType()      { return type; }
    public LightMode getLightMode() { return lightMode; }
    public void setLightMode(LightMode m) { this.lightMode = m; }
    public int getCurrentPhase()    { return currentPhase; }
}