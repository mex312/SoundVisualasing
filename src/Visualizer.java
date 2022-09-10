import com.sun.media.sound.FFT;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class Visualizer extends JComponent{
    private final double[] wave;
    private final int fourierSampleSize;
    private final Timer fourierDrawer;
    private final TargetDataLine targetDataLine;

    private static double[] bytesToDoubles (byte[] arr, int byteRate){
        double[] out = new double[arr.length];

        for(int i = 0; i < arr.length; i += byteRate) {
            if(byteRate == 2){
                out[i / 2] = (arr[i]) << 8 | arr[i + 1];
            } else {
                out[i] = arr[i];
            }
        }

        return out;
    }

    public Visualizer(Mixer.Info mixerInfo, int frameRate) throws Throwable {
        targetDataLine = AudioSystem.getTargetDataLine(Main.FORMAT, mixerInfo);
        //System.out.println(MixerInfoItem.asString(mixerInfo));
        targetDataLine.open(Main.FORMAT);

        fourierSampleSize = (int)Main.FORMAT.getSampleRate() / frameRate;

        wave = new double[fourierSampleSize];

        FFT fft = new FFT(fourierSampleSize, -1);
        //FFT ifft = new FFT(fourieSampleSize, 1);
        fourierDrawer = new Timer((int)(1000.0 / ((double)Main.FORMAT.getFrameRate() / (double)fourierSampleSize / (double)(Main.FORMAT.getSampleSizeInBits() / 8))) - 2, e -> {
            byte[] input = new byte[fourierSampleSize * Main.FORMAT.getSampleSizeInBits()/8];
            targetDataLine.read(input, 0, input.length);

            double[] result = bytesToDoubles(input, Main.FORMAT.getSampleSizeInBits()/8);

            //System.out.println(Arrays.toString(input));
            fft.transform(result);

            for(int i = 0; i < fourierSampleSize; i++){
                wave[i] = Math.abs(result[i]) / fourierSampleSize;
            }

            //System.out.println(Arrays.toString(wave));

            repaint();
        });

        setPreferredSize(new Dimension(800, 600));
    }

    public void stop(){
        fourierDrawer.stop();
        targetDataLine.close();
    }

    public void start() {
        targetDataLine.start();
        fourierDrawer.start();
    }

    public void paint(Graphics g){
        Graphics2D g2 = (Graphics2D) g;

        g2.clearRect(0, 0, getWidth(), getHeight());

        for(int i = 0; i < fourierSampleSize; i++) {
            double progress = (double)(i) / ((double)fourierSampleSize);
            int x = (int)(progress * getWidth());
            int nextX = (int)((double)(i + 1) / (double)fourierSampleSize * getWidth());

            Color color = Color.getHSBColor((float)(progress), 1, 1);

            g2.setColor(color);
            if(wave[i] >= 0) {
                g2.fillRect(x, getHeight() / 2 - (int) wave[i], nextX - x, (int) wave[i]);
            } else {
                g2.fillRect(x, getHeight() / 2, nextX - x, (int) -wave[i]);
            }
        }
    }
}
