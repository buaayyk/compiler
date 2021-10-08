import java.io.BufferedWriter;
import java.io.IOException;

public class TerminalWord implements Word {
    private final String wordType;
    private final String wordName;


    public TerminalWord(String wordType, String wordName) {
        this.wordType = wordType;
        this.wordName = wordName;
    }

    public String getWordType() {
        return wordType;
    }

    public String getWordName() {
        return wordName;
    }

    public void print(BufferedWriter bw) {
        try {
            bw.write(wordType + " " + wordName + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
