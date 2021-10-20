import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

public class LexicalAnalysis {
    private ArrayList<String> text; // 要进行词法分析的文本
    private ArrayList<String> processedText = new ArrayList<>(); //经过删除注释的预处理的文本
    private int row; //当前正在分析的字符的行：从1开始
    private int col; //当前正在分析的字符的列，从1开始
    private ArrayList<String[]> lexicalAnalysisResult = new ArrayList<>(); //词法分析的结果
    private HashMap<String, String> reversedWord2type = new HashMap<String, String>() {
        {
            put("main", "MAINTK");
            put("const", "CONSTTK");
            put("int", "INTTK");
            put("break", "BREAKTK");
            put("continue", "CONTINUETK");
            put("if", "IFTK");
            put("else", "ELSETK");
            put("!", "NOT");
            put("&&", "AND");
            put("||", "OR");
            put("while", "WHILETK");
            put("getint", "GETINTTK");
            put("printf", "PRINTFTK");
            put("return", "RETURNTK");
            put("+", "PLUS");
            put("-", "MINU");
            put("void", "VOIDTK");
            put("*", "MULT");
            put("/", "DIV");
            put("%", "MOD");
            put("<", "LSS");
            put("<=", "LEQ");
            put(">", "GRE");
            put(">=", "GEQ");
            put("==", "EQL");
            put("!=", "NEQ");
            put("=", "ASSIGN");
            put(";", "SEMICN");
            put(",", "COMMA");
            put("(", "LPARENT");
            put(")", "RPARENT");
            put("[", "LBRACK");
            put("]", "RBRACK");
            put("{", "LBRACE");
            put("}", "RBRACE");
        }
    };
    private ArrayList<String> sortedKey;
    private ErrorArrayList errorArrayList;
    private boolean existError;

