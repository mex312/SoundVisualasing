import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;

public class Launcher extends JFrame {
    private JComboBox inputDeviceComboBox;
    private JButton launchButton;
    private JComboBox fpsComboBox;
    private JPanel rootPanel;

    public void onVisualizerStart(){
        inputDeviceComboBox.setEnabled(false);
        fpsComboBox.setEnabled(false);
        launchButton.setText("Остановить визуализатор");
    }

    public void onVisualizerStop(){
        inputDeviceComboBox.setEnabled(true);
        fpsComboBox.setEnabled(true);
        launchButton.setText("Запустить визуализатор");
    }

    public Launcher() {
        setContentPane(rootPanel);
        setPreferredSize(new Dimension(400, 300));
        pack();
        setResizable(false);

        fpsComboBox.addItem(16);
        fpsComboBox.addItem(32);
        fpsComboBox.addItem(64);

        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        for(Mixer.Info mixerInfo : mixerInfos) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            if(mixer.isLineSupported(new DataLine.Info(TargetDataLine.class, Main.FORMAT))) {
                inputDeviceComboBox.addItem(new MixerInfoItem(mixerInfo));
            }
        }

        launchButton.addActionListener(action -> {
            //System.out.println(Main.isVisualizerRunning);
            if(!Main.isVisualizerRunning) {
                Mixer.Info mixerInfo = ((MixerInfoItem)inputDeviceComboBox.getSelectedItem()).mixerInfo;
                int frameRate = (Integer)fpsComboBox.getSelectedItem();
                Main.runVisualizer(mixerInfo, frameRate);
            } else {
                Main.stopVisualizer();
            }
        });
    }
}
