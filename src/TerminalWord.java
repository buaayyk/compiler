public class TerminalWord extends Word {
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


}
