package util;

import javafx.scene.media.AudioClip;

public class SoundManager {
    private static AudioClip ambulance, firetruck, signal, horn;
    public static boolean isMuted = false;

    // Load file âm thanh bằng phương thức getResource chuẩn của Java
    static {
        try {
            ambulance = new AudioClip(SoundManager.class.getResource("/audio/ambulance.mp3").toExternalForm());
            firetruck = new AudioClip(SoundManager.class.getResource("/audio/firetruck.mp3").toExternalForm());
            signal = new AudioClip(SoundManager.class.getResource("/audio/signal.mp3").toExternalForm());
            horn = new AudioClip(SoundManager.class.getResource("/audio/horn.mp3").toExternalForm());
        } catch (Exception e) {
            System.out.println("⚠️ Lỗi tải âm thanh: " + e.getMessage());
        }
    }

    public static void playAmbulance() {
        if (isMuted || ambulance == null) return;
        if (!ambulance.isPlaying()) ambulance.play(0.3); 
    }

    public static void playFiretruck() {
        if (isMuted || firetruck == null) return;
        if (!firetruck.isPlaying()) firetruck.play(0.3);
    }

    public static void playSignal() {
        if (isMuted || signal == null) return;
        if (!signal.isPlaying()) signal.play(0.5);
    }

    public static void playHorn() {
        if (isMuted || horn == null) return;
        if (!horn.isPlaying()) horn.play(0.4);
    }
}