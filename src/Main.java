import com.sun.media.sound.FFT;
import sun.audio.AudioData;
import sun.audio.AudioDataStream;
import sun.audio.AudioPlayer;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Array;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;

public class Main extends JComponent{
    public static double[] wave;

    public static final int WIDTH = 1600;
    public static final int HEIGHT = 900;

    public static double[] bytesToDoubles (byte[] arr, int byteRate){
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
    public static double[] getSinWave(){
        double[] out = new double[fourieSampleSize * 4];

        for(int i = 0; i < out.length / 2; i++) {
            out[i] = Math.sin((double)i / 2) * 16;
        }

        return out;
    }

    public static final AudioFormat FORMAT = new AudioFormat(8192, 16, 1, true, true);
    public static final int fourieSampleSize = 512;

    public Main() throws Throwable {

        for(Mixer.Info info : AudioSystem.getMixerInfo()) {
            System.out.println(new String(info.getName().getBytes("Windows-1252"), "Windows-1251"));
        }
        Mixer mixer = AudioSystem.getMixer(AudioSystem.getMixerInfo()[3]);
        TargetDataLine targetDataLine = AudioSystem.getTargetDataLine(FORMAT, mixer.getMixerInfo());
        targetDataLine.open(FORMAT);
        targetDataLine.start();

        wave = new double[fourieSampleSize];

        FFT fft = new FFT(fourieSampleSize, 1);
        Timer fourierDrawer = new Timer((int)(1000.0 / ((double)FORMAT.getFrameRate() / (double)fourieSampleSize / (double)(FORMAT.getSampleSizeInBits() / 8))) - 2, e -> {
            byte[] input = new byte[fourieSampleSize * FORMAT.getSampleSizeInBits()/8];
            byte[] doubledInput = new byte[fourieSampleSize * 2 * FORMAT.getSampleSizeInBits()/8];
            byte[] quadripledInput = new byte[fourieSampleSize * 4 * FORMAT.getSampleSizeInBits()/8];
            targetDataLine.read(input, 0, input.length);
            System.arraycopy(input, 0, doubledInput, 0, input.length);
            System.arraycopy(input, 0, doubledInput, input.length, input.length);
            System.arraycopy(doubledInput, 0, quadripledInput, 0, doubledInput.length);
            System.arraycopy(doubledInput, 0, quadripledInput, doubledInput.length, doubledInput.length);

            double[] result = bytesToDoubles(quadripledInput, FORMAT.getSampleSizeInBits()/8);
            //double[] result = getSinWave();


            //System.out.println(Arrays.toString(input));
            fft.transform(result);

            for(int i = 0; i < fourieSampleSize; i++){
                wave[i] = Math.abs(result[i] / ((double)fourieSampleSize / 2.0));
                //wave[i] = Math.abs(result[i]);
            }

            System.out.println(Arrays.toString(wave));

            repaint();
        });
        fourierDrawer.start();

        setPreferredSize(new Dimension(WIDTH, HEIGHT));

        JFrame frame = new JFrame();
        frame.add(this);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    public void paint(Graphics g){
        Graphics2D g2 = (Graphics2D) g;

        g2.clearRect(0, 0, WIDTH, HEIGHT);

        for(int i = 0; i < fourieSampleSize; i++) {
            double progress = (double)(i) / ((double)fourieSampleSize);
            int x = (int)(progress * WIDTH);
            int nextX = (int)((double)(i + 1) / (double)fourieSampleSize * WIDTH);

            Color color = Color.getHSBColor((float)(progress), 1, 1);

            g2.setColor(color);
            g2.fillRect(x, HEIGHT - (int)wave[i] - 100, nextX - x, (int)wave[i]);
        }
    }

    public static void main(String[] args) throws Throwable {
        new Main();
    }
}
