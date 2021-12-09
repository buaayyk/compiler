import java.io.*;
import java.util.*;

public class BlockSplit {
    private final ArrayList<String> codes;
    private final ParseMidCode parseMidCode = new ParseMidCode();
    private final HashMap<String, Integer> label2index = new HashMap<>();
    private final HashSet<Integer> blockStart = new HashSet<>();
    private final HashMap<Integer, Block> start2Block = new HashMap<>();
    private final ArrayList<Block> blocks = new ArrayList<>();

    public BlockSplit(ArrayList<String> codes) {
        this.codes = codes;
    }

    public void blockSplit() {
        String code;
        String[] five;
        // 建立标签和位置的映射
        for (int i = 0; i < codes.size(); i++) {
            code = codes.get(i);
            five = parseMidCode.parseMidCode(code);
            if (five[0].equals("11")) {
                label2index.put(five[1], i);
            }
        }
        for (int i = 0; i < codes.size(); i++) {
            code = codes.get(i);
            five = parseMidCode.parseMidCode(code);
            switch (five[0]) {
                case "12":
                    // 比较跳转
                    blockStart.add(label2index.get(five[3]));
                    if (i < codes.size() - 1) {
                        blockStart.add(i + 1);
                    }
                    break;
                case "13":
                    // 直接跳转
                    blockStart.add(label2index.get(five[1]));
                    if (i < codes.size() - 1) {
                        blockStart.add(i + 1);
                    }
                    break;
                case "8":
                    // 函数返回
                    if (i < codes.size() - 1) {
                        blockStart.add(i + 1);
                    }
                    break;
                default:
                    break;
            }
        }
        int number = 0;
        blockStart.add(0);
        blockStart.add(codes.size());
        ArrayList<Integer> starts = new ArrayList<>(blockStart);
        Collections.sort(starts);
        for (int i = 0; i < starts.size() - 1; i++) {
            Block block = new Block(number, new ArrayList<>(codes.subList(starts.get(i), starts.get(i + 1))));
            number += 1;
            start2Block.put(starts.get(i), block);
        }
        for (int i = 0; i < starts.size() - 1; i++) {
            int start = starts.get(i);
            int last = starts.get(i + 1) - 1;
            Block block1 = start2Block.get(start);
            Block block2;
            Block block3;
            code = codes.get(last);
            five = parseMidCode.parseMidCode(code);
            switch (five[0]) {
                case "12":
                    // 比较跳转
                    block2 = start2Block.get(label2index.get(five[3]));
                    block1.addNext(block2);
                    block2.addPre(block1);
                    if (last < codes.size() - 1) {
                        block3 = start2Block.get(last + 1);
                        block1.addNext(block3);
                        block3.addPre(block1);
                    }
                    break;
                case "13":
                    // 直接跳转
                    block2 = start2Block.get(label2index.get(five[1]));
                    block1.addNext(block2);
                    block2.addPre(block1);
                    break;
                case "8":
                    // 函数返回，则没有后继
                    break;
                default:
                    if (last < codes.size() - 1) {
                        block3 = start2Block.get(last + 1);
                        block1.addNext(block3);
                        block3.addPre(block1);
                    }
                    break;
            }
        }
        for (int i = 0; i < starts.size() - 1; i++) {
            Block block = start2Block.get(starts.get(i));
            blocks.add(block);
        }
    }

    public ArrayList<Block> getBlocks() {
        return blocks;
    }
    public static void main(String[] args) throws IOException {
        final boolean grammaticalAnalysisPrint = false;
        final boolean errorPrint = false;
        final boolean midCodePrint = true;
        final boolean finalCodePrint = true;
        ErrorArrayList errorArrayList = new ErrorArrayList();
        ArrayList<String> strings = new ArrayList<>();
        try {
            BufferedReader br =
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

        MidCodeGenerator midCodeGenerator = new MidCodeGenerator(compUnit, true);
        midCodeGenerator.analyse();
        ArrayList<String> midCodes = midCodeGenerator.getMidCodes();
        if (midCodePrint) {
            write(midCodes, "midCode1.txt");
        }
        ReplaceDuplicate replaceDuplicate = new ReplaceDuplicate(midCodes);
        replaceDuplicate.replaceDuplicate();
        midCodes = replaceDuplicate.getMidCodes();
        if (midCodePrint) {
            write(midCodes, "midCode.txt");
        }
        BlockSplit blockSplit = new BlockSplit(midCodes);
        blockSplit.blockSplit();
        FinalCodeGenerator finalCodeGenerator = new FinalCodeGenerator(midCodes);
        finalCodeGenerator.generateFinalCodes();
        ArrayList<String> finalCodes = finalCodeGenerator.getFinalCodes();
        write(finalCodes, "mips.txt");
    }

    public static void write(ArrayList<String> strings, String path) throws IOException {
        BufferedWriter bw2 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path)));
        for (String string : strings) {
            bw2.write(string + "\n");
        }
        bw2.close();
    }

}
