import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.ArrayList;

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
        ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
        ScriptEngine scriptEngine = scriptEngineManager.getEngineByName("nashorn");
        Integer eval;
        eval = null;
        try {
            eval = (Integer) scriptEngine.eval(string);
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

    private void analyseConstDef(NonTerminalWord constDef) {
        ArrayList<Word> components = constDef.getComponents();
        TerminalWord ident = (TerminalWord) components.get(0);
        NonTerminalWord constInitVal = (NonTerminalWord) components.get(components.size() - 1);
        int dimensions = (components.size() - 3) / 3;  //确定维数
        IdentSymbol identSymbol = new IdentSymbol(ident.getWordName(), true);
        if (dimensions == 0) {
            //普通变量
            midCodes.add("const int " + ident.getWordName());
            midCodes.add(ident.getWordName() + " = " + calculateExp(constInitVal.toString()));
        } else {
            NonTerminalWord constExp1 = (NonTerminalWord) components.get(2);
            int d1 = calculateExp(constExp1.toString()); // 第一维
            identSymbol.add(d1);
            int i;
            int j;
            if (dimensions == 1) {
                //一维数组
                midCodes.add("arr int " + ident.getWordName() + " [" + d1 + "]");
                String[] expList = constInitVal.toString().split("[,{}]");
                i = 0;
                for (String exp : expList) {
                    Integer value = calculateExp(exp);
                    if (value != null) {
                        midCodes.add(ident.getWordName() + "[" + i + "]" + " = " + value);
                        i += 1;
                    }
                    if (i == d1) {
                        break;
                    }
                }
            } else {
                //二维数组
                NonTerminalWord constExp2 = (NonTerminalWord) components.get(5);
                int d2 = calculateExp(constExp2.toString()); // 第二维
                identSymbol.add(d2);
                midCodes.add("arr int " + ident.getWordName() + " [" + (d1 * d2) + "]");
                String[] expList = constInitVal.toString().split("[,{}]");
                i = j = 0;
                for (String exp : expList) {
                    Integer value = calculateExp(exp);
                    if (value != null) {
                        midCodes.add(ident.getWordName() + "[" + (i + d2 * j) + " = " + value);
                        j += 1;
                    }
                    if (j == d2) {
                        i += 1;
                        j = 0;
                        if (i == d1) {
                            break;
                        }
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
            int d1 = calculateExp(constExp1.toString());
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
                int d2 = calculateExp(constExp2.toString());
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
            midCodes.add("para int " + ident.getWordName());
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
            int d2 = calculateExp(constExp.toString());
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
                analyse(nonTerminalWord);
                symbolTable.removeCurrentLayer();
            }
        }
    }

    private void analyseIfStmt(NonTerminalWord stmt) {
        ArrayList<NonTerminalWord> arrayList = transIfStmt(stmt);
        String endLabel = "endif" + numberOfIf;
        numberOfIf += 1;
        int length = arrayList.size() / 2; // cond的数目
        if (arrayList.size() % 2 == 0) {
            //最终以else if 结尾
            for (int i = 0; i < length; i++) {
                NonTerminalWord cond = arrayList.get(2 * i);
                NonTerminalWord stmt1 = arrayList.get(2 * i + 1);
                String successLabel = "if" + numberOfIf + "_" + i + "_B";
                String failLabel;
                if (i == length - 1) {
                    failLabel = endLabel;
                } else {
                    failLabel = "if" + numberOfIf + "_" + (i + 1) + "_A";
                }
                midCodes.add("@label " + "if" + numberOfIf + "_" + i + "_A");
                analyseCond(cond, successLabel, failLabel);
                midCodes.add("@label " + "if" + numberOfIf + "_" + i + "_B");
                analyse(stmt1);
                if (i != length - 1) {
                    midCodes.add("goto " + endLabel);
                }
            }
        } else {
            for (int i = 0; i < length; i++) {
                NonTerminalWord cond = arrayList.get(2 * i);
                NonTerminalWord stmt1 = arrayList.get(2 * i + 1);
                String successLabel = "if" + numberOfIf + "_" + i + "_B";
                String failLabel = "if" + numberOfIf + "_" + (i + 1) + "_A";
                midCodes.add("@label " + "if" + numberOfIf + "_" + i + "_A");
                analyseCond(cond, successLabel, failLabel);
                midCodes.add("@label " + "if" + numberOfIf + "_" + i + "_B");
                analyse(stmt1);
            }
            midCodes.add("@label " + "if" + numberOfIf + "_" + length + "_A");
            NonTerminalWord stmt2 = arrayList.get(arrayList.size() - 1);
            analyseStmt(stmt2);
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
                i += 2;
                last = i;
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
            midCodes.add("@printf" + " %d " + temp);
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
        returnTemp = "#t" + numberOfTemp;
        numberOfTemp += 1;
        midCodes.add(returnTemp + " = " + temp);
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
        returnTemp = "#t" + numberOfTemp;
        numberOfTemp += 1;
        midCodes.add(returnTemp + " = " + temp);
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
                analyseFuncRParams(funcRParams);
            }
            midCodes.add("call " + ident.getWordName());
            return "RET";
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

    private void analyseFuncRParams(NonTerminalWord funcRParams) {
        ArrayList<Word> components = funcRParams.getComponents();
        ArrayList<String> temps = new ArrayList<>();
        for (int i = 0; i < components.size(); i += 2) {
            NonTerminalWord exp = (NonTerminalWord) components.get(i);
            String temp = analyseExp(exp);
            temps.add(temp);
        }
        for (String temp : temps) {
            midCodes.add("push " + temp);
        }
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
            midCodes.add("#t" + numberOfTemp + " = " + temp);
            returnTemp = "#t" + numberOfTemp;
            numberOfTemp += 1;
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
        returnTemp = "#t" + numberOfTemp;
        numberOfTemp += 1;
        midCodes.add(returnTemp + " = " + temp);
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
        returnTemp = "#t" + numberOfTemp;
        numberOfTemp += 1;
        midCodes.add(returnTemp + " = " + temp);
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