    public LexicalAnalysis(ArrayList<String> text, ErrorArrayList errorArrayList) {
        this.text = new ArrayList<>();
        this.text.addAll(text);
        this.row = 1;
        this.col = 1;
        this.sortedKey = new ArrayList<String>(this.reversedWord2type.keySet());
        this.sortedKey.sort(new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                return Integer.compare(s2.length(), s1.length());
            }
        });
        this.existError = false;
        this.errorArrayList = errorArrayList;
    }

    private void preProcess() {
        boolean inComment = false; //当前在/ *注释里面,并且一定有结尾
        boolean inPrint = false; //当前在printf的格式化字符串里面
        StringBuilder stringBuilder = new StringBuilder();
        char c;
        for (String string : text) {
            stringBuilder.append(string);
            stringBuilder.append("\n");
        }
        for (int index = 0; index < stringBuilder.length(); index++) {
            if (stringBuilder.charAt(index) == '"') {
                inPrint = !inPrint;
            }
            if (!inPrint) {
                if (stringBuilder.charAt(index) == '/' && index + 1 < stringBuilder.length()
                        && stringBuilder.charAt(index + 1) == '*') {
                    int start = index;
                    boolean bool = false;
                    index += 3;
                    while (index < stringBuilder.length()) {
                        if (index - 1 >= 0 && stringBuilder.charAt(index - 1) == '*' && stringBuilder.charAt(index) == '/') {
                            bool = true;
                            break;
                        }
                        index += 1;
                    }
                    if (bool) {
                        for (int i = start; i <= index; i++) {
                            if (stringBuilder.charAt(i) != '\n') {
                                stringBuilder.replace(i, i + 1, " ");
                            }
                        }
                        continue;
                    } else {
                        index = start;
                    }
                }
                if (stringBuilder.charAt(index) == '/' && index + 1 < stringBuilder.length()
                        && stringBuilder.charAt(index + 1) == '/') {
                    int start = index;
                    while (index < stringBuilder.length()) {
                        if (stringBuilder.charAt(index) == '\n') {
                            break;
                        }
                        index += 1;
                    }
                    for (int i = start; i < index; i++) {
                        stringBuilder.replace(i, i + 1, " ");
                    }
                    stringBuilder.replace(index, index + 1, "\n");
                    continue;
                }
            }
        }
        processedText = new ArrayList<String>(Arrays.asList(new String(stringBuilder).split("\n")));
    }

    public void analyse() {
        preProcess();
        for (String string : processedText) {
            boolean bool;
            row = 1;
            while (true) {
                // removeBlank先将单词之间的空白符去除
                bool = removeBlank(string);
                // 此处表示一行已经被读完
                if (bool) {
                    break;
                }
                bool = isIdentifier(string);
                if (bool) {
                    continue;
                }
                bool = isIntConst(string);
                if (bool) {
                    continue;
                }
                bool = isFormatString(string);
                if (bool) {
                    continue;
                }
                bool = isReservedWord(string);
                if (bool) {
                    continue;
                }
                //TODO 这里补充错误处理
                break;
            }
            col += 1;
        }
    }

    public ArrayList<String[]> getLexicalAnalysisResult() {
        return new ArrayList<>(lexicalAnalysisResult);
    }

    private boolean removeBlank(String string) {
        //去除字符串头部的空白符
        while (row <= string.length() && string.substring(row - 1, row).matches("\\s")) {
            row += 1;
        }
        //如果当去除空白符之后已经没有字符了，则返回
        return row > string.length();
    }

    private boolean isIdentifier(String string) {
        boolean isIdentifier;
        int start = row;
        char c;
        c = string.charAt(row - 1);
        if (Character.isLetter(c) || c == '_') {
            row += 1;
            while (row <= string.length()) {
                c = string.charAt(row - 1);
                if (!Character.isLetter(c) && c != '_' && !Character.isDigit(c)) {
                    break;
                }
                row += 1;
            }
            String identifier = string.substring(start - 1, row - 1);
            if (reversedWord2type.containsKey(identifier)) {
                row = start;
                isIdentifier = false;
            } else {
                String[] ss = new String[4];
                ss[0] = "IDENFR";
                ss[1] = identifier;
                ss[2] = "" + col;
                ss[3] = "" + row;
                lexicalAnalysisResult.add(ss);
                isIdentifier = true;
            }
        } else {
            isIdentifier = false;
        }
        return isIdentifier;
    }

    private boolean isIntConst(String string) {
        boolean isIntConst;
        int start = row;
        char c;
        c = string.charAt(row - 1);
        if (c == '0') {
            row += 1;
            String[] ss = new String[4];
            ss[0] = "INTCON";
            ss[1] = "0";
            ss[2] = "" + col;
            ss[3] = "" + row;
            lexicalAnalysisResult.add(ss);
            isIntConst = true;
        } else if (Character.isDigit(c)) {
            row += 1;
            while (row <= string.length()) {
                c = string.charAt(row - 1);
                if (!Character.isDigit(c)) {
                    break;
                }
                row += 1;
            }
            String intConst = string.substring(start - 1, row - 1);
            String[] ss = new String[4];
            ss[0] = "INTCON";
            ss[1] = intConst;
            ss[2] = "" + col;
            ss[3] = "" + row;
            lexicalAnalysisResult.add(ss);
            isIntConst = true;
        } else {
            isIntConst = false;
        }
        return isIntConst;
    }

    private boolean isFormatString(String string) {
        boolean isFormatString = false;
        int start = row;
        char c;
        c = string.charAt(row - 1);
        boolean error = false;
        if (c == '"') {
            isFormatString = true;
            row += 1;
            while (row <= string.length() && string.charAt(row - 1) != '"') {
                c = string.charAt(row - 1);
                if (c == '%') {
                    if (string.charAt(row) == 'd') {
                        row += 2;
                    } else {
                        row += 1;
                        error = true;
                    }
                } else if (((int) c >= 40 && (int) c <= 126 || (int) c == 32 || (int) c == 33) && c != '\\') {
                    row += 1;
                } else if (c == '\\') {
                    if (string.charAt(row) != 'n') {
                        error = true;
                    }
                    row += 1;
                } else {
                    error = true;
                    row += 1;
                }
            }
            row += 1;
            String[] ss = new String[4];
            ss[0] = "STRCON";
            ss[1] = string.substring(start - 1, row - 1);
            ss[2] = "" + col;
            ss[3] = "" + row;
            lexicalAnalysisResult.add(ss);
            if (error) {
                existError = true;
                errorArrayList.add("" + col, "" + row, "a");
            }
        }
        return isFormatString;
    }

    private boolean isReservedWord(String string) {
        String substring = string.substring(row - 1);
        for (String reversedWord : sortedKey) {
            if (substring.startsWith(reversedWord)) {
                row += reversedWord.length();
                String[] ss = new String[4];
                ss[0] = reversedWord2type.get(reversedWord);
                ss[1] = reversedWord;
                ss[2] = "" + col;
                ss[3] = "" + row;
                lexicalAnalysisResult.add(ss);
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {

    }
}
