import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Mixer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Main {
    public static final AudioFormat FORMAT = new AudioFormat(8192 * 2, 16, 1, true, true);

    public static boolean isVisualizerRunning = false;

    public static Launcher launcher;
    public static Visualizer visualizer;
    private static JFrame frame;

    public static void controlVisualizer(Mixer.Info mixerInfo, int frameRate, String option, boolean mirror) {
        if(!isVisualizerRunning) {
            runVisualizer(mixerInfo, frameRate, option, mirror);
        } else {
            stopVisualizer();
        }
    }

    private static void runVisualizer(Mixer.Info mixerInfo, int frameRate, String option, boolean mirror){
        try {
            visualizer = new Visualizer(mixerInfo, frameRate, option, mirror);

            frame = new JFrame("Визуализатор звука");
            frame.add(visualizer);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    Main.stopVisualizer();
                }
            });
            frame.pack();
            frame.setVisible(true);

            visualizer.start();

            launcher.onVisualizerStart();
            isVisualizerRunning = true;
        } catch (Throwable ex){
            ex.printStackTrace();
        }
    }

    private static void stopVisualizer(){
        if(visualizer != null) {
            visualizer.stop();
            visualizer = null;
        }
        if(frame != null) {
            frame.dispose();
            frame.removeAll();
            frame = null;
        }

        launcher.onVisualizerStop();
        isVisualizerRunning = false;
    }

    public static void main(String[] args) {
        launcher = new Launcher("Лаунчер визуализатора звука");
        launcher.setVisible(true);
        launcher.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
