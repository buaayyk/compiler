import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;

public class Compiler {
    public static void main(String[] args) throws IOException {
        final boolean grammaticalAnalysisPrint = false;
        final boolean errorPrint = false;
        final boolean midCodePrint = true;
        final boolean finalCodePrint = true;
        ErrorArrayList errorArrayList = new ErrorArrayList();
        ArrayList<String> strings = new ArrayList<>();
        try {BufferedReader br =
                new BufferedReader(new InputStreamReader(new FileInputStream("testfile.txt")));

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

        LexicalAnalysis lexicalAnalysis = new LexicalAnalysis(strings, errorArrayList);
        lexicalAnalysis.analyse();
        ArrayList<String[]> results = lexicalAnalysis.getLexicalAnalysisResult();

        ArrayList<TerminalWord> terminalWords = new ArrayList<>();
        for (String[] string1 : results) {
            terminalWords.add(new TerminalWord(string1[0], string1[1], string1[2], string1[3]));
        }
        terminalWords.add(new TerminalWord("end", "#", "-1", "-1"));
        terminalWords.add(new TerminalWord("end", "#", "-1", "-1"));
        terminalWords.add(new TerminalWord("end", "#", "-1", "-1"));

        GrammaticalAnalysis grammaticalAnalysis = new GrammaticalAnalysis(terminalWords, errorArrayList);
        grammaticalAnalysis.analyse();
        NonTerminalWord compUnit = grammaticalAnalysis.getCompUnit();
        if (grammaticalAnalysisPrint) {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("output.txt")));
            compUnit.print(bw);
            bw.close();
        }

        ErrorAnalysis errorAnalysis = new ErrorAnalysis(compUnit, errorArrayList);
        errorAnalysis.analyse();
        ArrayList<String[]> errors = errorArrayList.getErrors();
        if (errorPrint) {
            BufferedWriter bw2 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("error.txt")));
            HashSet<String> hashSet = new HashSet<>();
            for (String[] strings1 : errors) {
                if (!hashSet.contains(strings1[0] + " " + strings1[2] + "\n")) {
                    bw2.write(strings1[0] + " " + strings1[2] + "\n");
                    hashSet.add(strings1[0] + " " + strings1[2] + "\n");
                }
            }
            bw2.close();
        }

        MidCodeGenerator midCodeGenerator = new MidCodeGenerator(compUnit, false);
        midCodeGenerator.analyse();
        ArrayList<String> midCodes = midCodeGenerator.getMidCodes();
        if (midCodePrint) {
            write(midCodes, "midCode.txt");
        }

        FinalCodeGenerator finalCodeGenerator = new FinalCodeGenerator(midCodes);
        finalCodeGenerator.generateFinalCodes();
        ArrayList<String> finalCodes = finalCodeGenerator.getFinalCodes();
        if (finalCodePrint) {
            write(finalCodes, "mips.txt");
        }
    }

    public static void write(ArrayList<String> strings, String path) throws IOException {
        BufferedWriter bw2 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path)));
        for (String string : strings) {
            bw2.write(string + "\n");
        }
        bw2.close();
    }
}
