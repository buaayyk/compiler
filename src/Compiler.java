import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class Compiler {
    public static void main(String[] args) throws IOException {
        ArrayList<String> strings = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("testfile.txt")));
            while (true) {
                String string = br.readLine();
                if (string == null) {
                    break;
                }
                strings.add(string);
            }
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
//        if (Math.random() < 0.5) {
//            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("output.txt")));
//            for (String string : strings) {
//                bw.write(string);
//            }
//            bw.close();
//        } else {
//            String a = null;
//            System.out.println(a.length());
//        }
        LexicalAnalysis lexicalAnalysis = new LexicalAnalysis(strings);
        lexicalAnalysis.analyse();
        ArrayList<String[]> results = lexicalAnalysis.getLexicalAnalysisResult();

        ArrayList<TerminalWord> terminalWords = new ArrayList<>();
        for (String[] string1 : results) {
            terminalWords.add(new TerminalWord(string1[0], string1[1]));
        }
        terminalWords.add(new TerminalWord("end","#"));
        terminalWords.add(new TerminalWord("end","#"));
        terminalWords.add(new TerminalWord("end","#"));
        GrammaticalAnalysis grammaticalAnalysis = new GrammaticalAnalysis(terminalWords);
        grammaticalAnalysis.analyse();
        NonTerminalWord compUnit = grammaticalAnalysis.getCompUnit();
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("output.txt")));
        compUnit.print(bw);
        bw.close();
    }
}
