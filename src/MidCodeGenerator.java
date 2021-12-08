import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MidCodeGenerator {
    private final NonTerminalWord compUnit;
    private final SymbolTable symbolTable = new SymbolTable();
    private final ArrayList<String> midCodes = new ArrayList<>();
    private final boolean optimized;
    private int numberOfTemp = 0;
    private int numberOfOr = 0;
    private int numberOfIf = 0;
    private int numberOfLoop = 0;
    private int loopNow;

    public MidCodeGenerator(NonTerminalWord compUnit, boolean optimized) {
        this.compUnit = compUnit;
        this.optimized = optimized;
    }

    public ArrayList<String> getMidCodes() {
        return new ArrayList<>(midCodes);
    }

    private void analyse(Word word) {
        if (word instanceof NonTerminalWord) {
            NonTerminalWord nonTerminalWord = (NonTerminalWord) word;
            String type = nonTerminalWord.getType();
            switch (type) {
                case "<ConstDef>":
                    analyseConstDef(nonTerminalWord);
                    break;
                case "<VarDef>":
                    analyseVarDef(nonTerminalWord);
                    break;
                case "<FuncDef>":
                    analyseFuncDef(nonTerminalWord);
                    break;
                case "<MainFuncDef>":
                    analyseMainFuncDef(nonTerminalWord);
                    break;
                case "<Stmt>":
                    analyseStmt(nonTerminalWord);
                    break;
                case "<Exp>":
                    analyseExp(nonTerminalWord);
                    break;
                default:
                    ArrayList<Word> components = nonTerminalWord.getComponents();
                    for (Word word1 : components) {
                        analyse(word1);
                    }
                    break;
            }
        }
    }

    private Integer calculateExp(String string) {
        System.out.println(string);
        ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
        ScriptEngine scriptEngine = scriptEngineManager.getEngineByName("js");
        Integer eval;
        eval = null;
        try {
            if (scriptEngine.eval(string) instanceof Double) {
                eval = ((Double) scriptEngine.eval(string)).intValue();
            } else {
                eval = (Integer) scriptEngine.eval(string);
            }
        } catch (ScriptException e) {
            e.printStackTrace();
        }
        return eval;
    }

    public void analyse() {
        symbolTable.addNewLayer();
        ArrayList<Word> components = compUnit.getComponents();
        for (Word word : components) {
            analyse(word);
        }
        symbolTable.removeCurrentLayer();
    }

    private Integer calConstExp(NonTerminalWord constExp) {
        Integer n = calculateExp(constExp.toString());
        if (n != null) {
            return n;
        } else {
            ArrayList<Word> components = constExp.getComponents();
            NonTerminalWord addExp = (NonTerminalWord) components.get(0);
            return calConstAddExp(addExp);
        }
    }

    private Integer calConstAddExp(NonTerminalWord addExp) {
        ArrayList<Word> components = addExp.getComponents();
        NonTerminalWord mulExp = (NonTerminalWord) components.get(0);
        TerminalWord op;
        Integer n = calConstMulExp(mulExp);
        Integer n1;
        for (int i = 0; i < (components.size() - 1) / 2; i += 1) {
            op = (TerminalWord) components.get(2 * i + 1);
            mulExp = (NonTerminalWord) components.get(2 * i + 2);
            n1 = calConstMulExp(mulExp);
            if (op.getWordName().equals("+")) {
                n = n + n1;
            } else {
                n = n - n1;
            }
        }
        return n;
    }

    private Integer calConstMulExp(NonTerminalWord mulExp) {
        ArrayList<Word> components = mulExp.getComponents();
        NonTerminalWord unaryExp = (NonTerminalWord) components.get(0);
        TerminalWord op;
        Integer n = calConstUnaryExp(unaryExp);
        Integer n1;
        for (int i = 0; i < (components.size() - 1) / 2; i += 1) {
            op = (TerminalWord) components.get(2 * i + 1);
            unaryExp = (NonTerminalWord) components.get(2 * i + 2);
            n1 = calConstUnaryExp(unaryExp);
            if (op.getWordName().equals("*")) {
                n = n * n1;
            } else if (op.getWordName().equals("/")) {
                n = n / n1;
            } else {
                n = n % n1;
            }
        }
        return n;
    }

    private Integer calConstUnaryExp(NonTerminalWord unaryExp) {
        // 不存在函数调用的情况
        ArrayList<Word> components = unaryExp.getComponents();
        NonTerminalWord first = (NonTerminalWord) components.get(0);
        if (first.getType().equals("<PrimaryExp>")) {
            return calConstPrimaryExp(first);
        } else {
            NonTerminalWord unaryExp1 = (NonTerminalWord) components.get(1);
            Integer n = calConstUnaryExp(unaryExp1);
            TerminalWord op = (TerminalWord) first.getComponents().get(0);
            if (op.getWordName().equals("+")) {
                return n;
            } else {
                return -n;
            }
        }
    }

    private Integer calConstPrimaryExp(NonTerminalWord primaryExp) {
        ArrayList<Word> components = primaryExp.getComponents();
        if (components.get(0) instanceof TerminalWord) {
            NonTerminalWord exp = (NonTerminalWord) components.get(1);
            return calConstExp(exp);
        } else {
            NonTerminalWord first = (NonTerminalWord) components.get(0);
            if (first.getType().equals("<LVal>")) {
                return calConstLVal(first);
            } else {
                TerminalWord number = (TerminalWord) first.getComponents().get(0);
                return Integer.parseInt(number.getWordName());
            }
        }
    }

    private Integer calConstLVal(NonTerminalWord lVal) {
        ArrayList<Word> components = lVal.getComponents();
        TerminalWord ident = (TerminalWord) components.get(0);
        IdentSymbol identSymbol = symbolTable.searchIdentInAllLayers(ident.getWordName());
        if (identSymbol.numberOfDimensions() == 0) {
            return identSymbol.getValue();
        } else if (identSymbol.numberOfDimensions() == 1) {
            NonTerminalWord exp1 = (NonTerminalWord) components.get(2);
            int i = calConstExp(exp1);
            return identSymbol.getValue(i);
        } else {
            NonTerminalWord exp1 = (NonTerminalWord) components.get(2);
            NonTerminalWord exp2 = (NonTerminalWord) components.get(5);
            int i = calConstExp(exp1);
            int j = calConstExp(exp2);
            return identSymbol.getValue(i, j);
        }
    }


    private void analyseConstDef(NonTerminalWord constDef) {
        ArrayList<Word> components = constDef.getComponents();
        TerminalWord ident = (TerminalWord) components.get(0);
        NonTerminalWord constInitVal = (NonTerminalWord) components.get(components.size() - 1);
        int dimensions = (components.size() - 3) / 3;  //确定维数
        IdentSymbol identSymbol = new IdentSymbol(ident.getWordName(), true);
        if (dimensions == 0) {
            //普通变量
            midCodes.add("const int " + ident.getWordName());
            NonTerminalWord constExp = (NonTerminalWord) constInitVal.getComponents().get(0);
            Integer value = calConstExp(constExp);
            identSymbol.restoreValue(value);
            midCodes.add(ident.getWordName() + " = " + value);
        } else {
            NonTerminalWord constExp1 = (NonTerminalWord) components.get(2);
            int d1 = calConstExp(constExp1);  // 第一维
            identSymbol.add(d1);
            int i;
            int j;
            if (dimensions == 1) {
                //一维数组
                midCodes.add("arr int " + ident.getWordName() + "[" + d1 + "]");
                i = 0;
                for (Word word : constInitVal.getComponents()) {
                    if (word instanceof NonTerminalWord) {
                        NonTerminalWord constExp =
                                (NonTerminalWord) ((NonTerminalWord) word).getComponents().get(0);
                        Integer value = calConstExp(constExp);
                        identSymbol.restoreValue(value, i);
                        midCodes.add(ident.getWordName() + "[" + i + "]" + " = " + value);
                        i += 1;
                    }
                    if (i == d1) {
                        break;
                    }
                }

            } else {
                NonTerminalWord constExp2 = (NonTerminalWord) components.get(5);
                int d2 = calConstExp(constExp2);
                identSymbol.add(d2);
                midCodes.add("arr int " + ident.getWordName() + "[" + (d1 * d2) + "]");
                i = 0;
                for (Word word : constInitVal.getComponents()) {
                    if (word instanceof NonTerminalWord) {
                        NonTerminalWord constInitVal1 = (NonTerminalWord) word;
                        j = 0;
                        for (Word word1 : constInitVal1.getComponents()) {
                            if (word1 instanceof NonTerminalWord) {
                                NonTerminalWord constExp =
                                        (NonTerminalWord) ((NonTerminalWord) word1).getComponents().get(0);
                                Integer value = calConstExp(constExp);
                                identSymbol.restoreValue(value, i, j);
                                midCodes.add(ident.getWordName() + "[" + (i * d2 + j) + "]" + " = " + value);
                                j += 1;
                            }
                            if (j == d2) {
                                i += 1;
                                break;
                            }
                        }
                    }
                    if (i == d1) {
                        break;
                    }
                }
            }
        }
        symbolTable.add(identSymbol);
    }


    private void analyseVarDef(NonTerminalWord varDef) {
        ArrayList<Word> components = varDef.getComponents();
        TerminalWord ident = (TerminalWord) components.get(0);
        IdentSymbol identSymbol = new IdentSymbol(ident.getWordName(), false);
        boolean initial = !(components.get(components.size() - 1) instanceof TerminalWord);
        //有初始化
        int dimensions;
        if (initial) {
            dimensions = (components.size() - 3) / 3;
        } else {
            dimensions = (components.size() - 1) / 3;
        }
        if (dimensions == 0) {
            midCodes.add("var int " + ident.getWordName());
            if (initial) {
                NonTerminalWord initVal = (NonTerminalWord) components.get(components.size() - 1);
                NonTerminalWord exp = (NonTerminalWord) initVal.getComponents().get(0);
                String s = analyseExp(exp);
                midCodes.add(ident.getWordName() + " = " + s);
            }
        } else {
            NonTerminalWord constExp1 = (NonTerminalWord) components.get(2);
            int d1 = calConstExp(constExp1);
            identSymbol.add(d1);
            int i;
            int j;
            if (dimensions == 1) {
                midCodes.add("arr int " + ident.getWordName() + "[" + d1 + "]");
                if (initial) {
                    NonTerminalWord initVal = (NonTerminalWord) components.get(components.size() - 1);
                    i = 0;
                    for (Word word : initVal.getComponents()) {
                        if (word instanceof NonTerminalWord) {
                            NonTerminalWord exp =
                                    (NonTerminalWord) ((NonTerminalWord) word).getComponents().get(0);
                            String s = analyseExp(exp);
                            midCodes.add(ident.getWordName() + "[" + i + "]" + " = " + s);
                            i += 1;

                        }
                        if (i == d1) {
                            break;
                        }
                    }
                }
            } else {
                NonTerminalWord constExp2 = (NonTerminalWord) components.get(5);
                int d2 = calConstExp(constExp2);
                identSymbol.add(d2);
                midCodes.add("arr int " + ident.getWordName() + "[" + (d1 * d2) + "]");
                if (initial) {
                    NonTerminalWord initVal = (NonTerminalWord) components.get(components.size() - 1);
                    i = 0;
                    for (Word word : initVal.getComponents()) {
                        if (word instanceof NonTerminalWord) {
                            NonTerminalWord initVal1 = (NonTerminalWord) word;
                            j = 0;
                            for (Word word1 : initVal1.getComponents()) {
                                if (word1 instanceof NonTerminalWord) {
                                    NonTerminalWord exp =
                                            (NonTerminalWord) ((NonTerminalWord) word1).getComponents().get(0);
                                    String s = analyseExp(exp);
                                    midCodes.add(ident.getWordName() + "[" + (i * d2 + j) + "]" + " = " + s);
                                    j += 1;
                                }
                                if (j == d2) {
                                    i += 1;
                                    break;
                                }
                            }
                        }
                        if (i == d1) {
                            break;
                        }
                    }
                }
            }
        }
        symbolTable.add(identSymbol);
    }

    private void analyseFuncDef(NonTerminalWord funcDef) {
        ArrayList<Word> components = funcDef.getComponents();
        NonTerminalWord funcType = (NonTerminalWord) components.get(0);
        TerminalWord ident = (TerminalWord) components.get(1);
        FuncSymbol funcSymbol
                = new FuncSymbol(ident.getWordName(), ((TerminalWord) funcType.getComponents().get(0)).getWordName());
        ArrayList<IdentSymbol> identSymbols;
        if (components.get(3) instanceof NonTerminalWord) {
            NonTerminalWord funcFParams = (NonTerminalWord) components.get(3);
            identSymbols = analyseFuncFParams(funcFParams);
        } else {
            identSymbols = new ArrayList<>();
        }
        for (IdentSymbol identSymbol : identSymbols) {
            funcSymbol.add(identSymbol);
        }
        symbolTable.add(funcSymbol);
        midCodes.add(funcSymbol.getReturnType() + " " + funcSymbol.getName() + "()");
        symbolTable.addNewLayer();
        for (IdentSymbol identSymbol : identSymbols) {
            symbolTable.add(identSymbol);
            midCodes.add("para int " + identSymbol.getName());
        }
        midCodes.add("@func_begin " + funcSymbol.getName());
        NonTerminalWord block = (NonTerminalWord) components.get(components.size() - 1);
        analyse(block);
        symbolTable.removeCurrentLayer();
        midCodes.add("@func_end " + funcSymbol.getName());
    }

    private ArrayList<IdentSymbol> analyseFuncFParams(NonTerminalWord funcFParams) {
        ArrayList<Word> components = funcFParams.getComponents();
        ArrayList<IdentSymbol> identSymbols = new ArrayList<>();
        for (Word word : components) {
            if (word instanceof NonTerminalWord) {
                NonTerminalWord funcFParam = (NonTerminalWord) word;
                IdentSymbol identSymbol = analyseFuncFParam(funcFParam);
                identSymbols.add(identSymbol);
            }
        }
        return identSymbols;
    }

    private IdentSymbol analyseFuncFParam(NonTerminalWord funcFParam) {
        ArrayList<Word> components = funcFParam.getComponents();
        TerminalWord ident = (TerminalWord) components.get(1);
        IdentSymbol identSymbol = new IdentSymbol(ident.getWordName(), false);
        int length = components.size();
        if (length == 4) {
            // 一维数组
            identSymbol.add(0);
        }
        if (length == 7) {
            // 二维数组
            identSymbol.add(0);
            NonTerminalWord constExp = (NonTerminalWord) components.get(5);
            int d2 = calConstExp(constExp);
            identSymbol.add(d2);
        }
        return identSymbol;
    }

    private void analyseMainFuncDef(NonTerminalWord mainFuncDef) {
        ArrayList<Word> components = mainFuncDef.getComponents();
        FuncSymbol funcSymbol = new FuncSymbol("main", "int");
        symbolTable.add(funcSymbol);
        midCodes.add("int main()");
        midCodes.add("@func_begin main");
        NonTerminalWord block = (NonTerminalWord) components.get(4);
        symbolTable.addNewLayer();
        analyse(block);
        symbolTable.removeCurrentLayer();
    }

    private void analyseStmt(NonTerminalWord stmt) {
        ArrayList<Word> components = stmt.getComponents();
        int length = components.size();
        if (components.get(0) instanceof TerminalWord) {
            TerminalWord terminalWord = (TerminalWord) components.get(0);
            switch (terminalWord.getWordName()) {
                case "if":
                    analyseIfStmt(stmt);
                    break;
                case "while":
                    analyseWhileStmt(stmt);
                    break;
                case "break":
                    String loopEnd = "loop_end" + loopNow;
                    midCodes.add("goto " + loopEnd);
                    break;
                case "continue":
                    String loopBegin = "loop_begin" + loopNow;
                    midCodes.add("goto " + loopBegin);
                    break;
                case "return":
                    if (length == 3) {
                        //有Exp
                        NonTerminalWord exp = (NonTerminalWord) components.get(1);
                        String temp = analyseExp(exp);
                        midCodes.add("ret " + temp);
                    } else {
                        //无Exp
                        midCodes.add("ret");
                    }
                    break;
                case "printf":
                    analysePrintfStmt(stmt);
                    break;
            }
        } else {
            NonTerminalWord nonTerminalWord = (NonTerminalWord) components.get(0);
            if (nonTerminalWord.getType().equals("<LVal>")) {
                analyseGetIntStmt(stmt);
            } else if (nonTerminalWord.getType().equals("<Exp>")) {
                //Exp
                analyseExp(nonTerminalWord);
            } else {
                // Block
                symbolTable.addNewLayer();
                midCodes.add("@block_begin");
                analyse(nonTerminalWord);
                midCodes.add("@block_end");
                symbolTable.removeCurrentLayer();
            }
        }
    }

    private void analyseIfStmt(NonTerminalWord stmt) {
        ArrayList<NonTerminalWord> arrayList = transIfStmt(stmt);
        int n = numberOfIf;
        numberOfIf += 1;
        String endLabel = "if_end" + n;
        int length = arrayList.size() / 2; // cond的数目
        if (arrayList.size() % 2 == 0) {
            //最终以else if 结尾
            for (int i = 0; i < length; i++) {
                NonTerminalWord cond = arrayList.get(2 * i);
                NonTerminalWord stmt1 = arrayList.get(2 * i + 1);
                String successLabel = "if_entry" + n + "_" + i;
                String failLabel;
                failLabel = "if_begin" + n + "_" + (i + 1);
                midCodes.add("@label " + "if_begin" + n + "_" + i);
                analyseCond(cond, successLabel, failLabel);
                midCodes.add("@label " + "if_entry" + n + "_" + i);
                analyse(stmt1);
                midCodes.add("goto " + endLabel);
            }
            midCodes.add("@label " + "if_begin" + n + "_" + length);
            midCodes.add("@label " + "if_entry" + n + "_" + length);
        } else {
            for (int i = 0; i < length; i++) {
                NonTerminalWord cond = arrayList.get(2 * i);
                NonTerminalWord stmt1 = arrayList.get(2 * i + 1);
                String successLabel = "if_entry" + n + "_" + i;
                String failLabel = "if_begin" + n + "_" + (i + 1);
                midCodes.add("@label " + "if_begin" + n + "_" + i);
                analyseCond(cond, successLabel, failLabel);
                midCodes.add("@label " + "if_entry" + n + "_" + i);
                analyse(stmt1);
                midCodes.add("goto " + endLabel);
            }
            midCodes.add("@label " + "if_begin" + n + "_" + length);
            midCodes.add("@label " + "if_entry" + n + "_" + length);
            NonTerminalWord stmt2 = arrayList.get(arrayList.size() - 1);
            analyseStmt(stmt2);
            midCodes.add("goto " + endLabel);
        }
        midCodes.add("@label " + endLabel);
    }

    private void analyseWhileStmt(NonTerminalWord stmt) {
        ArrayList<Word> components = stmt.getComponents();
        String loopBegin = "loop_begin" + numberOfLoop;
        String loopEnd = "loop_end" + numberOfLoop;
        String loopEntry = "loop_entry" + numberOfLoop;
        int preLoop = loopNow;
        loopNow = numberOfLoop;
        numberOfLoop += 1;
        NonTerminalWord cond = (NonTerminalWord) components.get(2);
        NonTerminalWord stmt1 = (NonTerminalWord) components.get(4);
        midCodes.add("@label " + loopBegin);
        analyseCond(cond, loopEntry, loopEnd);
        midCodes.add("@label " + loopEntry);
        analyseStmt(stmt1);
        midCodes.add("goto " + loopBegin);
        midCodes.add("@label " + loopEnd);
        loopNow = preLoop;
    }

    private void analysePrintfStmt(NonTerminalWord stmt) {
        ArrayList<Word> components = stmt.getComponents();
        int length = components.size();
        int numberOfExps = (length - 4) / 2;
        TerminalWord formatString = (TerminalWord) components.get(2);
        String string =
                formatString.getWordName().substring(1, formatString.getWordName().length() - 1);
        ArrayList<String> ss = new ArrayList<>();
        int last = 0;
        for (int i = 0; i < string.length(); i++) {
            if (string.charAt(i) == '%') {
                ss.add(string.substring(last, i));
                last = i + 2;
                i += 1;
            }
        }
        if (last < string.length()) {
            ss.add(string.substring(last));
        }
        for (int i = 0; i < numberOfExps; i++) {
            if (!ss.get(i).equals("")) {
                midCodes.add("@printf " + ss.get(i).replace(" ", "#"));
            }
            NonTerminalWord exp1 = (NonTerminalWord) components.get(4 + 2 * i);
            String temp = analyseExp(exp1);
            Pattern pattern = Pattern.compile("^[-+]?[\\d]*$");
            Matcher matcher = pattern.matcher(temp);
            if (matcher.matches()) {
                midCodes.add("@printf " + temp);
            } else {
                midCodes.add("@printf" + " %d " + temp);
            }
        }
        if (ss.size() > numberOfExps) {
            midCodes.add("@printf " + ss.get(ss.size() - 1).replace(" ", "#"));
        }
    }

    private void analyseGetIntStmt(NonTerminalWord stmt) {
        ArrayList<Word> components = stmt.getComponents();
        int length = components.size();
        NonTerminalWord lVal = (NonTerminalWord) components.get(0);
        if (length == 4) {
            NonTerminalWord exp = (NonTerminalWord) components.get(2);
            String tempExp = analyseExp(exp);
            String tempLVal = analyseLVal(lVal);
            midCodes.add(tempLVal + " = " + tempExp);
        } else {
            String tempLVal = analyseLVal(lVal);
            midCodes.add("@getint " + "#t" + numberOfTemp);
            midCodes.add(tempLVal + " = " + "#t" + numberOfTemp);
            numberOfTemp += 1;
        }
    }

    private ArrayList<NonTerminalWord> transIfStmt(NonTerminalWord stmt) {
        ArrayList<NonTerminalWord> arrayList = new ArrayList<>();
        ArrayList<Word> components = stmt.getComponents();
        if (components.get(0) instanceof TerminalWord
                && ((TerminalWord) components.get(0)).getWordName().equals("if")) {
            int length = components.size();
            NonTerminalWord cond1 = (NonTerminalWord) components.get(2);
            NonTerminalWord stmt1 = (NonTerminalWord) components.get(4);
            arrayList.add(cond1);
            ArrayList<NonTerminalWord> arrayList1 = transIfStmt(stmt1);
            arrayList.addAll(arrayList1);
            if (length != 5) {
                NonTerminalWord stmt2 = (NonTerminalWord) components.get(6);
                arrayList1 = transIfStmt(stmt2);
                arrayList.addAll(arrayList1);
            }
        } else {
            arrayList.add(stmt);
        }
        return arrayList;
    }

    private String analyseExp(NonTerminalWord exp) {
        ArrayList<Word> components = exp.getComponents();
        NonTerminalWord addExp = (NonTerminalWord) components.get(0);
        return analyseAddExp(addExp);
    }

    private String analyseAddExp(NonTerminalWord addExp) {
        ArrayList<Word> components = addExp.getComponents();
        String returnTemp;
        String returnTemp1;
        NonTerminalWord mulExp = (NonTerminalWord) components.get(0);
        String temp = analyseMulExp(mulExp);
        returnTemp = temp;
        if (components.size() == 1) {
            // 如果只有一项，那么不需要生成等式
            return returnTemp;
        }
        for (int i = 0; i < (components.size() - 1) / 2; i++) {
            TerminalWord op = (TerminalWord) components.get(2 * i + 1);
            mulExp = (NonTerminalWord) components.get(2 * i + 2);
            returnTemp1 = returnTemp;
            temp = analyseMulExp(mulExp);
            returnTemp = "#t" + numberOfTemp;
            numberOfTemp += 1;
            midCodes.add(returnTemp + " = " + returnTemp1 + " " + op.getWordName() + " " + temp);
        }
        return returnTemp;
    }

    private String analyseMulExp(NonTerminalWord mulExp) {
        ArrayList<Word> components = mulExp.getComponents();
        String returnTemp;
        String returnTemp1;
        NonTerminalWord unaryExp = (NonTerminalWord) components.get(0);
        String temp = analyseUnaryExp(unaryExp);
        returnTemp = temp;
        if (components.size() == 1) {
            // 如果只有一项，那么不需要生成等式
            return temp;
        }
        for (int i = 0; i < (components.size() - 1) / 2; i++) {
            TerminalWord op = (TerminalWord) components.get(2 * i + 1);
            unaryExp = (NonTerminalWord) components.get(2 * i + 2);
            returnTemp1 = returnTemp;
            temp = analyseUnaryExp(unaryExp);
            returnTemp = "#t" + numberOfTemp;
            numberOfTemp += 1;
            midCodes.add(returnTemp + " = " + returnTemp1 + " " + op.getWordName() + " " + temp);
        }
        return returnTemp;
    }

    private String analyseUnaryExp(NonTerminalWord unaryExp) {
        ArrayList<Word> components = unaryExp.getComponents();
        if (components.get(0) instanceof TerminalWord) {
            //函数调用
            TerminalWord ident = (TerminalWord) components.get(0);
            int length = components.size();
            if (length == 4) {
                NonTerminalWord funcRParams = (NonTerminalWord) components.get(2);
                analyseFuncRParams(ident.getWordName(), funcRParams);
            }
            midCodes.add("call " + ident.getWordName());
            midCodes.add("#t" + numberOfTemp + " = RET");
            numberOfTemp += 1;
            return "#t" + (numberOfTemp - 1);
        } else {
            NonTerminalWord first = (NonTerminalWord) components.get(0);
            if (first.getType().equals("<UnaryOp>")) {
                TerminalWord op = (TerminalWord) first.getComponents().get(0);
                String temp;
                NonTerminalWord unaryExp1 = (NonTerminalWord) components.get(1);
                temp = analyseUnaryExp(unaryExp1);
                String returnTemp = "#t" + numberOfTemp;
                numberOfTemp += 1;
                midCodes.add(returnTemp + " = " + op.getWordName() + " " + temp);
                return returnTemp;
            } else {
                return analysePrimaryExp(first);
            }
        }
    }

    private void analyseFuncRParams(String funcName, NonTerminalWord funcRParams) {
        ArrayList<Word> components = funcRParams.getComponents();
        FuncSymbol funcSymbol = symbolTable.searchFunc(funcName);
        ArrayList<IdentSymbol> paras = funcSymbol.getParameters();
        ArrayList<NonTerminalWord> exps = new ArrayList<>();
        for (int i = 0; i < components.size(); i += 2) {
            exps.add((NonTerminalWord) components.get(i));
        }
        ArrayList<String> arrayList1 = new ArrayList<>(); // 存放push前的表达式
        ArrayList<String> arrayList2 = new ArrayList<>(); // 存放直接push的表达式
        for (int i = 0; i < paras.size(); i++) {
            // 针对每一个参数进行分析
            IdentSymbol identSymbol = paras.get(i);
            NonTerminalWord exp = exps.get(i);
            if (identSymbol.numberOfDimensions() == 0) {
                // 传进去普通变量的值
                String temp = analyseExp(exp);
                Pattern pattern = Pattern.compile("^[-+]?[\\d]*$");
                Matcher matcher = pattern.matcher(temp);
                if (matcher.matches()) {
                    // 纯数字
                    arrayList1.add("#t" + numberOfTemp + " = " + temp);
                    arrayList2.add("push " + "#t" + numberOfTemp);
                    numberOfTemp += 1;
                } else {
                    arrayList2.add("push " + temp);
                }
            } else if (identSymbol.numberOfDimensions() == 1) {
                // 传入一维数组,在这种情况下只可能是a[xxx](a是二维数组)或者a(a是一维数组),这时可能需要计算[xxx]
                // 用push带[]表示传入的是地址
                ArrayList<Word> arrayList = analyseArray(exp);
                TerminalWord ident = (TerminalWord) arrayList.get(0);
                if (arrayList.size() == 1) {
                    arrayList2.add("push " + ident.getWordName() + "[0]");
                } else {
                    NonTerminalWord exp1 = (NonTerminalWord) arrayList.get(1);
                    String temp = analyseExp(exp1);
                    IdentSymbol identSymbol1 = symbolTable.searchIdentInAllLayers(ident.getWordName());
                    int d2 = identSymbol1.dimension(1);
                    arrayList1.add("#t" + numberOfTemp + " = " + d2 + " * " + temp);
                    arrayList2.add("push " + ident.getWordName() + "[" + "#t" + numberOfTemp + "]");
                    numberOfTemp += 1;
                }
            } else {
                // 传入二维数组,在这种情况下一定是a(a是二维数组),因此只需要找出ident就行了
                // 用push带[]表示传入的是地址
                ArrayList<Word> arrayList = analyseArray(exp);
                TerminalWord ident = (TerminalWord) arrayList.get(0);
                arrayList2.add("push " + ident.getWordName() + "[0]");
            }
        }
        midCodes.addAll(arrayList1);
        midCodes.addAll(arrayList2);
    }

    private ArrayList<Word> analyseArray(NonTerminalWord exp) {
        // 返回数组所在的lVal的components(不包括[])
        NonTerminalWord addExp = (NonTerminalWord) exp.getComponents().get(0);
        NonTerminalWord mulExp = (NonTerminalWord) addExp.getComponents().get(0);
        NonTerminalWord unaryExp = (NonTerminalWord) mulExp.getComponents().get(0);
        NonTerminalWord primaryExp = (NonTerminalWord) unaryExp.getComponents().get(0);
        NonTerminalWord lVal = (NonTerminalWord) primaryExp.getComponents().get(0);
        ArrayList<Word> components = lVal.getComponents();
        ArrayList<Word> arrayList = new ArrayList<>();
        TerminalWord ident = (TerminalWord) components.get(0);
        arrayList.add(ident);
        for (int i = 2; i < components.size(); i += 3) {
            arrayList.add(components.get(i));
        }
        return arrayList;
    }

    private String analysePrimaryExp(NonTerminalWord primaryExp) {
        ArrayList<Word> components = primaryExp.getComponents();
        String returnTemp;
        String temp;
        if (components.get(0) instanceof TerminalWord) {
            TerminalWord first = (TerminalWord) components.get(0);
            if (first.getWordName().equals("(")) {
                NonTerminalWord exp = (NonTerminalWord) components.get(1);
                temp = analyseExp(exp);
                returnTemp = temp;
            } else {
                returnTemp = first.getWordName();
            }
        } else {
            NonTerminalWord lVal = (NonTerminalWord) components.get(0);
            temp = analyseLVal(lVal);
            if (temp.contains("[")) {
                // 如果是数组左值，那么需要一个临时变量存储
                midCodes.add("#t" + numberOfTemp + " = " + temp);
                returnTemp = "#t" + numberOfTemp;
                numberOfTemp += 1;
            } else {
                // 不是数组变量，直接返回左值本身
                returnTemp = temp;
            }

        }
        return returnTemp;
    }

    private String analyseLVal(NonTerminalWord lVal) {
        ArrayList<Word> components = lVal.getComponents();
        String returnTemp;
        String returnTemp1;
        String temp;
        int length = components.size();
        TerminalWord ident = (TerminalWord) components.get(0);
        if (length == 1) {
            returnTemp = ident.getWordName();
        } else if (length == 4) {
            NonTerminalWord exp1 = (NonTerminalWord) components.get(2);
            temp = analyseExp(exp1);
            returnTemp = ident.getWordName() + "[" + temp + "]";
        } else {
            NonTerminalWord exp1 = (NonTerminalWord) components.get(2);
            temp = analyseExp(exp1);
            String temp2;
            NonTerminalWord exp2 = (NonTerminalWord) components.get(5);
            temp2 = analyseExp(exp2);
            IdentSymbol identSymbol = symbolTable.searchIdentInAllLayers(ident.getWordName());
            returnTemp1 = "#t" + numberOfTemp;
            numberOfTemp += 1;
            midCodes.add(returnTemp1 + " = " + identSymbol.dimension(1) + " * " + temp);
            returnTemp = "#t" + numberOfTemp;
            midCodes.add(returnTemp + " = " + returnTemp1 + " + " + temp2);
            numberOfTemp += 1;
            returnTemp = ident.getWordName() + "[" + returnTemp + "]";
        }
        return returnTemp;
    }

    private void analyseCond(NonTerminalWord cond, String successLabel, String failLabel) {
        ArrayList<Word> components = cond.getComponents();
        NonTerminalWord lOrExp = (NonTerminalWord) components.get(0);
        analyseLOrExp(lOrExp, successLabel, failLabel);
    }

    private void analyseLOrExp(NonTerminalWord lOrExp, String successLabel, String failLabel) {
        ArrayList<Word> components = lOrExp.getComponents();
        int length = (components.size() + 1) / 2; // LAndExp的个数
        String failLabel1;
        for (int i = 0; i < length; i++) {
            NonTerminalWord lAndExp = (NonTerminalWord) components.get(2 * i);
            if (i == length - 1) {
                failLabel1 = failLabel;
            } else {
                failLabel1 = "or" + numberOfOr;
                numberOfOr += 1;
            }
            analyseLAndExp(lAndExp, successLabel, failLabel1);
            if (i != length - 1) {
                midCodes.add("@label " + "or" + (numberOfOr - 1));
            }
        }
    }

    private void analyseLAndExp(NonTerminalWord lAndExp, String successLabel, String failLabel) {
        ArrayList<Word> components = lAndExp.getComponents();
        int length = (components.size() + 1) / 2; // LEqExp的个数
        for (int i = 0; i < length; i++) {
            NonTerminalWord eqExp = (NonTerminalWord) components.get(2 * i);
            String temp = analyseEqExp(eqExp);
            midCodes.add("beq " + temp + " 0 " + failLabel);
        }
        midCodes.add("goto " + successLabel);
    }

    private String analyseEqExp(NonTerminalWord eqExp) {
        ArrayList<Word> components = eqExp.getComponents();
        String returnTemp;
        String returnTemp1;
        NonTerminalWord relExp = (NonTerminalWord) components.get(0);
        String temp = analyseRelExp(relExp);
        returnTemp = temp;
        if (components.size() == 1) {
            // 如果只有一项，那么不需要生成等式
            return returnTemp;
        }
        for (int i = 0; i < (components.size() - 1) / 2; i++) {
            TerminalWord op = (TerminalWord) components.get(2 * i + 1);
            relExp = (NonTerminalWord) components.get(2 * i + 2);
            returnTemp1 = returnTemp;
            temp = analyseRelExp(relExp);
            returnTemp = "#t" + numberOfTemp;
            numberOfTemp += 1;
            midCodes.add(returnTemp + " = " + returnTemp1 + " " + op.getWordName() + " " + temp);
        }
        return returnTemp;
    }

    private String analyseRelExp(NonTerminalWord relExp) {
        ArrayList<Word> components = relExp.getComponents();
        String returnTemp;
        String returnTemp1;
        NonTerminalWord addExp = (NonTerminalWord) components.get(0);
        String temp = analyseAddExp(addExp);
        returnTemp = temp;
        if (components.size() == 1) {
            // 如果只有一项，那么不需要生成等式
            return returnTemp;
        }
        for (int i = 0; i < (components.size() - 1) / 2; i++) {
            TerminalWord op = (TerminalWord) components.get(2 * i + 1);
            relExp = (NonTerminalWord) components.get(2 * i + 2);
            returnTemp1 = returnTemp;
            temp = analyseAddExp(relExp);
            returnTemp = "#t" + numberOfTemp;
            numberOfTemp += 1;
            midCodes.add(returnTemp + " = " + returnTemp1 + " " + op.getWordName() + " " + temp);
        }
        return returnTemp;
    }
}
