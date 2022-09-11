import javax.sound.sampled.Mixer;
import java.io.UnsupportedEncodingException;

public class MixerInfoItem {
    public Mixer.Info mixerInfo;

    public MixerInfoItem(Mixer.Info mixerInfo) {
        this.mixerInfo = mixerInfo;
    }

    public String toString(){
        try {
            return new String(mixerInfo.getName().getBytes("Windows-1252"), "Windows-1251");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String asString(Mixer.Info mixerInfo) {
        try {
            return new String(mixerInfo.getName().getBytes("Windows-1252"), "Windows-1251");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }
}
