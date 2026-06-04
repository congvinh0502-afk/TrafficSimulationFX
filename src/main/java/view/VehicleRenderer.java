package view;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import model.vehicle.*;

public class VehicleRenderer {

    public static boolean BASIC_MODE = false;

    // =====================================================
    // TẦNG 1: VẼ THÂN XE
    // =====================================================
    public static void drawCarBody(GraphicsContext gc, Vehicle v) {
        gc.save();
        gc.translate(v.getX(), v.getY());
        gc.rotate(v.getAngle());

        if (BASIC_MODE) {
            // --- CHẾ ĐỘ BASIC: Hình chữ nhật màu + nhãn tên ---
            gc.setFill(v.getColor());
            gc.fillRect(-v.getWidth() / 2, -v.getHeight() / 2, v.getWidth(), v.getHeight());

            // Viền trắng
            gc.setStroke(Color.WHITE.deriveColor(0, 1, 1, 0.6));
            gc.setLineWidth(1);
            gc.strokeRect(-v.getWidth() / 2, -v.getHeight() / 2, v.getWidth(), v.getHeight());

            // Nhãn loại xe
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 7));
            gc.fillText(getVehicleLabel(v), -v.getWidth() / 2 + 2, 3);

        } else {
            // --- CHẾ ĐỘ ĐỒ HỌA: Render chi tiết ---
            Color bodyColor = v.getColor();

            // Thân xe (bo góc)
            gc.setFill(bodyColor);
            gc.fillRoundRect(-v.getWidth() / 2, -v.getHeight() / 2, v.getWidth(), v.getHeight(), 8, 8);

            // Highlight phía trên (phản quang mái xe)
            gc.setFill(Color.WHITE.deriveColor(0, 1, 1, 0.18));
            gc.fillRoundRect(-v.getWidth() / 2 + 1, -v.getHeight() / 2 + 1,
                    v.getWidth() - 2, v.getHeight() / 2 - 1, 6, 6);

            // Kính lái (màu xanh lam nhạt)
            gc.setFill(Color.web("#87ceeb", 0.85));
            gc.fillRoundRect(v.getWidth() / 4 - 2, -v.getHeight() / 2 + 2,
                    9, v.getHeight() - 4, 3, 3);

            // Kính hậu (nhỏ hơn)
            gc.setFill(Color.web("#87ceeb", 0.5));
            gc.fillRect(-v.getWidth() / 2 + 3, -v.getHeight() / 2 + 3,
                    6, v.getHeight() - 6);

            // Logo chữ thập đỏ cho xe cứu thương & cứu hỏa
            if (v instanceof EmergencyVehicle) {
                gc.setFill(Color.RED.deriveColor(0, 1, 1, 0.9));
                double cx = -v.getWidth() / 6;
                double cy2 = 0;
                gc.fillRect(cx - 1.5, -v.getHeight() / 2 + 4, 3, v.getHeight() - 8); // Dọc
                gc.fillRect(cx - 5, cy2 - 1.5, 10, 3);                                // Ngang
            }

            // Sọc đỏ xe cứu hỏa
            if (v instanceof FireTruck) {
                gc.setFill(Color.WHITE.deriveColor(0, 1, 1, 0.4));
                gc.fillRect(-v.getWidth() / 2 + 5, -v.getHeight() / 2 + 2, 4, v.getHeight() - 4);
                gc.fillRect(-v.getWidth() / 2 + 12, -v.getHeight() / 2 + 2, 4, v.getHeight() - 4);
            }

            // Viền xe (outline)
            gc.setStroke(bodyColor.darker().darker());
            gc.setLineWidth(0.8);
            gc.strokeRoundRect(-v.getWidth() / 2, -v.getHeight() / 2, v.getWidth(), v.getHeight(), 8, 8);
        }

        gc.restore();
    }

    // =====================================================
    // TẦNG 2: VẼ ĐÈN XE (đèn pha, đèn hậu, đèn ưu tiên, hazard)
    // =====================================================
    public static void drawLights(GraphicsContext gc, Vehicle v, double darkness) {
        gc.save();
        gc.translate(v.getX(), v.getY());
        gc.rotate(v.getAngle());

        // 1. Đèn pha (headlights) - chỉ bật khi trời tối, kích thước nhỏ vừa nhìn
        if (darkness > 0.2) {
            gc.setGlobalBlendMode(BlendMode.ADD);
            gc.setFill(Color.rgb(255, 255, 100, 0.55 * darkness));
            double[] hx = {v.getWidth() / 2, v.getWidth() / 2 + 28, v.getWidth() / 2 + 28};
            double[] hy = {0, -9, 9};
            gc.fillPolygon(hx, hy, 3);
            gc.setGlobalBlendMode(BlendMode.SRC_OVER);
        }

        // 2. Đèn hậu (taillights) - sáng khi phanh
        Color tailColor = (v.getSpeed() < 1.0)
                ? Color.rgb(255, 50, 50, 0.95)
                : Color.rgb(255, 0, 0, 0.35);
        gc.setFill(tailColor);
        gc.fillOval(-v.getWidth() / 2 - 3, -v.getHeight() / 2 + 2, 5, 5);
        gc.fillOval(-v.getWidth() / 2 - 3, v.getHeight() / 2 - 7, 5, 5);

        // 3. Đèn ưu tiên (đỏ/xanh nháy) cho xe cứu thương & cứu hỏa
        if (v instanceof EmergencyVehicle) {
            long t = System.currentTimeMillis();
            boolean blink = (t % 400) < 200;
            // Đèn trái
            gc.setFill(blink ? Color.RED : Color.BLUE);
            gc.fillOval(-8, -v.getHeight() / 2 - 4, 7, 7);
            // Đèn phải
            gc.setFill(blink ? Color.BLUE : Color.RED);
            gc.fillOval(2, -v.getHeight() / 2 - 4, 7, 7);
            // Ánh sáng lan toả (additive)
            if (!BASIC_MODE) {
                gc.setGlobalBlendMode(BlendMode.ADD);
                gc.setFill(blink ? Color.rgb(255, 0, 0, 0.15) : Color.rgb(0, 80, 255, 0.15));
                gc.fillOval(-30, -30, 60, 60);
                gc.setGlobalBlendMode(BlendMode.SRC_OVER);
            }
        }

        // 4. Đèn hazard (hỏng xe - nháy vàng cam 4 góc)
        if (v.isBroken() && System.currentTimeMillis() % 800 < 400) {
            gc.setFill(Color.ORANGE);
            gc.fillOval(-v.getWidth() / 2 - 3, -v.getHeight() / 2 - 3, 7, 7);
            gc.fillOval(-v.getWidth() / 2 - 3, v.getHeight() / 2 - 4, 7, 7);
            gc.fillOval(v.getWidth() / 2 - 4, -v.getHeight() / 2 - 3, 7, 7);
            gc.fillOval(v.getWidth() / 2 - 4, v.getHeight() / 2 - 4, 7, 7);
        }

        gc.restore();
    }

    // =====================================================
    // Trả về nhãn ngắn gọn cho Basic Mode
    // =====================================================
    private static String getVehicleLabel(Vehicle v) {
        if (v instanceof FireTruck)      return "Fire";
        if (v instanceof EmergencyVehicle) return "Ambu";
        if (v instanceof Bicycle)        return "Bike";
        if (v instanceof Motorbike)      return "Moto";
        return "Car";
    }
}
