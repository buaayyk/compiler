import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FinalCodeGenerator {
    private final ArrayList<String> midCodes;
    private int index = 0;
    private String midCodeNow;
    private final ArrayList<String> finalCodes = new ArrayList<>();
    private final SymbolTable symbolTable = new SymbolTable();
    private int numberOfString = 0;
    private final ArrayList<Integer> depthsOfWhile = new ArrayList<>();
    // 当前代码所处的while里面嵌套的if的个数：用于break和continue的时候使用
    private boolean funcStart = false; // 标识当前已经开始编译函数
    private int numberOfNot = 0;
    private final ArrayList<String> dataFinalCodes = new ArrayList<>(); // 数据区目标代码
    private final ArrayList<Integer> depthsOfFunc = new ArrayList<>(); // 当前在函数里面套的层数---指自一个函数里面的block的层数
    private ParseMidCode parseMidCode = new ParseMidCode();

    private int space; // 当前函数所需空间的大小
    private int alloc; // 当前函数已经被占据空间的大小

    private BlockSplit blockSplit;
    private ArrayList<Integer> starts;
    private RegPool regPool;
    private ArrayList<String> paras = new ArrayList<>(); // 当前函数中分配到了a寄存器的形参
    private ArrayList<String> otherParas = new ArrayList<>(); // 没有分配到a寄存器的形参
    private boolean writeBackPara = false; // 是否在push参数前写回了所有的a寄存器
    private ArrayList<String> tempParas = new ArrayList<>();// 在形参作为实参压栈的时候临时存储所有形参

    private HashMap<String, String> var2sReg = new HashMap<>();
    private ArrayList<Block> blocks;
    private int blocksOffset;
    private ArrayList<String> allFieldsVar = new ArrayList<>();

    public FinalCodeGenerator(ArrayList<String> midCodes) {
        this.midCodes = midCodes;
        dataFinalCodes.add(".data");
        finalCodes.add(".text");
        starts = new ArrayList<>();
        regPool = new RegPool(midCodes, finalCodes, symbolTable, parseMidCode);
    }

    private boolean isDigit(String s) {
        Pattern pattern = Pattern.compile("^[-+]?[\\d]*$");
        Matcher matcher = pattern.matcher(s);
        return matcher.matches();
    }

    public ArrayList<String> getFinalCodes() {
        return new ArrayList<>(finalCodes);
    }

    private void getMidCode() {
        if (index < midCodes.size()) {
            midCodeNow = midCodes.get(index);
            index += 1;
        } else {
            midCodeNow = null;
        }
    }

    private String allocS(String var) {
        // 源代码中变量的全局寄存分配方式
        return var2sReg.getOrDefault(var, "");
    }

    public void generateFinalCodes() {
        int start = 0;
        int end = 0;
        for (String midCode : midCodes) {
            String[] five = parseMidCode.parseMidCode(midCode);
            if (five[0].equals("2")) {
                break;
            }
            end += 1;
        }
        regPool.updateBlock(start, end);
        symbolTable.addNewLayer();
        getMidCode();
        String[] five = parseMidCode.parseMidCode(midCodeNow);
        while (five != null) {

            if (starts.contains(index - 1) && starts.get(starts.size() - 1) != index - 1) {
                starts.remove(0);
                // 进入基本块前清空临时寄存器池
                regPool.updateBlock(index - 1, starts.get(0));
            }
            if (!funcStart && five[0].equals("2")) {
                funcStart = true;
                finalCodes.add("j main");
                finalCodes.add("nop");
            }
            if (midCodeNow.equals("@block_end") && starts.contains(index) && index != 0) {
                // 对于block_end需要特殊处理，因为block_end可能会丢弃一些变量的地址
                regPool.writeBackAll(five, true);
                // 基本快结尾不需要写回临时变量
                regPool.flush();
            }
            switch (five[0]) {
                case "1":
                    exp(five);
                    break;
                case "2":
                    funcDef(five);
                    break;
                case "3":
                    funcBegin(five);
                    break;
                case "4":
                    funcEnd(five);
                    break;
                case "5":
                    funcParaF(five);
                    break;
                case "6":
                    funcParaR(five);
                    break;
                case "7":
                    callFunc(five);
                    break;
                case "8":
                    funcRet(five);
                    break;
                case "9":
                    varDef(five);
                    break;
                case "10":
                    constDef(five);
                    break;
                case "11":
                    label(five);
                    break;
                case "12":
                    beq(five);
                    break;
                case "13":
                    go2(five);
                    break;
                case "14":
                    arrDef(five);
                    break;
                case "15":
                    leftArr(five);
                    break;
                case "16":
                    rightArr(five);
                    break;
                case "17":
                    getint(five);
                    break;
                case "18":
                    printf(five);
                    break;
                case "19":
                    blockBegin(five);
                    break;
                case "20":
                    blockEnd(five);
                    break;
                default:
                    break;
            }
            if (starts.contains(index) && index != 0) {
                regPool.writeBackAll(five, true);
                regPool.flush();
            }
            getMidCode();
            five = parseMidCode.parseMidCode(midCodeNow);
        }
        finalCodes.addAll(0, dataFinalCodes);
    }

    private void exp(String[] five) {
        String op = five[1];
        String leftValue = five[2];
        String opNumber1 = five[3];
        String opNumber2 = five[4];
        String reg0; // 左值分配的寄存器
        String reg1; // 操作数1分配的寄存器
        String reg2; // 操作数2分配的寄存器
        IdentSymbol identSymbol;


        // 考虑操作数1和2分别可能是常数
        if (isDigit(opNumber1)) {
            reg1 = opNumber1;
        } else {
            if (opNumber1.equals("RET")) {
                reg1 = "$v0";
            } else {
                int offset = symbolTable.getIdentAddress(opNumber1);
                if (offset == -1) {
                    // 全局变量没有被存入符号表，因此会返回-1
                    finalCodes.add("lw $t8," + opNumber1);
                    reg1 = "$t8";
                } else {
                    if (paras.contains(opNumber1)) {
                        reg1 = "$a" + paras.indexOf(opNumber1);
                    } else {
                        if (allocS(opNumber1).equals("")) {
                            // 没有分配到全局寄存器
                            ArrayList<String> deny = new ArrayList<>();
                            deny.add(leftValue);
                            deny.add(opNumber2);
                            reg1 = regPool.allocReg(opNumber1, index - 1, false, deny);
                        } else {
                            reg1 = allocS(opNumber1);
                        }
                    }
                }
            }
        }

        if (isDigit(opNumber2)) {
            reg2 = opNumber2;
        } else {
            if (opNumber2.equals("RET")) {
                reg2 = "$v0";
            } else {
                int offset = symbolTable.getIdentAddress(opNumber2);
                if (offset == -1) {
                    // 全局变量没有被存入符号表，因此会返回-1
                    finalCodes.add("lw $t9," + opNumber2);
                    reg2 = "$t9";
                } else {
                    if (paras.contains(opNumber2)) {
                        reg2 = "$a" + paras.indexOf(opNumber2);
                    } else {
                        if (allocS(opNumber2).equals("")) {
                            // 没有分配到全局寄存器
                            ArrayList<String> deny = new ArrayList<>();
                            deny.add(leftValue);
                            deny.add(opNumber1);
                            reg2 = regPool.allocReg(opNumber2, index - 1, false, deny);
                        } else {
                            reg2 = allocS(opNumber2);
                        }
                    }
                }
            }
        }

        identSymbol = symbolTable.searchIdentInAllLayers(leftValue);
        if (identSymbol == null) {
            if (leftValue.startsWith("#")) {
                // 临时变量之前一定没有声明过，因此开辟新空间存储
                identSymbol = new IdentSymbol(leftValue, false);
                symbolTable.add(identSymbol);
                alloc += 4;
                identSymbol.setAddress(alloc);
                ArrayList<String> deny = new ArrayList<>();
                deny.add(opNumber1);
                deny.add(opNumber2);
                reg0 = regPool.allocReg(leftValue, index - 1, true, deny);
            } else {
                // 用户变量之前声明过，因此没在符号表里面搜到说明是一个全局变量
                reg0 = "$v1";
            }
        } else {
            // 是一个知道位置的非全局变量
            if (paras.contains(leftValue)) {
                // 如果是形参
                reg0 = "$a" + paras.indexOf(leftValue);
            } else {
                // 不是形参
                if (allocS(leftValue).equals("")) {
                    // 没有分配到全局寄存器
                    ArrayList<String> deny = new ArrayList<>();
                    deny.add(opNumber1);
                    deny.add(opNumber2);
                    reg0 = regPool.allocReg(leftValue, index - 1, true, deny);
                } else {
                    reg0 = allocS(leftValue);
                }
            }
        }

        int kind;
        if (!isDigit(reg1)) {
            if (!isDigit(reg2)) {
                // reg1,reg2都不是数字
                kind = 0;
            } else {
                // reg1不是数字,reg2是数字
                kind = 1;
            }
        } else {
            if (!isDigit(reg2)) {
                // reg1是数字，reg2不是数字
                kind = 2;
            } else {
                // reg1,reg2都不是数字
                kind = 3;
            }
        }

        switch (op) {
            case "+":
                switch (kind) {
                    case 0:
                        finalCodes.add("add " + reg0 + "," + reg1 + "," + reg2);
                        break;
                    case 1:
                        finalCodes.add("addi " + reg0 + "," + reg1 + "," + reg2);
                        break;
                    case 2:
                        finalCodes.add("addi " + reg0 + "," + reg2 + "," + reg1);
                        break;
                    default:
                        finalCodes.add(
                                "li " + reg0 + "," + (Integer.parseInt(reg1) + Integer.parseInt(reg2)));
                        break;
                }
                break;
            case "-":
                switch (kind) {
                    // 考虑到subi并不是基础指令，即使得到subi的汇编，也会被转化
                    case 0:
                        finalCodes.add("sub " + reg0 + "," + reg1 + "," + reg2);
                        break;
                    case 1:
                        finalCodes.add("addi $at,$0," + reg2);
                        finalCodes.add("sub " + reg0 + "," + reg1 + "," + "$at");
                        break;
                    case 2:
                        finalCodes.add("addi $at,$0," + reg1);
                        finalCodes.add("sub " + reg0 + ",$at," + reg2);
                        break;
                    default:
                        finalCodes.add(
                                "li " + reg0 + "," + (Integer.parseInt(reg1) - Integer.parseInt(reg2)));
                        break;
                }
                break;
            case "!":
                if (kind == 2) {
                    finalCodes.add("seq " + reg0 + "," + "$0" + "," + reg2);
                } else {
                    if (Integer.parseInt(reg2) == 0) {
                        finalCodes.add("li " + reg0 + ",1");
                    } else {
                        finalCodes.add("li " + reg0 + ",0");
                    }
                }
                break;
            case "*":
                switch (kind) {
                    case 0:
                        finalCodes.add("mult " + reg1 + "," + reg2);
                        finalCodes.add("mflo " + reg0);
                        break;
                    case 1:
                        // TODO 优化可能存在的地方
                        finalCodes.add("li $t9," + reg2);
                        finalCodes.add("mult " + reg1 + ",$t9");
                        finalCodes.add("mflo " + reg0);
                        break;
                    case 2:
                        finalCodes.add("li $t8," + reg1);
                        finalCodes.add("mult $t8," + reg2);
                        finalCodes.add("mflo " + reg0);
                        break;
                    default:
                        finalCodes.add("li " + reg0 + "," + (Integer.parseInt(reg1) * Integer.parseInt(reg2)));
                        break;
                }
                break;
            case "/":
                switch (kind) {
                    case 0:
                        finalCodes.add("div " + reg1 + "," + reg2);
                        finalCodes.add("mflo " + reg0);
                        break;
                    case 1:
                        // TODO 优化可能存在的地方
                        finalCodes.add("li $t9," + reg2);
                        finalCodes.add("div " + reg1 + ",$t9");
                        finalCodes.add("mflo " + reg0);
                        break;
                    case 2:
                        finalCodes.add("li $t8," + reg1);
                        finalCodes.add("div $t8," + reg2);
                        finalCodes.add("mflo " + reg0);
                        break;
                    default:
                        finalCodes.add("li " + reg0 + "," + (Integer.parseInt(reg1) / Integer.parseInt(reg2)));
                        break;
                }
                break;
            case "%":
                switch (kind) {
                    case 0:
                        finalCodes.add("div " + reg1 + "," + reg2);
                        finalCodes.add("mfhi " + reg0);
                        break;
                    case 1:
                        // TODO 优化可能存在的地方
                        finalCodes.add("li $t9," + reg2);
                        finalCodes.add("div " + reg1 + ",$t9");
                        finalCodes.add("mfhi " + reg0);
                        break;
                    case 2:
                        finalCodes.add("li $t8," + reg1);
                        finalCodes.add("div $t8," + reg2);
                        finalCodes.add("mfhi " + reg0);
                        break;
                    default:
                        finalCodes.add("li " + reg0 + "," + (Integer.parseInt(reg1) % Integer.parseInt(reg2)));
                        break;
                }
                break;
            case "<":
                switch (kind) {
                    case 0:
                        finalCodes.add("slt " + reg0 + "," + reg1 + "," + reg2);
                        break;
                    case 1:
                        finalCodes.add("slti " + reg0 + "," + reg1 + "," + reg2);
                        break;
                    case 2:
                        finalCodes.add("li $t8," + reg1);
                        finalCodes.add("slt " + reg0 + ",$t8," + reg2);
                        break;
                    default:
                        if (Integer.parseInt(reg1) < Integer.parseInt(reg2)) {
                            finalCodes.add("li " + reg0 + ",1");
                        } else {
                            finalCodes.add("li " + reg0 + ",0");
                        }
                        break;
                }
                break;
            case ">":
                switch (kind) {
                    case 0:
                        finalCodes.add("sgt " + reg0 + "," + reg1 + "," + reg2);
                        break;
                    case 1:
                        finalCodes.add("li $t9," + reg2);
                        finalCodes.add("sgt " + reg0 + "," + reg1 + ",$t9");
                        break;
                    case 2:
                        finalCodes.add("slti " + reg0 + "," + reg2 + "," + reg1);
                        break;
                    default:
                        if (Integer.parseInt(reg1) > Integer.parseInt(reg2)) {
                            finalCodes.add("li " + reg0 + ",1");
                        } else {
                            finalCodes.add("li " + reg0 + ",0");
                        }
                        break;
                }
                break;
            case "<=":
                switch (kind) {
                    case 0:
                        finalCodes.add("sle " + reg0 + "," + reg1 + "," + reg2);
                        break;
                    case 1:
                        finalCodes.add("li $t9," + reg2);
                        finalCodes.add("sle " + reg0 + "," + reg1 + ",$t9");
                        break;
                    case 2:
                        finalCodes.add("li $t8," + reg1);
                        finalCodes.add("sle " + reg0 + ",$t8," + reg2);
                        break;
                    default:
                        if (Integer.parseInt(reg1) <= Integer.parseInt(reg2)) {
                            finalCodes.add("li " + reg0 + ",1");
                        } else {
                            finalCodes.add("li " + reg0 + ",0");
                        }
                        break;
                }
                break;
            case ">=":
                switch (kind) {
                    case 0:
                        finalCodes.add("sge " + reg0 + "," + reg1 + "," + reg2);
                        break;
                    case 1:
                        finalCodes.add("li $t9," + reg2);
                        finalCodes.add("sge " + reg0 + "," + reg1 + ",$t9");
                        break;
                    case 2:
                        finalCodes.add("li $t8," + reg1);
                        finalCodes.add("sge " + reg0 + ",$t8," + reg2);
                        break;
                    default:
                        if (Integer.parseInt(reg1) >= Integer.parseInt(reg2)) {
                            finalCodes.add("li " + reg0 + ",1");
                        } else {
                            finalCodes.add("li " + reg0 + ",0");
                        }
                        break;
                }
                break;
            case "==":
                switch (kind) {
                    case 0:
                        finalCodes.add("seq " + reg0 + "," + reg1 + "," + reg2);
                        break;
                    case 1:
                        finalCodes.add("li $t9," + reg2);
                        finalCodes.add("seq " + reg0 + "," + reg1 + ",$t9");
                        break;
                    case 2:
                        finalCodes.add("li $t8," + reg1);
                        finalCodes.add("seq " + reg0 + ",$t8," + reg2);
                        break;
                    default:
                        if (Integer.parseInt(reg1) == Integer.parseInt(reg2)) {
                            finalCodes.add("li " + reg0 + ",1");
                        } else {
                            finalCodes.add("li " + reg0 + ",0");
                        }
                        break;
                }
                break;
            case "!=":
                switch (kind) {
                    case 0:
                        finalCodes.add("sne " + reg0 + "," + reg1 + "," + reg2);
                        break;
                    case 1:
                        finalCodes.add("li $t9," + reg2);
                        finalCodes.add("sne " + reg0 + "," + reg1 + ",$t9");
                        break;
                    case 2:
                        finalCodes.add("li $t8," + reg1);
                        finalCodes.add("sne " + reg0 + ",$t8," + reg2);
                        break;
                    default:
                        if (Integer.parseInt(reg1) != Integer.parseInt(reg2)) {
                            finalCodes.add("li " + reg0 + ",1");
                        } else {
                            finalCodes.add("li " + reg0 + ",0");
                        }
                        break;
                }
                break;
            default:
                break;
        }
        if (reg0.equals("$v1")) {
            // 如果是全局变量，则需要写回
            finalCodes.add("sw $v1," + leftValue);
        }
    }

    private String getLatestFuncBegin() {
        String funcBegin = null;
        for (int i = index - 1; i >= 0; i--) {
            String midCode = midCodes.get(i);
            String[] ss = midCode.split(" ");
            if (ss[0].equals("@func_begin")) {
                funcBegin = ss[1];
                break;
            }
        }
        return funcBegin;
    }

    private void funcDef(String[] five) {
        symbolTable.addNewLayer();
        depthsOfFunc.add(1);
        paras.clear();
        otherParas.clear();
        int count = 0;
        // 记录所有通过a寄存器传值的变量名
        for (int i = index; i < midCodes.size(); i++) {
            String midCode = midCodes.get(i);
            String[] five1 = parseMidCode.parseMidCode(midCode);
            if (five1[0].equals("5")) {
                if (count < 4) {
                    paras.add(five1[1]);
                } else {
                    // 最多只有4个形参存入寄存器,多出来的需要存在栈上
                    otherParas.add(five1[1]);
                }
                count += 1;
            } else {
                break;
            }
        }
    }

    private void funcBegin(String[] five) {
        space = 0;
        alloc = 0;
        int count = 0; // 记录函数块的大小
        for (int i = index; i < midCodes.size(); i++) {
            String midCode = midCodes.get(i);
            String[] five1 = parseMidCode.parseMidCode(midCode);
            switch (five1[0]) {
                case "1":
                case "3":
                case "9":
                case "10":
                case "14":
                case "16":
                case "17":
                    space += addSpace(five1);
                    break;
                default:
                    break;
            }
            count += 1;
            if (five1[0].equals("4")) {
                break;
            }
        }

        space += 4; // 地址寄存器的空间

        ArrayList<String> arrayList = new ArrayList<>(allFieldsVar);
        arrayList.addAll(paras);
        // 这些变量不会去分全局寄存器
        blockSplit = new BlockSplit(new ArrayList<>(midCodes.subList(index - 1, index - 1 + count)), arrayList);
        blockSplit.blockSplit(false);
        blocksOffset = index - 1;

        blocks = blockSplit.getBlocks();  // 代码分块
        ActiveAnalysis activeAnalysis = new ActiveAnalysis(blocks);
        activeAnalysis.analyse(); // 活跃变量分析
        ConflictMap conflictMap = new ConflictMap(blocks);
        conflictMap.graphColoring(8); // 只用8个s寄存器的图着色
        var2sReg = conflictMap.getVar2sReg();
        System.out.println(five[1]);
        System.out.println(var2sReg);
        System.out.println(var2sReg.values());

        starts = blockSplit.getStarts();
        for (int i = 0; i < starts.size(); i++) {
            starts.set(i, starts.get(i) + index - 1);
        }
        regPool.writeBackAll(five, true);
        regPool.flush();
        regPool.setSpace(space);
        starts.remove(0);
        regPool.updateBlock(index - 1, starts.get(0));

        String funcName = five[1];
        finalCodes.add(funcName + ":");
        finalCodes.add("addi $sp,$sp," + (-space));
        alloc += 4;
        finalCodes.add("sw $ra," + (space - alloc) + "($sp)");
        System.out.println(otherParas);
        for (String otherPara : otherParas) {
            if (!allocS(otherPara).equals("")) {
                // 如果形参被分配了全局寄存器
                System.out.println("???????????????????????");
                IdentSymbol identSymbol = symbolTable.searchIdentInAllLayers(otherPara);
                String reg = allocS(otherPara);
                finalCodes.add("lw " + reg + "," + (space - identSymbol.getAddress()) + "($sp)");
            }
        }
    }

    // 该函数只有在进入函数之后才能使用
    private int addSpace(String[] five) {
        // 根据传入的语句计算需要分配多少空间
        int space = 0;
        switch (five[0]) {
            case "1":
                // 左侧临时变量分配空间
                if (five[2].startsWith("#")) {
                    space = 4;
                }
                break;
            case "3":
                // 地址寄存器分配空间
                space = 4;
                break;
            case "9":
                // 变量定义分配空间
                space = 4;
                break;
            case "10":
                // 常量定义分配空间
                space = 4;
                break;
            case "14":
                // 数组定义分配空间（数组空间+指针空间）
                space = 4 + 4 * Integer.parseInt(five[2]);
                break;
            case "16":
                if (five[1].startsWith("#")) {
                    space = 4;
                }
                break;
            case "17":
                space = 4;
                break;
            default:
                break;
        }
        return space;
    }

    private void funcEnd(String[] five) {
        String latestFuncBegin = getLatestFuncBegin();
        if (latestFuncBegin != null && latestFuncBegin.equals("main")) {
            finalCodes.add("li $v0,10");
            finalCodes.add("syscall");
        } else {
            finalCodes.add("addi $sp,$sp," + space);
            finalCodes.add("lw $ra,-4($sp)");
            finalCodes.add("jr $ra");
            finalCodes.add("nop");
            symbolTable.removeCurrentLayer();
        }
        depthsOfFunc.remove(depthsOfFunc.size() - 1);
    }

    private void funcParaF(String[] five) {
        ArrayList<String[]> fives = new ArrayList<>();
        fives.add(five);
        getMidCode();
        String[] five1 = parseMidCode.parseMidCode(midCodeNow);
        while (five1 != null && five1[0].equals("5")) {
            fives.add(five1);
            getMidCode();
            five1 = parseMidCode.parseMidCode(midCodeNow);
        }
        index -= 1;
        int count = 0;
        for (String[] strings : fives) {
            String paraFName = strings[1];
            IdentSymbol identSymbol = new IdentSymbol(paraFName, false);
            symbolTable.add(identSymbol);
            count += 1;
            identSymbol.setAddress(4 * (count - fives.size()));
        }
    }

    private void funcParaR(String[] five) {
        if (!writeBackPara) {
            // 写回所有a寄存器中的值
            for (int i = 0; i < paras.size(); i++) {
                IdentSymbol identSymbol = symbolTable.searchIdentInAllLayers(paras.get(i));
                finalCodes.add("sw $a" + i + "," + (space - identSymbol.getAddress()) + "($sp)");
            }
            tempParas = new ArrayList<>(paras);
            paras.clear();
            writeBackPara = true;
        }
        int n = 0; // 该条语句前面push的数量
        for (int i = index - 2; i >= 0; i--) {
            String midCode = midCodes.get(i);
            String[] ss = midCode.split(" ");
            if (ss[0].equals("push")) {
                n += 1;
            } else {
                break;
            }
        }
        alloc += 4; // 分配空间
        String funcRExp = five[1];
        // 在中间代码生成阶段已经保证了不会出现push a[1](a[1]是一个值)的情况发生
        if (five[2].equals("")) {
            // 此时传值
            if (isDigit(funcRExp)) {
                if (n < 4) {
                    finalCodes.add("li $a" + n + "," + funcRExp);
                } else {
                    finalCodes.add("li $t8," + funcRExp);
                    finalCodes.add("sw $t8," + (space - alloc) + "($sp)");
                }
            } else {
                int offset = symbolTable.getIdentAddress(funcRExp);
                if (offset == -1) {
                    // 符号表里找不到，说明一定是全局变量
                    if (n < 4) {
                        finalCodes.add("lw $a" + n + "," + funcRExp);
                    } else {
                        finalCodes.add("lw $t8," + funcRExp);
                        finalCodes.add("sw $t8," + (space - alloc) + "($sp)");
                    }
                } else {
                    // 此时不用考虑是形参的情况，因为a寄存器的值已经变得不可靠
                    String reg;
                    if (allocS(funcRExp).equals("")) {
                        reg = regPool.allocReg(funcRExp, index - 1, false, new ArrayList<>());
                    } else {
                        reg = allocS(funcRExp);
                    }
                    if (n < 4) {
                        finalCodes.add("move $a" + n + "," + reg);
                    } else {
                        finalCodes.add("sw " + reg + "," + (space - alloc) + "($sp)");
                    }
                }
            }
        } else {
            // 此时传地址
            // 此时的funcExp不可能是纯数字
            String offset1 = five[2];
            String reg1;
            String reg2;
            int offset = symbolTable.getIdentAddress(funcRExp);
            if (offset == -1) {
                finalCodes.add("la $t8," + funcRExp);
                reg1 = "$t8";
            } else {
                if (allocS(funcRExp).equals("")) {
                    ArrayList<String> deny = new ArrayList<>();
                    deny.add(offset1);
                    reg1 = regPool.allocReg(funcRExp, index - 1, false, deny);
                } else {
                    reg1 = allocS(funcRExp);
                }
            }
            if (isDigit(offset1)) {
                if (n < 4) {
                    finalCodes.add("addi $a" + n + "," + reg1 + "," + (4 * Integer.parseInt(offset1)));
                } else {
                    finalCodes.add("addi $t8" + "," + reg1 + "," + (4 * Integer.parseInt(offset1)));
                    finalCodes.add("sw $t8," + (space - alloc) + "($sp)");
                }
            } else {
                if (allocS(offset1).equals("")) {
                    ArrayList<String> deny = new ArrayList<>();
                    deny.add(funcRExp);
                    reg2 = regPool.allocReg(offset1, index - 1, false, deny);
                } else {
                    reg2 = allocS(offset1);
                }
                finalCodes.add("sll $t9," + reg2 + ",2");
                if (n < 4) {
                    finalCodes.add("add $a" + n + "," + reg1 + ",$t9");
                } else {
                    finalCodes.add("add $t8," + reg1 + ",$t9");
                    finalCodes.add("sw $t8," + (space - alloc) + "($sp)");
                }
            }
        }
    }

    private void callFunc(String[] five) {
        // TODO, 此处需要分析活跃变量
        HashSet<String> activeVars = new HashSet<>();
        Block block = new Block(-1, new ArrayList<>(), new ArrayList<>());
        for (Block block1 : blocks) {
            if (block1.getIndex() + blocksOffset <= index - 1
                    && index - 1 < block1.getIndex() + block1.size() + blocksOffset) {
                block = block1;
            }
        }
        ArrayList<ArrayList<HashSet<String>>> codes = block.getCodes();
        System.out.println(block.getSplitCodes());
        activeVars = block.getActiveOut();
        System.out.println("函数名：" + five[1]);
        System.out.println(activeVars);
        for (int i = blocksOffset + block.getIndex() + block.size() - 1; i >= index - 1; i--) {
            ArrayList<HashSet<String>> code = codes.get(i - blocksOffset - block.getIndex());
            HashSet<String> defs = code.get(0);
            HashSet<String> uses = code.get(1);
            activeVars.removeAll(defs);
            activeVars.addAll(uses);
        }
        System.out.println(activeVars);
        for (String name : activeVars) {
            String reg = allocS(name);
            if (!reg.equals("")) {
                // 把活跃变量写回对应地址
                IdentSymbol identSymbol = symbolTable.searchIdentInAllLayers(name);
                finalCodes.add("sw " + reg + "," + (space - identSymbol.getAddress()) + "($sp)");
            }
        }

        regPool.writeBackAll(five, false); // 调用函数一般在基本块中间，因此临时变量需要写回
        regPool.flush();
        finalCodes.add("addi $sp,$sp," + (space - alloc));
        finalCodes.add("jal " + five[1]);
        finalCodes.add("nop");
        int number = 0; // 形参的个数
        String midCode;
        for (int i = index - 2; i >= 0; i--) {
            midCode = midCodes.get(i);
            if (midCode.contains("push")) {
                number += 1;
            } else {
                break;
            }
        }

        finalCodes.add("addi $sp,$sp," + (alloc - space));
        if (writeBackPara) {
            paras = new ArrayList<>(tempParas);
            tempParas.clear();
            for (int i = 0; i < paras.size(); i++) {
                IdentSymbol identSymbol = symbolTable.searchIdentInAllLayers(paras.get(i));
                finalCodes.add("lw $a" + i + "," + (space - identSymbol.getAddress()) + "($sp)");
            }
        }
        writeBackPara = false;
        alloc = alloc - 4 * number;

        for (String name : activeVars) {
            String reg = allocS(name);
            if (!reg.equals("")) {
                // 把活跃变量对应的寄存器恢复
                IdentSymbol identSymbol = symbolTable.searchIdentInAllLayers(name);
                finalCodes.add("lw " + reg + "," + (space - identSymbol.getAddress()) + "($sp)");
            }
        }
    }

    private void funcRet(String[] five) {
        if (!five[1].equals("")) {
            String funcRetExp = five[1];
            if (isDigit(five[1])) {
                // 返回值可能是一个常数
                finalCodes.add("li $v0," + five[1]);
            } else {
                int offset = symbolTable.getIdentAddress(funcRetExp);
                if (offset == -1) {
                    // 变量在数据区
                    finalCodes.add("lw $v0," + funcRetExp);
                } else {
                    if (paras.contains(five[1])) {
                        finalCodes.add("move $v0,$a" + paras.indexOf(five[1]));
                    } else {
                        String reg;
                        if (allocS(five[1]).equals("")) {
                            reg = regPool.allocReg(five[1], index, false, new ArrayList<>());
                        } else {
                            reg = allocS(five[1]);
                        }
                        finalCodes.add("move $v0," + reg);
                    }
                }
            }
        }
        String latestFuncBegin = getLatestFuncBegin();
        if (latestFuncBegin != null && latestFuncBegin.equals("main")) {
            // 如果是main函数，那么代码运行结束
            finalCodes.add("li $v0,10");
            finalCodes.add("syscall");
        } else {
            // 如果不是main函数，则释放栈空间，然后跳转回调用函数
            finalCodes.add("addi $sp,$sp," + space);
            finalCodes.add("lw $ra,-4($sp)");
            finalCodes.add("jr $ra");
            finalCodes.add("nop");
        }
    }

    private void varDef(String[] five) {
        if (!funcStart) {
            // 如果是在数据区
            dataFinalCodes.add(five[1] + ": .space 4");
            allFieldsVar.add(five[1]);
        } else {
            alloc += 4;
            IdentSymbol identSymbol = new IdentSymbol(five[1], false);
            symbolTable.add(identSymbol);
            identSymbol.setAddress(alloc);
        }
    }

    private void constDef(String[] five) {
        if (!funcStart) {
            // 如果是在数据区
            dataFinalCodes.add(five[1] + ": .space 4");
            allFieldsVar.add(five[1]);
        } else {
            alloc += 4;
            IdentSymbol identSymbol = new IdentSymbol(five[1], true);
            symbolTable.add(identSymbol);
            identSymbol.setAddress(alloc);
        }
    }

    private void label(String[] five) {
        if (five[1].contains("loop_begin")) {
            symbolTable.addNewLayer();
        }
        if (five[1].contains("loop_entry")) {
            depthsOfWhile.add(1);
            depthsOfFunc.set(depthsOfFunc.size() - 1, depthsOfFunc.get(depthsOfFunc.size() - 1) + 1);
            symbolTable.removeCurrentLayer();
            symbolTable.addNewLayer();
        }
        if (five[1].contains("loop_end")) {
            depthsOfWhile.remove(depthsOfWhile.size() - 1);
            depthsOfFunc.set(depthsOfFunc.size() - 1, depthsOfFunc.get(depthsOfFunc.size() - 1) - 1);
            symbolTable.removeCurrentLayer();
        }
        if (five[1].contains("if_begin")) {
            if (five[1].charAt(five[1].length() - 1) != '0') {
                // 最后一个字符不是0，说明不是第一个if_begin
                if (depthsOfWhile.size() > 0) {
                    // 如果当前的if正处于某个while循环当中，则层数减一，用于之后的break和continue计算释放栈的空间时使用
                    depthsOfWhile.set(depthsOfWhile.size() - 1,
                            depthsOfWhile.get(depthsOfWhile.size() - 1) - 1);
                }
                depthsOfFunc.set(depthsOfFunc.size() - 1, depthsOfFunc.get(depthsOfFunc.size() - 1) - 1);
                symbolTable.removeCurrentLayer();
            }
            symbolTable.addNewLayer();
        }
        if (five[1].contains("if_entry")) {
            if (depthsOfWhile.size() > 0) {
                // 如果当前的if正处于某个while循环当中，则层数加一，用于之后的break和continue计算释放栈的空间时使用
                depthsOfWhile.set(depthsOfWhile.size() - 1,
                        depthsOfWhile.get(depthsOfWhile.size() - 1) + 1);
            }
            depthsOfFunc.set(depthsOfFunc.size() - 1, depthsOfFunc.get(depthsOfFunc.size() - 1) + 1);
            symbolTable.removeCurrentLayer();
            symbolTable.addNewLayer();
        }
        if (five[1].contains("if_end")) {
            if (depthsOfWhile.size() > 0) {
                // endif出block
                depthsOfWhile.set(depthsOfWhile.size() - 1,
                        depthsOfWhile.get(depthsOfWhile.size() - 1) - 1);
            }
            depthsOfFunc.set(depthsOfFunc.size() - 1, depthsOfFunc.get(depthsOfFunc.size() - 1) - 1);
            symbolTable.removeCurrentLayer();
        }
        if (five[1].contains("or")) {
            symbolTable.removeCurrentLayer();
            symbolTable.addNewLayer();
        }
        finalCodes.add(five[1] + ":");
    }

    private void blockBegin(String[] five) {
        if (depthsOfWhile.size() > 0) {
            depthsOfWhile.set(depthsOfWhile.size() - 1,
                    depthsOfWhile.get(depthsOfWhile.size() - 1) + 1);
        }
        depthsOfFunc.set(depthsOfFunc.size() - 1, depthsOfFunc.get(depthsOfFunc.size() - 1) + 1);
        symbolTable.addNewLayer();
    }

    private void blockEnd(String[] five) {
        if (depthsOfWhile.size() > 0) {
            // endif出block
            depthsOfWhile.set(depthsOfWhile.size() - 1,
                    depthsOfWhile.get(depthsOfWhile.size() - 1) - 1);
        }
        depthsOfFunc.set(depthsOfFunc.size() - 1, depthsOfFunc.get(depthsOfFunc.size() - 1) - 1);
        symbolTable.removeCurrentLayer();
    }

    private void beq(String[] five) {
        String opNumber1 = five[1];
        String label = five[3];
        String reg;
        if (isDigit(opNumber1)) {
            reg = "$t8";
            finalCodes.add("li $t8," + opNumber1);
        } else {
            int offset = symbolTable.getIdentAddress(opNumber1);
            if (offset == -1) {
                // 全局变量
                reg = "$t8";
                finalCodes.add("lw $t8," + opNumber1);
            } else {
                if (paras.contains(opNumber1)) {
                    // 是形参
                    reg = "$a" + paras.indexOf(opNumber1);
                } else {
                    // 不是形参
                    if (allocS(opNumber1).equals("")) {
                        reg = regPool.allocReg(opNumber1, index - 1, false, new ArrayList<>());
                    } else {
                        reg = allocS(opNumber1);
                    }
                }
            }
        }
        finalCodes.add("beq " + reg + ",0," + label);
        finalCodes.add("nop");
    }

    private void go2(String[] five) {
        String label = five[1];
        finalCodes.add("j " + label);
        finalCodes.add("nop");
    }

    private void arrDef(String[] five) {
        String arrName = five[1];
        String length = five[2];
        if (!funcStart) {
            // 如果是在数据区
            dataFinalCodes.add(arrName + ": .space " + (4 * Integer.parseInt(length)));
            allFieldsVar.add(five[1]);
        } else {
            IdentSymbol arrPoint = new IdentSymbol(arrName, true);
            alloc += 4;
            arrPoint.setAddress(alloc);

            String reg;
            if (allocS(arrName).equals("")) {
                // 使用t8不会占据其它临时寄存器
                reg = "$t8";
            } else {
                reg = allocS(arrName);
            }
            finalCodes.add("addi " + reg + ",$sp," + (space - alloc - 4 * Integer.parseInt(length)));
            finalCodes.add("sw " + reg + "," + (space - alloc) + "($sp)");

            IdentSymbol arr = new IdentSymbol("@array " + arrName, true);
            arr.add(Integer.valueOf(length));
            alloc += 4 * Integer.parseInt(length);
            arr.setAddress(alloc);
            symbolTable.add(arrPoint);
            symbolTable.add(arr);

        }
    }

    private void leftArr(String[] five) {
        String arrName = five[1];
        String offset = five[2];
        String right = five[3];
        String reg1;
        String reg2;
        String reg3;

        int offset1;
        if (isDigit(offset)) {
            finalCodes.add("li $t8," + (4 * Integer.parseInt(offset)));
            reg2 = "$t8";
        } else {
            offset1 = symbolTable.getIdentAddress(offset);
            if (offset1 == -1) {
                finalCodes.add("lw $t8," + offset);
                reg2 = "$t8";
            } else {
                if (paras.contains(offset)) {
                    reg2 = "$a" + paras.indexOf(offset);
                } else {
                    if (allocS(offset).equals("")) {
                        ArrayList<String> deny = new ArrayList<>();
                        deny.add(right);
                        deny.add(arrName);
                        reg2 = regPool.allocReg(offset, index - 1, false, deny);
                    } else {
                        reg2 = allocS(offset);
                    }
                }
            }
            finalCodes.add("sll $t8," + reg2 + ",2");
        }

        offset1 = symbolTable.getIdentAddress(arrName);
        if (offset1 == -1) {
            reg1 = "$t9";
            finalCodes.add("la $t9," + arrName);
        } else {
            if (paras.contains(arrName)) {
                reg1 = "$a" + paras.indexOf(arrName);
            } else {
                if (allocS(arrName).equals("")) {
                    ArrayList<String> deny = new ArrayList<>();
                    deny.add(right);
                    deny.add(offset);
                    reg1 = regPool.allocReg(arrName, index - 1, false, deny);
                } else {
                    reg1 = allocS(arrName);
                }
            }
        }
        // t8算出了要写的地址
        finalCodes.add("add $t8," + "$t8," + reg1);

        if (right.equals("RET")) {
            reg3 = "$v0";
        } else {
            if (isDigit(right)) {
                reg3 = "$t9";
                finalCodes.add("li $t9," + right);
            } else {
                offset1 = symbolTable.getIdentAddress(right);
                if (offset1 == -1) {
                    reg3 = "$t9";
                    finalCodes.add("lw $t9," + right);
                } else {
                    if (paras.contains(right)) {
                        reg3 = "$a" + paras.indexOf(right);
                    } else {
                        if (allocS(right).equals("")) {
                            ArrayList<String> deny = new ArrayList<>();
                            deny.add(arrName);
                            deny.add(offset);
                            reg3 = regPool.allocReg(right, index - 1, false, deny);
                        } else {
                            reg3 = allocS(right);
                        }
                    }
                }
            }
        }

        finalCodes.add("sw " + reg3 + ",0($t8)");
    }

    private void rightArr(String[] five) {
        String left = five[1];
        String arrName = five[2];
        String offset = five[3];

        String reg1;
        String reg2;
        String reg3;

        int offset1;

        if (isDigit(offset)) {
            finalCodes.add("li $t8," + (4 * Integer.parseInt(offset)));
            reg3 = "$t8";
        } else {
            offset1 = symbolTable.getIdentAddress(offset);
            if (offset1 == -1) {
                finalCodes.add("lw $t8," + offset);
                reg3 = "$t8";
            } else {
                if (paras.contains(offset)) {
                    reg3 = "$a" + paras.indexOf(offset);
                } else {
                    if (allocS(offset).equals("")) {
                        ArrayList<String> deny = new ArrayList<>();
                        deny.add(left);
                        deny.add(arrName);
                        reg3 = regPool.allocReg(offset, index - 1, false, deny);
                    } else {
                        reg3 = allocS(offset);
                    }
                }
            }
            finalCodes.add("sll $t8," + reg3 + ",2");
        }

        offset1 = symbolTable.getIdentAddress(arrName);
        if (offset1 == -1) {
            reg2 = "$t9";
            finalCodes.add("la $t9," + arrName);
        } else {
            if (paras.contains(arrName)) {
                reg2 = "$a" + paras.indexOf(arrName);
            } else {
                if (allocS(arrName).equals("")) {
                    ArrayList<String> deny = new ArrayList<>();
                    deny.add(left);
                    deny.add(offset);
                    reg2 = regPool.allocReg(arrName, index - 1, false, deny);
                } else {
                    reg2 = allocS(arrName);
                }
            }
        }
        // t8算出了要读的地址
        finalCodes.add("add $t8," + "$t8," + reg2);

        offset1 = symbolTable.getIdentAddress(left);
        if (offset1 == -1) {
            if (left.startsWith("#")) {
                // 临时变量，需要新开辟空间
                IdentSymbol identSymbol = new IdentSymbol(left, false);
                symbolTable.add(identSymbol);
                alloc += 4;
                identSymbol.setAddress(alloc);
                reg1 = regPool.allocReg(left, index - 1, true, new ArrayList<>());
                finalCodes.add("lw " + reg1 + ",0($t8)");
            } else {
                finalCodes.add("lw $t8,0($t8)");
                finalCodes.add("sw $t8," + left);
            }
        } else {
            if (paras.contains(left)) {
                reg1 = "$a" + paras.indexOf(left);
            } else {
                if (allocS(left).equals("")) {
                    ArrayList<String> deny = new ArrayList<>();
                    deny.add(arrName);
                    deny.add(offset);
                    reg1 = regPool.allocReg(left, index - 1, false, deny);
                } else {
                    reg1 = allocS(left);
                }
            }
            finalCodes.add("lw " + reg1 + ",0($t8)");
        }
    }

    private void getint(String[] five) {
        String ident = five[1];
        finalCodes.add("li $v0,5");
        finalCodes.add("syscall");
        IdentSymbol identSymbol = new IdentSymbol(ident, false);
        symbolTable.add(identSymbol);
        alloc += 4;
        identSymbol.setAddress(alloc);
        String reg = regPool.allocReg(ident, index - 1, true, new ArrayList<>());
        finalCodes.add("move " + reg + ",$v0");
    }

    private void printf(String[] five) {
        if (paras.size() != 0) {
            finalCodes.add("move $t8,$a0");
        }
        if (five[1].equals("%d")) {
            String reg;
            String ident = five[2];
            if (ident.equals("RET")) {
                finalCodes.add("move $a0,$v0");
            } else {
                int offset = symbolTable.getIdentAddress(ident);
                if (offset == -1) {
                    finalCodes.add("lw $a0," + ident);
                } else {
                    if (paras.contains(ident)) {
                        reg = "$a" + paras.indexOf(ident);
                    } else {
                        if (allocS(ident).equals("")) {
                            reg = regPool.allocReg(ident, index - 1, false, new ArrayList<>());
                        } else {
                            reg = allocS(ident);
                        }
                    }
                    finalCodes.add("move $a0," + reg);
                }
            }
            finalCodes.add("li $v0,1");
            finalCodes.add("syscall");
        } else {
            dataFinalCodes.add("printf_string" + numberOfString + ": .asciiz " + "\"" + five[1].replace("#", " ") + "\"");
            finalCodes.add("la $a0," + "printf_string" + numberOfString);
            finalCodes.add("li $v0,4");
            finalCodes.add("syscall");
            numberOfString += 1;
        }
        if (paras.size() != 0) {
            finalCodes.add("move $a0,$t8");
        }
    }
}
