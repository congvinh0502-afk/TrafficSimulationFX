package view;

import engine.SimulationEngine;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class MainView {
    private BorderPane root;
    private SimulationEngine engine;
    private double lastMouseX, lastMouseY;

    public MainView() {
        root = new BorderPane();
        engine = new SimulationEngine(new javafx.scene.canvas.Canvas(config.Constants.WINDOW_WIDTH - 300, config.Constants.WINDOW_HEIGHT));
        
        // Cắm Canvas vào giữa
        root.setCenter(engine.getCanvas());
        
        // Tạo Bảng điều khiển siêu cấp bên phải
        root.setRight(createControlPanel());
        
        setupCameraControls();
    }

    public BorderPane getRoot() { return root; }
    public void startSimulation() { engine.start(); }

    // --- XÂY DỰNG GIAO DIỆN KIỂU V3 ---
    private ScrollPane createControlPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(15));
        panel.setStyle("-fx-background-color: #1e272e;"); // Nền Dark Mode
        panel.setPrefWidth(320);

        // Khối 1: CẢNH MÔ PHỎNG
        VBox secMap = createSection("🗺 CẢNH MÔ PHỎNG");
        ToggleGroup mapGroup = new ToggleGroup();
        RadioButton rbNga3 = createRadio("Ngã Ba", mapGroup);
        RadioButton rbNga4 = createRadio("Ngã Tư", mapGroup);
        RadioButton rbNga5 = createRadio("Ngã Năm", mapGroup);
        RadioButton rbGrid = createRadio("Mạng lưới", mapGroup);
        rbGrid.setSelected(true);
        mapGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) engine.changeMap(((RadioButton)newV).getText());
        });
        HBox mapRow1 = new HBox(20, rbNga3, rbNga4);
        HBox mapRow2 = new HBox(20, rbNga5, rbGrid);
        secMap.getChildren().addAll(mapRow1, mapRow2);

        // Khối 2: LƯU LƯỢNG & ĐIỀU KHIỂN CHUNG
        VBox secFlow = createSection("🚦 LƯU LƯỢNG & ĐIỀU KHIỂN");
        Button btnPause = new Button("⏸ Tạm dừng / Tiếp tục");
        btnPause.setMaxWidth(Double.MAX_VALUE);
        btnPause.setStyle("-fx-background-color: #ecf0f1; -fx-text-fill: #2c3e50; -fx-font-weight: bold;");
        btnPause.setOnAction(e -> engine.togglePause());

        Label lblSpawn = new Label("Mật độ xe (Ít -> Nhiều):");
        lblSpawn.setTextFill(Color.web("#bdc3c7"));
        Slider spawnSlider = new Slider(2, 60, 30);
        spawnSlider.valueProperty().addListener((obs, oldVal, newVal) -> engine.getSpawnSystem().setSpawnDelay(62 - newVal.intValue()));
        
        Button btnClear = new Button("🗑 Xóa tất cả xe");
        btnClear.setMaxWidth(Double.MAX_VALUE);
        btnClear.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        btnClear.setOnAction(e -> engine.clearAllVehicles()); // Cần thêm hàm này vào Engine
        btnClear.setOnAction(e -> {
            util.SoundManager.playHorn(); // ---> GỌI TIẾNG CÒI NGAY LÚC CLICK BẰNG TAY
            engine.clearAllVehicles();
        });
        secFlow.getChildren().addAll(btnPause, lblSpawn, spawnSlider, btnClear);

        // Khối 3: CAMERA ZOOM
        VBox secZoom = createSection("🔍 ZOOM");
        HBox zoomControls = new HBox(10);
        Button btnZoomOut = new Button("Q -");
        Button btnZoomIn = new Button("Q +");
        Button btnResetZoom = new Button("Reset");
        btnZoomOut.setOnAction(e -> engine.zoomCamera(0.8));
        btnZoomIn.setOnAction(e -> engine.zoomCamera(1.2));
        btnResetZoom.setOnAction(e -> engine.resetCamera()); // Cần thêm hàm này vào Engine
        zoomControls.getChildren().addAll(btnZoomOut, btnZoomIn, btnResetZoom);
        secZoom.getChildren().add(zoomControls);
        
        // ---> MỚI: Khối 4: ĐIỀU CHỈNH TỐC ĐỘ (SPEED)
        VBox secSpeed = createSection("⚡ TỐC ĐỘ CÁC LOẠI XE");
        secSpeed.getChildren().addAll(
            createSpeedSlider("Ô tô", val -> config.Constants.SPEED_CAR = val),
            createSpeedSlider("Xe máy", val -> config.Constants.SPEED_MOTORBIKE = val),
            createSpeedSlider("Xe đạp", val -> config.Constants.SPEED_BICYCLE = val),
            createSpeedSlider("Cứu thương", val -> config.Constants.SPEED_AMBULANCE = val),
            createSpeedSlider("Cứu hỏa", val -> config.Constants.SPEED_FIRETRUCK = val)
        );

        // Khối 5: CÀI ĐẶT HỆ THỐNG
        VBox secSettings = createSection("⚙ CÀI ĐẶT HỆ THỐNG");
        CheckBox chkAutoLight = createCheck("Đèn Tự động", true);
        chkAutoLight.setOnAction(e -> config.Constants.AUTO_LIGHTS = chkAutoLight.isSelected());
        CheckBox chkRain = createCheck("Trời mưa (Đường trơn)", false);
        chkRain.setOnAction(e -> config.Constants.IS_RAINING = chkRain.isSelected());
        CheckBox chkMute = createCheck("Tắt Âm thanh", false);
        chkMute.setOnAction(e -> util.SoundManager.isMuted = chkMute.isSelected());
        CheckBox chkDebug = createCheck("Bật tia Laser (Debug)", false);
        chkDebug.setOnAction(e -> engine.setDebugMode(chkDebug.isSelected()));
        secSettings.getChildren().addAll(chkAutoLight, chkRain, chkMute, chkDebug);

        // Khối 6: HIỂN THỊ
        VBox secDisplay = createSection("📺 HIỂN THỊ");
        ToggleGroup dispGroup = new ToggleGroup();
        RadioButton rbBasic = createRadio("Basic", dispGroup);
        RadioButton rbGraphic = createRadio("Đồ họa", dispGroup);
        rbGraphic.setSelected(true);
        dispGroup.selectedToggleProperty().addListener((obs, old, newVal) -> {
            view.VehicleRenderer.BASIC_MODE = ((RadioButton)newVal).getText().equals("Basic");
        });
        HBox dispRow = new HBox(20, rbBasic, rbGraphic);
        secDisplay.getChildren().add(dispRow);
        // ---> MỚI: Khối 7: THỜI GIAN
        VBox secTime = createSection("🌗 THỜI GIAN");
        ComboBox<String> cbTime = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(
            "Chu kỳ Tự động", "Luôn Ban Ngày", "Luôn Ban Đêm"
        ));
        cbTime.setValue("Chu kỳ Tự động");
        cbTime.setMaxWidth(Double.MAX_VALUE);
        cbTime.setStyle("-fx-font-size: 13px; -fx-cursor: hand;");
        cbTime.setOnAction(e -> config.Constants.TIME_MODE = cbTime.getSelectionModel().getSelectedIndex());
        secTime.getChildren().add(cbTime);

        // ---> MỚI: Khối 8: LOẠI ĐÈN
        VBox secLightMode = createSection("🚥 LOẠI ĐÈN");
        ComboBox<String> cbLight = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(
            "Không đếm số", "Đếm toàn thời gian", "Đếm khi <= 10s"
        ));
        cbLight.setValue("Đếm khi <= 10s");
        cbLight.setMaxWidth(Double.MAX_VALUE);
        cbLight.setStyle("-fx-font-size: 13px; -fx-cursor: hand;");
        cbLight.setOnAction(e -> engine.setTrafficLightMode(cbLight.getSelectionModel().getSelectedIndex()));
        secLightMode.getChildren().add(cbLight);
        
        // Đã bổ sung secTime và secLightMode vào danh sách
        panel.getChildren().addAll(secMap, secFlow, secZoom, secSpeed, secSettings, secDisplay, secTime, secLightMode);
        ScrollPane scroll = new ScrollPane(panel);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #1e272e; -fx-border-color: #1e272e;");
        return scroll;
    }

    // --- CÁC HÀM TIỆN ÍCH TẠO GIAO DIỆN ---
    private VBox createSection(String titleStr) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-border-color: #34495e; -fx-border-width: 1; -fx-border-radius: 5;");
        Label title = new Label(titleStr);
        title.setFont(Font.font("System", FontWeight.BOLD, 14));
        title.setTextFill(Color.WHITE);
        box.getChildren().add(title);
        return box;
    }

    private RadioButton createRadio(String text, ToggleGroup group) {
        RadioButton rb = new RadioButton(text);
        rb.setToggleGroup(group);
        rb.setTextFill(Color.web("#bdc3c7"));
        return rb;
    }

    private CheckBox createCheck(String text, boolean selected) {
        CheckBox chk = new CheckBox(text);
        chk.setSelected(selected);
        chk.setTextFill(Color.web("#bdc3c7"));
        return chk;
    }

    private void setupCameraControls() {
        javafx.scene.canvas.Canvas canvas = engine.getCanvas();
        canvas.setCursor(Cursor.HAND);
        canvas.setOnScroll(event -> {
            if (event.getDeltaY() > 0) engine.zoomCamera(1.1);
            else engine.zoomCamera(0.9);
        });
        canvas.setOnMousePressed(event -> {
            lastMouseX = event.getX(); lastMouseY = event.getY();
            canvas.setCursor(Cursor.CLOSED_HAND);
        });
        canvas.setOnMouseDragged(event -> {
            engine.unlockCamera();
            engine.moveCamera((event.getX() - lastMouseX) / engine.getZoomScale(), (event.getY() - lastMouseY) / engine.getZoomScale());
            lastMouseX = event.getX(); lastMouseY = event.getY();
        });
        canvas.setOnMouseReleased(event -> canvas.setCursor(Cursor.HAND));
        canvas.setOnMouseClicked(event -> {
            boolean isRightClick = event.getButton() == javafx.scene.input.MouseButton.SECONDARY;
            engine.handleMouseClick(event.getX(), event.getY(), isRightClick);
        });
    }
    // Ném hàm này xuống cuối cùng của file MainView.java
    private HBox createSpeedSlider(String name, java.util.function.Consumer<Double> action) {
        Label lbl = new Label(name + ":");
        lbl.setTextFill(Color.web("#bdc3c7"));
        lbl.setPrefWidth(90);
        
        Slider slider = new Slider(0.1, 3.0, 1.0); // Cho phép chỉnh từ x0.1 (rùa bò) đến x3.0 (bàn thờ)
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(1.0);
        
        Label valLbl = new Label("x1.0");
        valLbl.setTextFill(Color.WHITE);
        valLbl.setPrefWidth(40);
        
        slider.valueProperty().addListener((obs, oldV, newV) -> {
            // Làm tròn số đến 1 chữ số thập phân
            double rounded = Math.round(newV.doubleValue() * 10.0) / 10.0;
            valLbl.setText("x" + rounded);
            action.accept(rounded); // Gửi giá trị mới cập nhật vào hệ thống
        });
        
        HBox box = new HBox(10, lbl, slider, valLbl);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }
}