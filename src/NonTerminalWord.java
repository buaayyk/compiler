import java.util.ArrayList;

public class NonTerminalWord extends Word {
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
}
