import java.util.ArrayList;
import java.util.HashMap;

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

    public void flush() {
        for (String reg : tRegs.keySet()) {
            tRegs.put(reg, "");
            dirty.put(reg, false);
        }
    }

    public void writeBackAll() {
        for (String reg : tRegs.keySet()) {
            String var = tRegs.get(reg);
            if (!var.equals("") && dirty.get(reg)) {
                IdentSymbol identSymbol = symbolTable.searchIdentInAllLayers(var);
                finalCodes.add("sw " + reg + "," + (space - identSymbol.getAddress()) + "($sp)");
            }
        }
    }

    public void setSpace(int space) {
        this.space = space;
    }

    public String allocReg(String var, int index, boolean write, String deny) {
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
            }
            tRegs.put(freerReg, var);
            return freerReg;
        }
        return "";
    }

    private void updateBlock(int start, int end) {
        this.start = start;

    }
}
