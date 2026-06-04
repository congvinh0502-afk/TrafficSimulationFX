package config;

public class Constants {
    // Kích thước cửa sổ
    public static final double WINDOW_WIDTH = 1280;
    public static final double WINDOW_HEIGHT = 800;
    
    // Cấu hình đường sá
    public static final double LANE_WIDTH = 30;
    public static final double ROAD_WIDTH = LANE_WIDTH * 4; // 120px tổng

    // Tốc độ cơ sở (px/frame) cho từng loại xe – nhỏ hơn để mô phỏng thực tế hơn
    public static final double BASE_CAR       = 2.2;
    public static final double BASE_MOTORBIKE = 2.6;
    public static final double BASE_BICYCLE   = 0.9;
    public static final double BASE_AMBULANCE = 3.5;
    public static final double BASE_FIRETRUCK = 3.2;
    // THÊM CÔNG TẮC THỜI TIẾT
    public static boolean IS_RAINING = false;
    // ---> THÊM DÒNG NÀY: Công tắc bật tắt đèn giao thông tự động
    public static boolean AUTO_LIGHTS = true;
    // ---> BƯỚC 20: HỆ SỐ TỐC ĐỘ TỪNG LOẠI XE (Mặc định x1.0)
    public static double SPEED_CAR = 1.0;
    public static double SPEED_MOTORBIKE = 1.0;
    public static double SPEED_BICYCLE = 1.0;
    public static double SPEED_AMBULANCE = 1.0;
    public static double SPEED_FIRETRUCK = 1.0;
    // ---> BƯỚC 21: CẤU HÌNH THỜI GIAN VÀ ĐÈN
    public static int TIME_MODE = 0; // 0: Chu kỳ tự động, 1: Luôn Ban Ngày, 2: Luôn Ban Đêm
}