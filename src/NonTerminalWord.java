import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public class NonTerminalWord implements Word {
    private final String type;
    private final ArrayList<Word> components = new ArrayList<>();

    public NonTerminalWord(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void add(Word word) {
        components.add(word);
    }

    public void remove() {
        components.remove(components.size() - 1);
    }

    public ArrayList<Word> getComponents() {
        return new ArrayList<>(components);
    }

    public String toString() {
        StringBuilder string = new StringBuilder();
        if (type.equals("<MulExp>")) {
            for (Word word : components) {
                if (word instanceof NonTerminalWord) {
                    string.append(((NonTerminalWord) word).toString());
                    string = new StringBuilder("parseInt(" + string + ")");
                } else {
                    string = new StringBuilder(string + ((TerminalWord) word).getWordName() + " ");
                }
            }
        } else {
            for (Word word : components) {
                if (word instanceof TerminalWord) {
                    string.append(((TerminalWord) word).getWordName());
                    string.append(" ");
                } else {
                    string.append(((NonTerminalWord) word).toString());
                }
            }
        }
        return string.toString().toString();
    }

    public void print(BufferedWriter bw) {
        for (int i = 0; i < components.size(); i++) {
            components.get(i).print(bw);
            if ((type.equals("<MulExp>") || type.equals("<AddExp>") || type.equals("<RelExp>")
                    || type.equals("<EqExp>") || type.equals("<LAndExp>") || type.equals("<LOrExp>"))
                    && (i % 2 == 0 && i < components.size() - 1)) {
                try {
                    bw.write(type + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (!type.equals("<BlockItem>") && !type.equals("<Decl>") && !type.equals("<Btype>")) {
            try {
                bw.write(type + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
