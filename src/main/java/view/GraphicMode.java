package view;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import model.vehicle.Vehicle;
import java.util.List;

public class GraphicMode {
    public void render(GraphicsContext gc, List<Vehicle> vehicles) {
        for (Vehicle v : vehicles) {
            gc.save();
            gc.translate(v.getX(), v.getY());
            gc.rotate(v.getAngle());
            
            // Vẽ thân xe cao cấp hơn v3
            gc.setFill(Color.web("#3498db"));
            gc.fillRoundRect(-15, -8, 30, 16, 5, 5);
            
            // Vẽ đèn xe giả lập
            gc.setFill(Color.WHITE);
            gc.fillRect(10, -5, 4, 3);
            gc.fillRect(10, 2, 4, 3);
            
            gc.restore();
        }
    }
}