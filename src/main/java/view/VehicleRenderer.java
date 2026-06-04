package view;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import model.vehicle.Vehicle;
import model.vehicle.EmergencyVehicle;
import javafx.scene.effect.BlendMode;
public class VehicleRenderer {
    // ---> BẮT BUỘC PHẢI CÓ DÒNG NÀY Ở ĐÂY (Ngay dưới tên class)
    public static boolean BASIC_MODE = false;
    
    // BƯỚC 1: LỚP VẬT THỂ (Bị tối đi khi trời tối)
    public static void drawCarBody(GraphicsContext gc, Vehicle v) {
        gc.save();
        gc.translate(v.getX(), v.getY());
        gc.rotate(v.getAngle());
        
        gc.setFill(v.getColor());
        gc.fillRoundRect(-v.getWidth() / 2, -v.getHeight() / 2, v.getWidth(), v.getHeight(), 8, 8);
        
        gc.setFill(Color.web("#87ceeb")); // Kính lái
        gc.fillRect(v.getWidth() / 4 - 2, -v.getHeight() / 2 + 2, 8, v.getHeight() - 4);
        
        gc.restore(); 
    }

    // BƯỚC 3: LỚP PHÁT SÁNG (Sáng rực rỡ xuyên qua màn đêm)
    public static void drawLights(GraphicsContext gc, Vehicle v, double darkness) {
        gc.save();
        gc.translate(v.getX(), v.getY());
        gc.rotate(v.getAngle());

        // 1. ĐÈN PHA (Headlights)
        if (darkness > 0.2) {
            // ---> BẬT CHẾ ĐỘ CỘNG ÁNH SÁNG (Additive Blending)
            gc.setGlobalBlendMode(BlendMode.ADD); 
            
            gc.setFill(Color.rgb(255, 255, 100, 0.4 * darkness)); 
            double[] hx = {v.getWidth()/2, v.getWidth()/2 + 130, v.getWidth()/2 + 130};
            double[] hy = {0, -50, 50};
            gc.fillPolygon(hx, hy, 3);
            
            // ---> TRẢ LẠI CHẾ ĐỘ VẼ BÌNH THƯỜNG CHO CÁC VẬT KHÁC
            gc.setGlobalBlendMode(BlendMode.SRC_OVER); 
        }

        // 2. ĐÈN HẬU (Taillights)
        // Nếu xe đang phanh (tốc độ < 1.0), đèn chói lóa lên. Nếu đi bình thường thì sáng mờ.
        if (v.getSpeed() < 1.0) {
            gc.setFill(Color.rgb(255, 50, 50, 0.9)); 
        } else {
            gc.setFill(Color.rgb(255, 0, 0, 0.3)); 
        }
        gc.fillOval(-v.getWidth()/2 - 2, -v.getHeight()/2 + 2, 4, 4);
        gc.fillOval(-v.getWidth()/2 - 2, v.getHeight()/2 - 6, 4, 4);
        
        // 3. ĐÈN ƯU TIÊN CỨU THƯƠNG
        if (v instanceof EmergencyVehicle) {
            if (System.currentTimeMillis() % 400 < 200) gc.setFill(Color.RED);
            else gc.setFill(Color.BLUE);
            gc.fillOval(-5, -5, 10, 10);
        }
        // 4. ĐÈN HAZARD (Sự cố hỏng xe - Nháy vàng cam cả 4 góc)
        if (v.isBroken() && System.currentTimeMillis() % 800 < 400) {
            gc.setFill(Color.ORANGE);
            gc.fillOval(-v.getWidth()/2 - 2, -v.getHeight()/2 - 2, 6, 6); // Trái sau
            gc.fillOval(-v.getWidth()/2 - 2, v.getHeight()/2 - 4, 6, 6);  // Phải sau
            gc.fillOval(v.getWidth()/2 - 4, -v.getHeight()/2 - 2, 6, 6);  // Trái trước
            gc.fillOval(v.getWidth()/2 - 4, v.getHeight()/2 - 4, 6, 6);   // Phải trước
        }
        
        gc.restore();
    }
}