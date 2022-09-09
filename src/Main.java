import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Mixer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Main {
    public static final AudioFormat FORMAT = new AudioFormat(8192, 16, 1, true, true);

    public static boolean isVisualizerRunning = false;

    public static Launcher launcher;
    public static Visualizer visualizer;
    private static JFrame frame;

    public static void runVisualizer(Mixer.Info mixerInfo, int frameRate){
        try {
            visualizer = new Visualizer(mixerInfo, frameRate);
            visualizer.setPreferredSize(new Dimension(800, 600));
            visualizer.setSize(new Dimension(800, 600));

            frame = new JFrame();
            frame.add(visualizer);
            frame.setResizable(true);
            frame.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    super.componentResized(e);
                    visualizer.setPreferredSize(frame.getContentPane().getSize());
                    visualizer.setSize(frame.getContentPane().getSize());
                    visualizer.setBounds(0, 0, frame.getContentPane().getWidth(), frame.getContentPane().getHeight());
                    frame.pack();
                }
            });
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    super.windowClosed(e);
                    Main.stopVisualizer();
                }
            });
            frame.pack();
            frame.setVisible(true);

            launcher.onVisualizerStart();
            isVisualizerRunning = true;
        } catch (Throwable ex){
            ex.printStackTrace();
        }
    }

    public static void stopVisualizer(){
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
        launcher = new Launcher();
        launcher.setVisible(true);
        launcher.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
