public class ComboBoxOption {
    private final String cover;
    private final String real;

    public ComboBoxOption(String cover, String real){
        this.cover = cover;
        this.real = real;
    }

    public String getOption(){
        return real;
    }

    public String toString(){
        return cover;
    }
}
