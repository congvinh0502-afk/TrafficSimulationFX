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
            // --- CHẾ ĐỘ ĐỒ HỌA ---
            double hw = v.getWidth() / 2.0, hh = v.getHeight() / 2.0;

            // Bóng đổ gầm xe
            gc.setFill(Color.rgb(0, 0, 0, 0.20));
            gc.fillOval(-hw + 3, hh - 1, hw * 2 - 6, hh + 3);

            if (v instanceof FireTruck) {
                drawFireTruck(gc, v, hw, hh);
            } else if (v instanceof EmergencyVehicle) {
                drawAmbulance(gc, v, hw, hh);
            } else if (v instanceof Bicycle) {
                drawBicycle(gc, v, hw, hh);
            } else if (v instanceof Motorbike) {
                drawMotorbike(gc, v, hw, hh);
            } else {
                drawCar(gc, v, hw, hh);
            }
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
    // VEHICLE DETAIL HELPERS (graphic mode)
    // =====================================================

    private static void drawCar(GraphicsContext gc, Vehicle v, double hw, double hh) {
        Color body = v.getColor();
        // 4 wheels
        gc.setFill(Color.web("#1a1a1a"));
        double wl = 7, wt = 4, wfx = hw - 8;
        gc.fillRoundRect( wfx - wl/2,  hh - wt/2, wl, wt, 2, 2); // front-right
        gc.fillRoundRect( wfx - wl/2, -hh - wt/2, wl, wt, 2, 2); // front-left
        gc.fillRoundRect(-wfx - wl/2,  hh - wt/2, wl, wt, 2, 2); // rear-right
        gc.fillRoundRect(-wfx - wl/2, -hh - wt/2, wl, wt, 2, 2); // rear-left
        // Body
        gc.setFill(body); gc.fillRoundRect(-hw, -hh, hw*2, hh*2, 8, 8);
        // Roof highlight
        gc.setFill(Color.WHITE.deriveColor(0,1,1,0.18));
        gc.fillRoundRect(-hw+1, -hh+1, hw*2-2, hh-1, 6, 6);
        // Windshield
        gc.setFill(Color.web("#87ceeb", 0.85));
        gc.fillRoundRect(hw*0.38, -hh+2, hw*0.52, hh*2-4, 3, 3);
        // Rear window
        gc.setFill(Color.web("#87ceeb", 0.55));
        gc.fillRect(-hw+2, -hh+3, hw*0.35, hh*2-6);
        // Side mirrors
        gc.setFill(body.darker());
        gc.fillRect(hw*0.25-1, -hh-2, 4, 2);
        gc.fillRect(hw*0.25-1,  hh,   4, 2);
        // Headlights
        gc.setFill(Color.web("#fffde0")); gc.fillOval(hw-4, -hh+2, 4, 4);
        gc.setFill(Color.web("#fffde0")); gc.fillOval(hw-4,  hh-6, 4, 4);
        // Taillights
        gc.setFill(Color.web("#ff4444")); gc.fillOval(-hw, -hh+2, 4, 4);
        gc.setFill(Color.web("#ff4444")); gc.fillOval(-hw,  hh-6, 4, 4);
        // Outline
        gc.setStroke(body.darker().darker()); gc.setLineWidth(0.8);
        gc.strokeRoundRect(-hw, -hh, hw*2, hh*2, 8, 8);
    }

    private static void drawMotorbike(GraphicsContext gc, Vehicle v, double hw, double hh) {
        Color body = v.getColor();
        // Wheels
        gc.setFill(Color.web("#1a1a1a"));
        gc.fillOval( hw - 5, -3, 6, 6);  // front
        gc.fillOval(-hw,     -3, 6, 6);  // rear
        // Handlebar
        gc.setStroke(Color.web("#888888")); gc.setLineWidth(1.5);
        gc.strokeLine(hw*0.55, -hh, hw*0.55, hh);
        // Frame/body
        gc.setFill(body); gc.fillRoundRect(-hw+4, -hh*0.6, hw*2-8, hh*1.2, 4, 4);
        gc.setFill(body.brighter());
        gc.fillRect(-hw+5, -hh*0.55, (hw*2-10)*0.5, hh*1.1);
        // Rider helmet
        gc.setFill(Color.web("#2c3e50")); gc.fillOval(-5, -hh-5, 10, 8);
        // Rider shoulders
        gc.setFill(Color.web("#555555")); gc.fillRect(-4, -hh+1, 8, 5);
        gc.setStroke(body.darker()); gc.setLineWidth(0.5);
        gc.strokeRoundRect(-hw+4, -hh*0.6, hw*2-8, hh*1.2, 4, 4);
    }

    private static void drawBicycle(GraphicsContext gc, Vehicle v, double hw, double hh) {
        // Wheels (thin ring style)
        gc.setStroke(Color.web("#222222")); gc.setLineWidth(1.8);
        gc.strokeOval( hw-5, -4, 8, 8);
        gc.strokeOval(-hw-3, -4, 8, 8);
        // Frame
        gc.setStroke(Color.web("#888888")); gc.setLineWidth(1.2);
        gc.strokeLine(-hw+4, 0, hw-2, -hh+1);
        gc.strokeLine(-hw+4, 0, hw-2,  hh-1);
        gc.strokeLine(-hw+4, 0, hw-6,  0);
        // Handlebar
        gc.strokeLine(hw-5, -hh, hw-5, hh);
        // Rider
        gc.setFill(v.getColor()); gc.fillOval(-3, -hh, 6, 6); // helmet
        gc.setFill(Color.web("#555555")); gc.fillRect(-3, -hh+5, 6, 4); // body
    }

    private static void drawAmbulance(GraphicsContext gc, Vehicle v, double hw, double hh) {
        // 4 wheels
        gc.setFill(Color.web("#1a1a1a"));
        double wl=8, wt=4.5, wfx=hw-9;
        gc.fillRoundRect( wfx-wl/2,  hh-wt/2, wl, wt, 2, 2);
        gc.fillRoundRect( wfx-wl/2, -hh-wt/2, wl, wt, 2, 2);
        gc.fillRoundRect(-wfx-wl/2,  hh-wt/2, wl, wt, 2, 2);
        gc.fillRoundRect(-wfx-wl/2, -hh-wt/2, wl, wt, 2, 2);
        // White body
        gc.setFill(Color.web("#ecf0f1")); gc.fillRoundRect(-hw, -hh, hw*2, hh*2, 6, 6);
        // Red stripe along side
        gc.setFill(Color.web("#e74c3c")); gc.fillRect(-hw, hh*0.4, hw*2, hh*0.6);
        // Red cross
        gc.setFill(Color.web("#e74c3c"));
        gc.fillRect(-hw/4-1.5, -hh+3, 3, hh*2-6);
        gc.fillRect(-hw/4-6,   -1.5,  12, 3);
        // Priority light bar on roof
        long t2 = System.currentTimeMillis();
        boolean blink = (t2 % 400) < 200;
        gc.setFill(blink ? Color.RED : Color.BLUE);    gc.fillRect(-5, -hh-4, 5, 3);
        gc.setFill(blink ? Color.BLUE : Color.RED);    gc.fillRect( 1, -hh-4, 5, 3);
        // Windshield
        gc.setFill(Color.web("#87ceeb", 0.6)); gc.fillRect(hw-9, -hh+2, 8, hh*2-4);
        gc.setStroke(Color.web("#bdc3c7")); gc.setLineWidth(0.8); gc.strokeRoundRect(-hw,-hh,hw*2,hh*2,6,6);
    }

    private static void drawFireTruck(GraphicsContext gc, Vehicle v, double hw, double hh) {
        gc.setFill(Color.web("#1a1a1a"));
        double wl=9, wt=5, wfx=hw-10;
        gc.fillRoundRect( wfx-wl/2,  hh-wt/2, wl, wt, 2, 2);
        gc.fillRoundRect( wfx-wl/2, -hh-wt/2, wl, wt, 2, 2);
        gc.fillRoundRect(-wfx-wl/2,  hh-wt/2, wl, wt, 2, 2);
        gc.fillRoundRect(-wfx-wl/2, -hh-wt/2, wl, wt, 2, 2);
        // Red body
        gc.setFill(v.getColor()); gc.fillRoundRect(-hw, -hh, hw*2, hh*2, 5, 5);
        // Yellow reflective strips
        gc.setFill(Color.web("#f1c40f",0.85));
        gc.fillRect(-hw, -hh*0.18, hw*2, hh*0.36);
        // Silver ladder on roof
        gc.setStroke(Color.web("#bdc3c7")); gc.setLineWidth(1.2);
        gc.strokeLine(-hw+6, -hh*0.8, hw-8, -hh*0.8);
        gc.strokeLine(-hw+6,  hh*0.8, hw-8,  hh*0.8);
        for (double rx=-hw+8; rx<hw-10; rx+=8)
            gc.strokeLine(rx, -hh*0.8, rx, hh*0.8);
        // White side stripe
        gc.setFill(Color.WHITE.deriveColor(0,1,1,0.35));
        gc.fillRect(-hw+4, -hh+2, 5, hh*2-4);
        // Windshield
        gc.setFill(Color.web("#87ceeb",0.6)); gc.fillRect(hw-10, -hh+2, 9, hh*2-4);
        gc.setStroke(v.getColor().darker().darker()); gc.setLineWidth(0.8);
        gc.strokeRoundRect(-hw, -hh, hw*2, hh*2, 5, 5);
    }

    // =====================================================
    // Trả về nhãn ngắn gọn cho Basic Mode
    // =====================================================
    private static String getVehicleLabel(Vehicle v) {
        if (v instanceof FireTruck)        return "Fire";
        if (v instanceof EmergencyVehicle) return "Ambu";
        if (v instanceof Bicycle)          return "Bike";
        if (v instanceof Motorbike)        return "Moto";
        return "Car";
    }
}
