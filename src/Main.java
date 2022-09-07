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
import java.sql.Time;
import java.util.Arrays;

public class Main extends JComponent{
    public static int currentVolume = 0;
    public static long delayTime = 0;

    public static int frameSize;
    public static float frameRate;
    public static int byteRate;
    public static int channels;
    public static double timeInSec;

    public static int iterator;
    public static Polygon wave;

    public static double[] doubleWave;

    public static final int WIDTH = 1600;
    public static final int HEIGHT = 900;

    public static float[] bytesToFloats (byte[] arr){
        float[] out = new float[arr.length];

        for(int i = 0; i < arr.length; i++) {
            out[i] = Byte.toUnsignedInt(arr[i]);
        }

        return out;
    }

    public static double[] bytesToDoubles (byte[] arr){
        double[] out = new double[arr.length];

        for(int i = 0; i < arr.length; i++) {
            out[i] = Byte.toUnsignedInt(arr[i]);
        }

        return out;
    }

    public static double[] getWave(byte[] buffer, int offset, int channels, int bitRate, int channel){
        int byteRate = bitRate / 8;
        int frameSize = byteRate * channels;
        double[] out = new double[(buffer.length - offset) / frameSize];

        for (int i = 0; i < out.length; i++){
            byte sign = 1;
            try {
                sign = (byte) (buffer[i * frameSize + (channel - 1) * byteRate + offset] / Math.abs(buffer[i * frameSize + (channel - 1) * byteRate + offset]));
            } catch (Throwable ignored) {}
            float volume = 0;
            for(int j = 0; j < bitRate / 8; j++) {
                if(j == 0) {
                    volume = buffer[i * frameSize + (channel - 1) * byteRate + offset];
                } else {
                    volume *= 256;
                    volume += Byte.toUnsignedInt(buffer[i * frameSize + (channel - 1) * byteRate + j + offset]) * sign;
                }
            }
            out[i] = volume;
        }

        return out;
    }

    public static double[] getSinWave(int length, double multx, double multy){
        double[] out = new double[length];

        for(int i = 0; i < length; i++){
            double x = (double)i * multx;
            double y = Math.sin(x) * multy;
            out[i] = y;
        }

        return out;
    }

    public static double[] arraySumm(double[] arr1, double arr2[]){
        double[] out = new double[Math.min(arr1.length, arr2.length)];
        for(int i = 0; i < Math.min(arr1.length, arr2.length); i++){
            out[i] = arr1[i] + arr2[i];
        }
        return out;
    }

    public static AudioFormat getAudioFormat(){
        return new AudioFormat(48000, 16, 1, true, false);
    }

    public Main() throws Throwable {

        System.out.println(Main.class.getResource("").toURI());

        File file = new File(Main.class.getResource("/resources/C Note.wav").getFile().replaceAll("%20", " "));

        AudioInputStream audioIn = AudioSystem.getAudioInputStream(file);
        AudioFormat audioFormat = audioIn.getFormat();

        for(Mixer.Info info : AudioSystem.getMixerInfo()) {
            System.out.println(new String(info.getName().getBytes("Windows-1252"), "Windows-1251"));
        }
        Mixer mixer = AudioSystem.getMixer(AudioSystem.getMixerInfo()[5]);
        TargetDataLine targetDataLine = (TargetDataLine) mixer.getLine(new DataLine.Info(TargetDataLine.class, getAudioFormat()));
        targetDataLine.start();

        System.out.println(audioIn.getFormat());

        InputStream fin = Files.newInputStream(file.toPath());
        byte[] buffer = new byte[(int)file.length()];
        fin.read(buffer, 0, buffer.length);
        fin.close();

        //System.out.println(Arrays.toString(buffer));

        AudioData audioData = new AudioData(buffer);
        AudioDataStream audioStream = new AudioDataStream(audioData);

        frameSize = audioFormat.getFrameSize();
        frameRate = audioFormat.getFrameRate();
        byteRate = audioFormat.getSampleSizeInBits() / 8;
        channels = audioFormat.getChannels();

        //iterator = 143360;
        iterator = 0;
        wave = new Polygon();
        doubleWave = getWave(buffer, 44, channels, byteRate * 8, 1);
        //doubleWave = arraySumm(getSinWave(2048 * 1000, 0.01, 1), getSinWave(2048 * 1000, 0.1, 1));



        Timer repainter = new Timer(200, e ->{
            graphicG2D.clearRect(0, 0, WIDTH, HEIGHT);
            graphicG2D.drawPolygon(wave);
            repaint();
        });
        //repainter.start();


        Timer fullDrawer = new Timer(20, e -> {
            for(int i = 0; i < 64; i++) {
                if (iterator % (WIDTH * (HEIGHT / 256)) == 0) wave = new Polygon();
                int volume = (int)(doubleWave[iterator] * 64);
                wave.addPoint(iterator % WIDTH, volume + ((iterator / WIDTH) * 256) % (HEIGHT - HEIGHT % 256) + 64);
                iterator++;
            }
            graphicG2D.clearRect(0, 0, WIDTH, HEIGHT);
            graphicG2D.drawPolygon(wave);
            repaint();
        });
        //fullDrawer.start();

        FFT fft = new FFT(65536 / 2, 1);
        Timer fourierDrawer = new Timer(0, e -> {
            wave = new Polygon();
            //double[] result = Arrays.copyOfRange(doubleWave, iterator, iterator + 4096 * 2);
            byte[] input = new byte[(int)(65536)];
            targetDataLine.read(input, 0, input.length);
            double[] result = bytesToDoubles(input);
            fft.transform(result);

            for(int i = 0; i < 65536; i++){
                wave.addPoint(i / 128, HEIGHT / 2 - Math.abs((int)(result[i] / (65536))));
            }

            iterator += 512;
            graphicG2D.clearRect(0, 0, WIDTH, HEIGHT);
            graphicG2D.drawPolygon(wave);
            repaint();
        });
        fourierDrawer.start();

        Timer timer = new Timer(0, e -> {
            long time = System.nanoTime() - delayTime;
            timeInSec = (double) time / 1000000000f;
            int frameNum = (int)(timeInSec * frameRate);
            int[] channelsVolume = new int[channels];
            for(int i = 0; i < channels; i++){
                int mult = 1;
                for(int j = 0; j < byteRate; j++){
                    channelsVolume[i] += (char)buffer[frameNum * frameSize + i * byteRate + j] * mult;
                    mult *= 256;
                }
            }
            currentVolume = 0;
            for (int volume : channelsVolume){
                currentVolume += volume;
            }
            currentVolume /= channels;
            graphicG2D.fillRect((int)(timeInSec * 100) % WIDTH, currentVolume/1024 + 75 * ((int)(timeInSec * 100) / WIDTH + 1), 1, 1);
        });

        //AudioPlayer.player.start(audioStream);
        delayTime = System.nanoTime();
        //timer.start();

        setPreferredSize(new Dimension(WIDTH, HEIGHT));

        JFrame frame = new JFrame();
        frame.add(this);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    public static BufferedImage graphic = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
    public static Graphics2D graphicG2D = graphic.createGraphics();

    public void paint(Graphics g){
        Graphics2D g2 = (Graphics2D) g;
        //g2.fillRect(20, 20, currentVolume / 256, 20);
        g2.drawImage(graphic, 0, 0, this);
    }

    public static void main(String[] args) throws Throwable {
        new Main();
    }
}
