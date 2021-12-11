import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegPool {
    private final HashMap<String, String> tRegs = new HashMap<>();
    private final HashMap<String, Boolean> dirty = new HashMap<>();
    private final ArrayList<String> midCodes;
    private final ArrayList<String> finalCodes;
    private SymbolTable symbolTable;
    private int start;
    private int space;
    private final ArrayList<Integer> indexes = new ArrayList<>();
    private final ArrayList<String> sequence = new ArrayList<>();
    private final ParseMidCode parseMidCode;

    public RegPool(ArrayList<String> midCodes, ArrayList<String> finalCodes, SymbolTable symbolTable, ParseMidCode parseMidCode) {
        tRegs.put("$t0", "");
        tRegs.put("$t1", "");
        tRegs.put("$t2", "");
        tRegs.put("$t3", "");
        tRegs.put("$t4", "");
        tRegs.put("$t5", "");
        tRegs.put("$t6", "");
        tRegs.put("$t7", "");
        dirty.put("$t0", false);
        dirty.put("$t1", false);
        dirty.put("$t2", false);
        dirty.put("$t3", false);
        dirty.put("$t4", false);
        dirty.put("$t5", false);
        dirty.put("$t6", false);
        dirty.put("$t7", false);
        this.midCodes = midCodes;
        this.finalCodes = finalCodes;
        this.symbolTable = symbolTable;
        this.parseMidCode = parseMidCode;
    }

    private boolean isDigit(String s) {
        Pattern pattern = Pattern.compile("^[-+]?[\\d]*$");
        Matcher matcher = pattern.matcher(s);
        return matcher.matches();
    }

    public void flush() {
        for (String reg : tRegs.keySet()) {
            tRegs.put(reg, "");
            dirty.put(reg, false);
        }
    }

    public void writeBackAll() {
        for (String reg : tRegs.keySet()) {
            writeBack(reg);
        }
    }

    private void writeBack(String reg) {
        String var = tRegs.get(reg);
        if (!var.equals("") && dirty.get(reg)) {
            IdentSymbol identSymbol = symbolTable.searchIdentInAllLayers(var);
            finalCodes.add("sw " + reg + "," + (space - identSymbol.getAddress()) + "($sp)");
        }
    }

    public void setSpace(int space) {
        this.space = space;
    }

    public String allocReg(String var, int index, boolean write, ArrayList<String> deny) {
        // write表示是否写, deny表示该次分配禁止占用的寄存器对应的变量名
        ArrayList<String> freeRegs = new ArrayList<>();
        for (String reg : tRegs.keySet()) {
            String var1 = tRegs.get(reg);
            if (var.equals(var1)) {
                if (write) {
                    dirty.put(reg, true);
                }
                return reg;
            }
            if (var.equals("")) {
                freeRegs.add(reg);
            }
        }
        if (!freeRegs.isEmpty()) {
            String freerReg = freeRegs.get(0);
            if (write) {
                dirty.put(freerReg, true);
            } else {
                dirty.put(freerReg, false);
            }
            tRegs.put(freerReg, var);
            return freerReg;
        }
        ArrayList<String> occupiedRegs = new ArrayList<>();
        for (String reg : tRegs.keySet()) {
            String var1 = tRegs.get(reg);
            if (!deny.contains(var1)) {
                occupiedRegs.add(reg);
            }
        }
        for (int i = indexes.get(index); i < sequence.size(); i++) {
            // opt算法找出应该被替换的寄存器
            String var1 = sequence.get(i);
            boolean bool = occupiedRegs.removeIf(e -> tRegs.get(e).equals(var1));
            if (bool) {
                if (occupiedRegs.size() == 1) {
                    break;
                }
            }
        }
        String allocReg = occupiedRegs.get(0);
        writeBack(occupiedRegs.get(0));
        if (write) {
            dirty.put(allocReg, true);
        } else {
            dirty.put(allocReg, false);
        }
        return occupiedRegs.get(0);
    }

    public void updateBlock(int start, int end) {
        this.start = start;
        indexes.clear();
        sequence.clear();
        for (int i = start; i < end; i++) {
            indexes.add(sequence.size());
            String midCode = midCodes.get(i);
            String[] five = parseMidCode.parseMidCode(midCode);
            switch (five[0]) {
                case "1":
                    if (!isDigit(five[3])) {
                        sequence.add(five[3]);
                    }
                    if (!isDigit(five[4])) {
                        sequence.add(five[4]);
                    }
                    if (!isDigit(five[2])) {
                        sequence.add(five[2]);
                    }
                    break;
                case "5":
                    // TODO 形参是否算做分配了临时寄存器，这点暂时存疑
                    sequence.add(five[1]);
                    break;
                case "6":
                    if (!isDigit(five[2]) && !five[2].equals("")) {
                        sequence.add(five[2]);
                    }
                    if (!isDigit(five[1])) {
                        sequence.add(five[1]);
                    }
                    break;
                case "12":
                    if (!isDigit(five[1])) {
                        sequence.add(five[1]);
                    }
                    if (!isDigit(five[2])) {
                        sequence.add(five[2]);
                    }
                    break;
                case "15":
                    if (!isDigit(five[2])) {
                        sequence.add(five[2]);
                    }
                    if (!isDigit(five[1])) {
                        sequence.add(five[1]);
                    }
                    if (!isDigit(five[3])) {
                        sequence.add(five[3]);
                    }
                    break;
                case "16":
                    if (!isDigit(five[3])) {
                        sequence.add(five[3]);
                    }
                    if (!isDigit(five[2])) {
                        sequence.add(five[2]);
                    }
                    if (!isDigit(five[1])) {
                        sequence.add(five[1]);
                    }
                    break;
                case "17":
                    sequence.add(five[1]);
                    break;
                case "18":
                    if (!isDigit(five[2]) && !five[2].equals("")) {
                        sequence.add(five[2]);
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
