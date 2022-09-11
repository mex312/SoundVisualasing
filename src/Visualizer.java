import com.sun.media.sound.FFT;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class Visualizer extends JComponent{
    private final double[] wave;
    private final byte[] input;
    private final double[] doubleInput;
    private final double[] inputBuffer;
    private final int fourierSampleSize;
    private final Timer fourierDrawer;
    private final TargetDataLine targetDataLine;

    private static void bytesToDoubles (double[] out, byte[] arr, int byteRate){
        if (out.length != arr.length / byteRate) throw new IllegalArgumentException("Length of out should be length of arr / byteRate");

        for(int i = 0; i < arr.length; i += byteRate) {
            if(byteRate == 2){
                out[i / 2] = (arr[i]) << 8 | arr[i + 1];
            } else {
                out[i] = arr[i];
            }
        }
    }

    private static double complexAbs(double real, double imaginary) {
        return Math.sqrt(real*real + imaginary*imaginary);
    }

    private static double[] complexArrayAbs(double[] complex) {
        double[] out = new double[complex.length / 2];

        for(int i = 0; i < out.length; i++) {
            out[i] = complexAbs(complex[i * 2], complex[i * 2 + 1]);
        }

        return out;
    }

    private static void putAtStart(double[] out, double[] arr){
        int diff = out.length - arr.length;
        System.arraycopy(out, 0, out, arr.length, diff);
        System.arraycopy(arr, 0, out, 0, arr.length);
    }

    private static void arrayMultiply(double[] out, double mult){
        for(int i = 0; i < out.length; i++) {
            out[i] *= mult;
        }
    }

    public Visualizer(Mixer.Info mixerInfo, int frameRate) throws Throwable {
        targetDataLine = AudioSystem.getTargetDataLine(Main.FORMAT, mixerInfo);
        //System.out.println(MixerInfoItem.asString(mixerInfo));
        targetDataLine.open(Main.FORMAT);

        fourierSampleSize = (int)Main.FORMAT.getSampleRate() / frameRate;



        //FFT ifft = new FFT(fourieSampleSize, 1);
        input = new byte[fourierSampleSize * (Main.FORMAT.getSampleSizeInBits() / 8) / 2];
        doubleInput = new double[input.length / (Main.FORMAT.getSampleSizeInBits() / 8)];
        inputBuffer = new double[fourierSampleSize];
        wave = new double[inputBuffer.length / 2];
        FFT fft = new FFT(inputBuffer.length, -1);
        fourierDrawer = new Timer((int)(1000.0 / ((double)Main.FORMAT.getFrameRate() / (double)input.length)), e -> {
            targetDataLine.read(input, 0, input.length);
            bytesToDoubles(doubleInput, input, Main.FORMAT.getSampleSizeInBits()/8);
            //arrayMultiply(inputBuffer, 0.98);
            putAtStart(inputBuffer, doubleInput);

            double[] complexResult = new double[inputBuffer.length * 2];
            System.arraycopy(inputBuffer, 0, complexResult, 0, inputBuffer.length);
            fft.transform(complexResult);
            double[] result = complexArrayAbs(complexResult);

            for(int i = 0; i < result.length / 2; i++){
                wave[i] = (result[i] + result[result.length - i - 1]) / fourierSampleSize / 2;
            }

            System.out.println(Arrays.toString(wave));

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

        for(int i = 0; i < wave.length; i++) {
            double progress = (double)(i) / ((double)wave.length);
            int x = (int)(progress * getWidth());
            int nextX = (int)((double)(i + 1) / (double)wave.length * getWidth());

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
