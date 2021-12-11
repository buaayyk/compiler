import java.util.ArrayList;
import java.util.Queue;
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

    public FinalCodeGenerator(ArrayList<String> midCodes) {
        this.midCodes = midCodes;
        dataFinalCodes.add(".data");
        finalCodes.add(".text");
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
        return "";
    }

    public void generateFinalCodes() {
        symbolTable.addNewLayer();
        getMidCode();
        String[] five = parseMidCode.parseMidCode(midCodeNow);
        while (five != null) {
            if (starts.contains(index) && starts.get(starts.size() - 1) != index) {
                starts.remove(0);
                regPool.updateBlock(index, index + starts.get(0));
            }
            if (!funcStart && five[0].equals("2")) {
                funcStart = true;
                finalCodes.add("j main");
                finalCodes.add("nop");
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
            getMidCode();
            five = parseMidCode.parseMidCode(midCodeNow);
        }
        finalCodes.addAll(0, dataFinalCodes);
    }

    private void exp(String[] five) {
        // TODO:现阶段没有使用立即数有关的指令，之后需要优化
        String op = five[1];
        String leftValue = five[2];
        String opNumber1 = five[3];
        String opNumber2 = five[4];
        // 考虑操作数1和2分别可能是常数
        if (isDigit(opNumber1)) {
            finalCodes.add("li $t1," + opNumber1);
        } else {
            if (opNumber1.equals("RET")) {
                finalCodes.add("move $t1,$v0");
            } else {
                int offset = symbolTable.getIdentAddress(opNumber1);
                if (offset == -1) {
                    // 全局变量没有被存入符号表，因此会返回-1
                    finalCodes.add("lw $t1," + opNumber1);
                } else {
                    finalCodes.add("lw $t1," + (space - offset) + "($sp)");
                }
            }
        }
        if (isDigit(opNumber2)) {
            finalCodes.add("li $t2," + opNumber2);
        } else {
            if (opNumber2.equals("RET")) {
                finalCodes.add("move $t2,$v0");
            } else {
                int offset = symbolTable.getIdentAddress(opNumber2);
                if (offset == -1) {
                    // 全局变量没有被存入符号表，因此会返回-1
                    finalCodes.add("lw $t2," + opNumber2);
                } else {
                    finalCodes.add("lw $t2," + (space - offset) + "($sp)");
                }
            }
        }
        switch (op) {
            case "+":
                finalCodes.add("add $t0,$t1,$t2");
                break;
            case "-":
                finalCodes.add("sub $t0,$t1,$t2");
                break;
            case "!":
                finalCodes.add("seq $t0,$t2,$zero");
                break;
            case "*":
                finalCodes.add("mult $t1,$t2");
                finalCodes.add("mflo $t0");
                break;
            case "/":
                finalCodes.add("div $t1,$t2");
                finalCodes.add("mflo $t0");
                break;
            case "%":
                finalCodes.add("div $t1,$t2");
                finalCodes.add("mfhi $t0");
                break;
            case "<":
                finalCodes.add("slt $t0,$t1,$t2");
                break;
            case ">":
                finalCodes.add("sgt $t0,$t1,$t2");
                break;
            case "<=":
                finalCodes.add("sle $t0,$t1,$t2");
                break;
            case ">=":
                finalCodes.add("sge $t0,$t1,$t2");
                break;
            case "==":
                finalCodes.add("seq $t0,$t1,$t2");
                break;
            case "!=":
                finalCodes.add("sne $t0,$t1,$t2");
                break;
            default:
                break;
        }
        IdentSymbol identSymbol;
        identSymbol = symbolTable.searchIdentInAllLayers(leftValue);
        if (identSymbol == null) {
            if (leftValue.startsWith("#")) {
                // 临时变量之前一定没有声明过，因此新开辟空间存储
                identSymbol = new IdentSymbol(leftValue, false);
                symbolTable.add(identSymbol);
                alloc += 4;
                identSymbol.setAddress(alloc);
                finalCodes.add("sw $t0," + (space - identSymbol.getAddress()) + "($sp)");
            } else {
                // 用户变量之前一定声明过，因此没有在符号表里面搜索到说明是一个全局变量
                finalCodes.add("sw $t0," + leftValue);
            }
        } else {
            int offset = symbolTable.getIdentAddress(leftValue);
            finalCodes.add("sw $t0," + (space - offset) + "($sp)");
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
                    System.out.println("==========");
                    System.out.println(addSpace(five1));
                    System.out.println(space);
                    break;
                default:
                    break;
            }
            count += 1;
            if (five1[0].equals("4")) {
                break;
            }
        }

        blockSplit = new BlockSplit(new ArrayList<>(midCodes.subList(index, index + count)));
        blockSplit.blockSplit();
        starts = blockSplit.getStarts();
        regPool.setSpace(space);
        starts.remove(0);
        regPool.updateBlock(index, index + starts.get(0));

        space += 4; // 地址寄存器的空间
        System.out.println("func: " + five[1]);
        System.out.println("space = " + space);
        String funcName = five[1];
        finalCodes.add(funcName + ":");
        finalCodes.add("addi $sp,$sp," + (-space));
        alloc += 4;
        finalCodes.add("sw $ra," + (space - alloc) + "($sp)");
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
        String funcRExp = five[1];
        // 在中间代码生成阶段已经保证了不会出现push a[1](a[1]是一个值)的情况发生
        if (five[2].equals("")) {
            // 此时传值
            if (isDigit(funcRExp)) {
                finalCodes.add("li $t0," + funcRExp);
            } else {
                int offset = symbolTable.getIdentAddress(funcRExp);
                if (offset == -1) {
                    // 符号表里找不到，说明一定是全局变量
                    finalCodes.add("lw $t0," + funcRExp);
                } else {
                    finalCodes.add("lw $t0," + (space - offset) + "($sp)");
                }
            }
        } else {
            // 此时传地址
            // 此时的funcExp不可能是纯数字
            String offset1 = five[2];
            int offset = symbolTable.getIdentAddress(funcRExp);
            if (offset == -1) {
                finalCodes.add("la $t0," + funcRExp);
            } else {
                finalCodes.add("lw $t0," + (space - offset) + "($sp)");
            }
            if (isDigit(offset1)) {
                finalCodes.add("li $t1," + (4 * Integer.parseInt(offset1)));
            } else {
                offset = symbolTable.getIdentAddress(offset1);
                finalCodes.add("lw $t1," + (space - offset) + "($sp)");
                finalCodes.add("sll $t1,$t1,2");
            }
            finalCodes.add("add $t0,$t0,$t1");
        }
        alloc += 4;
        finalCodes.add("sw $t0," + (space - alloc) + "($sp)");
    }

    private void callFunc(String[] five) {
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
        alloc = alloc - 4 * number;
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
                    finalCodes.add("lw $t0," + funcRetExp);
                } else {
                    finalCodes.add("lw $v0," + (space - offset) + "($sp)");
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
        if (isDigit(opNumber1)) {
            finalCodes.add("li $t0," + opNumber1);
        } else {
            int offset = symbolTable.getIdentAddress(opNumber1);
            if (offset == -1) {
                finalCodes.add("lw $t0," + opNumber1);
            } else {
                finalCodes.add("lw $t0," + (space - offset) + "($sp)");
            }
        }
        finalCodes.add("beq $t0,0," + label);
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
        } else {
            IdentSymbol arrPoint = new IdentSymbol(arrName, true);
            alloc += 4;
            arrPoint.setAddress(alloc);
            finalCodes.add("addi $t0,$sp," + (space - alloc - 4 * Integer.parseInt(length)));
            finalCodes.add("sw $t0," + (space - alloc) + "($sp)");
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
        if (right.equals("RET")) {
            finalCodes.add("move $t1,$v0");
        } else {
            if (isDigit(right)) {
                finalCodes.add("li $t1," + right);
            } else {
                int offset1 = symbolTable.getIdentAddress(right);
                if (offset1 == -1) {
                    finalCodes.add("lw $t1," + right);
                } else {
                    finalCodes.add("lw $t1," + (space - offset1) + "($sp)");
                }
            }
        }
        int offset1 = symbolTable.getIdentAddress(arrName);
        if (offset1 == -1) {
            finalCodes.add("la $t0," + arrName);
        } else {
            finalCodes.add("lw $t0," + (space - offset1) + "($sp)");
        }
        if (isDigit(offset)) {
            finalCodes.add("sw $t1," + (4 * Integer.parseInt(offset)) + "($t0)");
        } else {
            offset1 = symbolTable.getIdentAddress(offset);
            if (offset1 == -1) {
                finalCodes.add("lw $t2," + offset);
            } else {
                finalCodes.add("lw $t2," + (space - symbolTable.getIdentAddress(offset)) + "($sp)");
            }
            finalCodes.add("sll $t2,$t2,2");
            finalCodes.add("add $t3,$t0,$t2");
            finalCodes.add("sw $t1,0($t3)");
        }
    }

    private void rightArr(String[] five) {
        String left = five[1];
        String arrName = five[2];
        String offset = five[3];
        int offset1 = symbolTable.getIdentAddress(arrName);
        if (offset1 == -1) {
            finalCodes.add("la $t0," + arrName);
        } else {
            finalCodes.add("lw $t0," + (space - offset1) + "($sp)");
        }
        if (isDigit(offset)) {
            finalCodes.add("lw $t1," + (4 * Integer.parseInt(offset)) + "($t0)");
        } else {
            offset1 = symbolTable.getIdentAddress(offset);
            if (offset1 == -1) {
                finalCodes.add("lw $t2," + offset);
            } else {
                finalCodes.add("lw $t2," + (space - symbolTable.getIdentAddress(offset)) + "($sp)");
            }
            finalCodes.add("sll $t2,$t2,2");
            finalCodes.add("add $t1,$t0,$t2");
            finalCodes.add("lw $t1,0($t1)");
        }
        offset1 = symbolTable.getIdentAddress(left);
        if (offset1 == -1) {
            if (left.contains("#")) {
                // 临时变量，需要新开辟空间
                IdentSymbol identSymbol = new IdentSymbol(left, false);
                symbolTable.add(identSymbol);
                alloc += 4;
                identSymbol.setAddress(alloc);
                finalCodes.add("sw $t1," + (space - identSymbol.getAddress()) + "($sp)");
            } else {
                finalCodes.add("sw $t1," + left);
            }
        } else {
            finalCodes.add("sw $t1," + offset1 + "($sp)");
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
        finalCodes.add("sw $v0," + (space - identSymbol.getAddress()) + "($sp)");
    }

    private void printf(String[] five) {
        if (five[1].equals("%d")) {
            String ident = five[2];
            if (ident.equals("RET")) {
                finalCodes.add("move $a0,$v0");
            } else {
                int offset = symbolTable.getIdentAddress(ident);
                if (offset == -1) {
                    finalCodes.add("lw $a0," + ident);
                } else {
                    finalCodes.add("lw $a0," + (space - offset) + "($sp)");
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
    }
}
