import java.io.BufferedWriter;
import java.io.IOException;

public class TerminalWord implements Word {
    private final String wordType;
    private final String wordName;
    private final String col;
    private final String row;


    public TerminalWord(String wordType, String wordName, String col, String row) {
        this.wordType = wordType;
        this.wordName = wordName;
        this.col = col;
        this.row = row;
    }

    public String getWordType() {
        return wordType;
    }

    public String getWordName() {
        return wordName;
    }

    public String getCol() {
        return col;
    }

    public String getRow() {
        return row;
    }

    public void print(BufferedWriter bw) {
        try {
            bw.write(wordType + " " + wordName + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
