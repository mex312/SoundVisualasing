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
    private final double maxNote;
    private final int freqStep;
    private final boolean mirror;
    private final String option;

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

    private static double getNote(double freq){
        return 12.0 * (Math.log(freq / 440.0) / Math.log(2));
    }

    private static Color colorFromWaveLength(double len){
        Color out;

        if (len < 380 || len > 781) {
            out = new Color(0, 0, 0);
        } else if (len < 440) {
            out = new Color((int)((440 - len) / 60), 0, 1);
        } else if (len < 490) {
            out = new Color(0, (int)((len - 440) / 50), 1);
        } else if (len < 510) {
            out = new Color(0, 1, (int)((510 - len) / 20));
        } else if (len < 580) {
            out = new Color((int)((len - 510) / 70), 1, 0);
        } else if (len < 645) {
            out = new Color(1, (int)((645 - len) / 65), 0);
        } else {
            out = new Color(1, 0, 0);
        }

        double factor;
        if (len < 380 || len > 781) {
            factor = 0;
        } else if (len < 420) {
            factor = 0.3 + 0.7 * (len - 380) / 40;
        } else if (len < 701) {
            factor = 1.0;
        } else {
            factor = 0.3 + 0.7 * (780 - len) / 80;
        }

        double gamma = 0.8;

        out = new Color((int)(out.getRed() > 0 ? 255 * Math.pow(out.getRed() * factor, gamma) : 0),
                (int)(out.getGreen() > 0 ? 255 * Math.pow(out.getGreen() * factor, gamma) : 0),
                (int)(out.getBlue() > 0 ? 255 * Math.pow(out.getBlue() * factor, gamma) : 0));

        return out;
    }

    public Visualizer(Mixer.Info mixerInfo, int frameRate, String option, boolean mirror) throws Throwable {
        targetDataLine = AudioSystem.getTargetDataLine(Main.FORMAT, mixerInfo);
        //System.out.println(MixerInfoItem.asString(mixerInfo));
        targetDataLine.open(Main.FORMAT);

        fourierSampleSize = (int)Main.FORMAT.getSampleRate() / frameRate;



        //FFT ifft = new FFT(fourieSampleSize, 1);
        input = new byte[fourierSampleSize * (Main.FORMAT.getSampleSizeInBits() / 8) / 2];
        doubleInput = new double[input.length / (Main.FORMAT.getSampleSizeInBits() / 8)];
        inputBuffer = new double[fourierSampleSize];
        wave = new double[inputBuffer.length / 2];

        this.option = option;
        this.mirror = mirror;

        maxNote = getNote(Main.FORMAT.getSampleRate());
        freqStep = (int)Main.FORMAT.getSampleRate() / fourierSampleSize;

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

        if(option.equals("frequencies"))
            drawFreq(g2);
        else if(option.equals("notes"))
            drawNotes(g2);
    }

    private void drawNotes(Graphics2D g2){
        int x = 0;
        for (int i = 0; i < wave.length; i++) {
            double note = getNote(i * 2 * freqStep);
            int nextX = (int)(getNote((i+1) * 2 * freqStep) / maxNote * getWidth());
            //double progress = (double)x / (double)getWidth();

            //Color color = Color.getHSBColor((float)(progress), 1, 1);
            Color color = Color.getHSBColor((float)(note / 12.0), 1, 1);

            g2.setColor(color);
            if(mirror)
                g2.fillRect(x, getHeight() / 2 - (int)(wave[i] * ((double)getHeight() / 1000)), nextX - x, (int)(wave[i] * ((double)getHeight() / 500)));
            else
                g2.fillRect(x, getHeight() - (int)(wave[i] * ((double)getHeight() / 1000)), nextX - x, (int)(wave[i] * ((double)getHeight() / 1000)));

            x = nextX;
        }
    }

     private void drawFreq(Graphics2D g2){
         int x = 0;
         double progressStep = 1.0 / (double)wave.length;
         double progress = 0;
         for(int i = 0; i < wave.length; i++) {
             int nextX = (int)((double)(i + 1) / (double)wave.length * getWidth());

             Color color = Color.getHSBColor((float)(progress * 0.8), 1, 1);

             g2.setColor(color);
             if(mirror)
                 g2.fillRect(x, getHeight() / 2 - (int)(wave[i] * ((double)getHeight() / 1000)), nextX - x, (int)(wave[i] * ((double)getHeight() / 500)));
             else
                 g2.fillRect(x, getHeight() - (int)(wave[i] * ((double)getHeight() / 1000)), nextX - x, (int)(wave[i] * ((double)getHeight() / 1000)));

             x = nextX;
             progress += progressStep;
         }
     }
}
