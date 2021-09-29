import java.io.*;
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
        LexicalAnalysis lexicalAnalysis = new LexicalAnalysis(strings);
        lexicalAnalysis.analyse();
        ArrayList<String[]> results = lexicalAnalysis.getLexicalAnalysisResult();
        for (String[] strings1 : results) {
            System.out.println(strings1[0] + " " + strings1[1]);
        }
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("output.txt")));
        for (String[] strings1 : results) {
            bw.write(strings1[0] + " " + strings1[1] + "\n");
        }
        bw.close();
    }
}
