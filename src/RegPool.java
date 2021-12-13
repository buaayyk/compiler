import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegPool {
    public final HashMap<String, String> tRegs = new HashMap<>();
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

    public void writeBackAll(String[] five, boolean temp) {
        // temp表示临时变量不写回
        System.out.println("writeBack: ");
        System.out.println(five[0] + " " + five[1] + " " + five[2] + " " + five[3] + " " + five[4]);
        for (String reg : tRegs.keySet()) {
            System.out.println(reg);
            System.out.println("var: " + tRegs.get(reg));
            System.out.println("dirty:" + dirty.get(reg));
            if (five[0].equals("12") || five[0].equals("13")) {
                writeBack(reg, true, temp);
            } else {
                writeBack(reg, false, temp);
            }
        }
    }

    private void writeBack(String reg, boolean jump, boolean temp) {
        String var = tRegs.get(reg);
        if (temp && var.startsWith("#")) {
            // 临时变量不写回
            return;
        }
        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        System.out.println(var);
        if (!var.equals("") && dirty.get(reg)) {
            IdentSymbol identSymbol = symbolTable.searchIdentInAllLayers(var);
            if (jump) {
                // 如果是跳转语句，需要在跳转语句之前写回
                System.out.println(var);
                if (identSymbol == null) {
                    System.out.println("ppppppppppppppppppppppppppppppppppp");
                }
                finalCodes.add(finalCodes.size() - 2, "sw " + reg + "," + (space - identSymbol.getAddress()) + "($sp)");
            } else {
                // 非跳转语句可可以在之后写回
                finalCodes.add("sw " + reg + "," + (space - identSymbol.getAddress()) + "($sp)");
            }
            System.out.println("sw " + reg + "," + (space - identSymbol.getAddress()) + "($sp)");
        }
        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
    }

    public void setSpace(int space) {
        this.space = space;
    }

    public String allocReg(String var, int index, boolean write, ArrayList<String> deny) {
        System.out.println("regPool space:" + space);
        System.out.println("var :" + var);
        System.out.println("分配前： " + tRegs.values());
        // write表示是否写, deny表示该次分配禁止占用的寄存器对应的变量名
        ArrayList<String> freeRegs = new ArrayList<>();
        for (String reg : tRegs.keySet()) {
            String var1 = tRegs.get(reg);
            if (var.equals(var1)) {
                if (write) {
                    dirty.put(reg, true);
                }
                System.out.println("分配后： " + tRegs.values());
                return reg;
            }
            if (var1.equals("")) {
                freeRegs.add(reg);
            }
        }
        if (!freeRegs.isEmpty()) {
            String freerReg = freeRegs.get(0);
            if (write) {
                dirty.put(freerReg, true);
            } else {
                // 如果是读，那么需要将内存中的值先放入寄存器
                dirty.put(freerReg, false);
                IdentSymbol identSymbol = symbolTable.searchIdentInAllLayers(var);
                finalCodes.add("lw " + freerReg + "," + (space - identSymbol.getAddress()) + "($sp)");
            }
            tRegs.put(freerReg, var);
            System.out.println("分配后： " + tRegs.values());
            return freerReg;
        }
        deny.removeIf(e -> isDigit(e));
        ArrayList<String> occupiedRegs = new ArrayList<>();
        for (String reg : tRegs.keySet()) {
            String var1 = tRegs.get(reg);
            if (!deny.contains(var1)) {
                occupiedRegs.add(reg);
            }
        }
        System.out.println(indexes);
        System.out.println("start = " + start);
        System.out.println("index = " + index);
        for (int i = indexes.get(index - start); i < sequence.size(); i++) {
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
        writeBack(occupiedRegs.get(0), false, false); // 被占用的变量之后可能有用，因此需要写回
        if (write) {
            dirty.put(allocReg, true);
        } else {
            dirty.put(allocReg, false);
        }
        tRegs.put(allocReg, var);
        if (!write) {
            // 如果是读，那么需要将内存中的值先放入寄存器
            IdentSymbol identSymbol = symbolTable.searchIdentInAllLayers(var);
            finalCodes.add("lw " + allocReg + "," + (space - identSymbol.getAddress()) + "($sp)");
        }
        System.out.println("分配后： " + tRegs.values());
        return allocReg;
    }

    public void updateBlock(int start, int end) {
        this.start = start;
        indexes.clear();
        sequence.clear();
        System.out.println("================================");
        for (int i = start; i < end; i++) {
            indexes.add(sequence.size());
            String midCode = midCodes.get(i);
            System.out.println(midCode);
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
        System.out.println("===================================");
    }
}
