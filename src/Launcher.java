import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;

public class Launcher extends JFrame {
    private JComboBox inputDeviceComboBox;
    private JButton launchButton;
    private JComboBox fpsComboBox;
    private JPanel rootPanel;
    private JComboBox viewTypeComboBox;
    private JRadioButton mirrorRadioButton;

    public void onVisualizerStart(){
        inputDeviceComboBox.setEnabled(false);
        fpsComboBox.setEnabled(false);
        viewTypeComboBox.setEnabled(false);
        mirrorRadioButton.setEnabled(false);
        launchButton.setText("Остановить визуализатор");
    }

    public void onVisualizerStop(){
        inputDeviceComboBox.setEnabled(true);
        fpsComboBox.setEnabled(true);
        viewTypeComboBox.setEnabled(true);
        mirrorRadioButton.setEnabled(true);
        launchButton.setText("Запустить визуализатор");
    }

    public Launcher() {
        setContentPane(rootPanel);
        //setPreferredSize(new Dimension(400, 300));
        pack();
        setResizable(false);

        fpsComboBox.addItem(16);
        fpsComboBox.addItem(32);
        fpsComboBox.addItem(64);

        ComboBoxOption freqsOpt = new ComboBoxOption("По частотам", "frequencies");
        ComboBoxOption notesOpt = new ComboBoxOption("По Нотам", "notes");

        viewTypeComboBox.addItem(freqsOpt);
        viewTypeComboBox.addItem(notesOpt);

        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        for(Mixer.Info mixerInfo : mixerInfos) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            if(mixer.isLineSupported(new DataLine.Info(TargetDataLine.class, Main.FORMAT))) {
                inputDeviceComboBox.addItem(new MixerInfoItem(mixerInfo));
            }
        }

        launchButton.addActionListener(action -> {
            //System.out.println(Main.isVisualizerRunning);
            Mixer.Info mixerInfo = ((MixerInfoItem)inputDeviceComboBox.getSelectedItem()).mixerInfo;
            int frameRate = (Integer)fpsComboBox.getSelectedItem();
            ComboBoxOption option = (ComboBoxOption) viewTypeComboBox.getSelectedItem();
            Main.controlVisualizer(mixerInfo, frameRate, option.getOption(), mirrorRadioButton.isSelected());
        });
    }
}
