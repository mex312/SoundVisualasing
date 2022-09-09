import com.sun.media.sound.FFT;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class Visualizer extends JComponent{
    private double[] wave;
    private final int fourieSampleSize;
    private final Timer fourierDrawer;
    private final TargetDataLine targetDataLine;

    private static double[] bytesToDoubles (byte[] arr, int byteRate){
        double[] out = new double[arr.length];

        for(int i = 0; i < arr.length; i += byteRate) {
            if(byteRate == 2){
                out[i] = (arr[i]) << 8 | arr[i + 1];
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
        targetDataLine.start();

        fourieSampleSize = (int)Main.FORMAT.getSampleRate() / frameRate;

        wave = new double[fourieSampleSize];

        FFT fft = new FFT(fourieSampleSize, -1);
        //FFT ifft = new FFT(fourieSampleSize, 1);
        fourierDrawer = new Timer((int)(1000.0 / ((double)Main.FORMAT.getFrameRate() / (double)fourieSampleSize / (double)(Main.FORMAT.getSampleSizeInBits() / 8))) - 2, e -> {
            byte[] input = new byte[fourieSampleSize * Main.FORMAT.getSampleSizeInBits()/8];
            targetDataLine.read(input, 0, input.length);
            double[] floatInput = bytesToDoubles(input, Main.FORMAT.getSampleSizeInBits()/8);
            double[] result = new double[floatInput.length * 2];
            System.arraycopy(floatInput, 0, result, 0, floatInput.length);
            //double[] result = getSinWave();

            //System.out.println(Arrays.toString(input));
            fft.transform(result);
            //ifft.transform(result);

            for(int i = 0; i < fourieSampleSize; i++){
                wave[i] = result[i] / fourieSampleSize;
                //wave[i] = Math.abs(result[i]);
            }

            //System.out.println(Arrays.toString(wave));

            repaint();
        });
        fourierDrawer.start();
    }

    public void stop(){
        fourierDrawer.stop();
        targetDataLine.close();
    }

    public void paint(Graphics g){
        Graphics2D g2 = (Graphics2D) g;

        g2.clearRect(0, 0, getWidth(), getHeight());

        for(int i = 0; i < fourieSampleSize; i++) {
            double progress = (double)(i) / ((double)fourieSampleSize);
            int x = (int)(progress * getWidth());
            int nextX = (int)((double)(i + 1) / (double)fourieSampleSize * WIDTH);

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
