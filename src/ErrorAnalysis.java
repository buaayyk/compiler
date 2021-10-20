import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.HashSet;

public class ErrorAnalysis {
    private final NonTerminalWord compUnit;
    private final ErrorArrayList errorArrayList;
    private final SymbolTable symbolTable = new SymbolTable();
    private boolean hasReturn = false;
    private int inWhile = 0;

    public ErrorAnalysis(NonTerminalWord compUnit, ErrorArrayList errorArrayList) {
        this.compUnit = compUnit;
        this.errorArrayList = errorArrayList;
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
        checkCompUnit(compUnit);
    }

    private void analyse(Word word) {
        if (word instanceof NonTerminalWord) {
            NonTerminalWord nonTerminalWord = (NonTerminalWord) word;
            String type = nonTerminalWord.getType();
            switch (type) {
                case "<ConstDef>":
                    checkConstDef(nonTerminalWord);
                    break;
                case "<VarDef>":
                    checkVarDef(nonTerminalWord);
                    break;
                case "<MainFuncDef>":
                    checkMainFuncDef(nonTerminalWord);
                    break;
                case "<FuncDef>":
                    checkFuncDef(nonTerminalWord);
                    break;
                case "<LVal>":
                    checkLVal(nonTerminalWord);
                    break;
                case "<UnaryExp>":
                    checkUnaryExp(nonTerminalWord);
                    break;
                case "<Stmt>":
                    checkStmt(nonTerminalWord);
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

    private void checkCompUnit(NonTerminalWord compUnit) {
        symbolTable.addNewLayer();
        ArrayList<Word> components = compUnit.getComponents();
        for (Word word : components) {
            analyse(word);
        }
        symbolTable.removeCurrentLayer();
    }

    private void checkMainFuncDef(NonTerminalWord mainFuncDef) {
        ArrayList<Word> components = mainFuncDef.getComponents();
        FuncSymbol funcSymbol = new FuncSymbol("main", "int");
        symbolTable.add(funcSymbol);
        NonTerminalWord block = (NonTerminalWord) components.get(4);
        symbolTable.addNewLayer();
        hasReturn = false;
        analyse(block);
        if (!hasReturn) {
            TerminalWord terminalWord = getLastTerminalWord(block);
            errorArrayList.add(terminalWord.getCol(), terminalWord.getRow(), "g");
        }
        hasReturn = true;
        symbolTable.removeCurrentLayer();
    }

    private void checkConstDef(NonTerminalWord constDef) {
        ArrayList<Word> components = constDef.getComponents();
        int length = components.size();
        int numberOfDimensions = (length - 3) / 3; // 计算变量的维数
        TerminalWord ident = (TerminalWord) components.get(0);
        String name = ident.getWordName();
        IdentSymbol identSymbol = symbolTable.searchIdentInCurrentLayer(name);
        if (identSymbol != null) {
            errorArrayList.add(ident.getCol(), ident.getRow(), "b");
        }
        identSymbol = new IdentSymbol(name, true);
        //TODO 目前只关心维数，每一维具体是什么暂时搁置
        for (int i = 0; i < numberOfDimensions; i++) {
            NonTerminalWord constExp = (NonTerminalWord) components.get(2 + 3 * i);
            analyse(constExp);
            Integer dimension = calculateExp(constExp.toString());
            identSymbol.add(dimension);
        }
        NonTerminalWord lastWord = (NonTerminalWord) components.get(components.size() - 1);
        analyse(lastWord);
        symbolTable.add(identSymbol);
    }

    private void checkVarDef(NonTerminalWord varDef) {
        ArrayList<Word> components = varDef.getComponents();
        Word lastWord = components.get(components.size() - 1);
        int length = components.size();
        int numberOfDimensions;
        // 区分有没有初始化
        if (lastWord instanceof NonTerminalWord) {
            numberOfDimensions = (length - 3) / 3;
        } else {
            numberOfDimensions = (length - 1) / 3;
        }
        TerminalWord ident = (TerminalWord) components.get(0);
        String name = ident.getWordName();
        IdentSymbol identSymbol = symbolTable.searchIdentInCurrentLayer(name);
        if (identSymbol != null) {
            errorArrayList.add(ident.getCol(), ident.getRow(), "b");
        }
        identSymbol = new IdentSymbol(name, false);
        //TODO 目前只关心维数，每一维具体是什么暂时搁置
        for (int i = 0; i < numberOfDimensions; i++) {
            NonTerminalWord constExp = (NonTerminalWord) components.get(2 + 3 * i);
            analyse(constExp);
            Integer dimension = calculateExp(constExp.toString());
            identSymbol.add(dimension);
        }
        if (lastWord instanceof NonTerminalWord) {
            analyse(lastWord);
        }
        symbolTable.add(identSymbol);
    }

    private void checkFuncDef(NonTerminalWord funcDef) {
        ArrayList<Word> components = funcDef.getComponents();
        NonTerminalWord funcType = (NonTerminalWord) components.get(0);
        String returnTypeName = ((TerminalWord) funcType.getComponents().get(0)).getWordName();
        TerminalWord ident = (TerminalWord) components.get(1);
        String funcName = ident.getWordName();

        // 判断函数是否有参数
        ArrayList<IdentSymbol> parameterSymbols;
        if (components.get(3) instanceof NonTerminalWord) {
            parameterSymbols = checkFuncFParams((NonTerminalWord) components.get(3));
        } else {
            parameterSymbols = new ArrayList<>();
        }
        FuncSymbol funcSymbol = symbolTable.searchFunc(funcName);
        //查看是否有同名函数
        if (funcSymbol != null) {
            errorArrayList.add(ident.getCol(), ident.getRow(), "b");
        }
        funcSymbol = new FuncSymbol(funcName, returnTypeName);
        for (IdentSymbol parameterSymbol : parameterSymbols) {
            funcSymbol.add(parameterSymbol);
        }
        symbolTable.add(funcSymbol);
        //参数压栈
        symbolTable.addNewLayer();
        for (IdentSymbol parameterSymbol : parameterSymbols) {
            symbolTable.add(parameterSymbol);
        }
        NonTerminalWord block = (NonTerminalWord) components.get(components.size() - 1);
        hasReturn = false;
        analyse(block);
        if (returnTypeName.equals("int") && !hasReturn) {
            TerminalWord terminalWord = getLastTerminalWord(block);
            errorArrayList.add(terminalWord.getCol(), terminalWord.getRow(), "g");
        }
        hasReturn = true;
        symbolTable.removeCurrentLayer();
    }

    private TerminalWord getLastTerminalWord(NonTerminalWord nonTerminalWord) {
        ArrayList<Word> components = nonTerminalWord.getComponents();
        Word word = components.get(components.size() - 1);
        if (word instanceof TerminalWord) {
            return (TerminalWord) word;
        } else {
            return getLastTerminalWord((NonTerminalWord) word);
        }
    }

    private ArrayList<IdentSymbol> checkFuncFParams(NonTerminalWord funcFParams) {
        ArrayList<NonTerminalWord> parameters = new ArrayList<>();
        for (int i = 0; i < funcFParams.getComponents().size(); i += 2) {
            parameters.add((NonTerminalWord) funcFParams.getComponents().get(i));
        }
        // 参数的属性
        ArrayList<IdentSymbol> parameterSymbols = new ArrayList<>();
        HashSet<String> parameterNames = new HashSet<>();
        for (NonTerminalWord parameter : parameters) {
            ArrayList<Word> parameterComponents = parameter.getComponents();
            TerminalWord ident = (TerminalWord) parameterComponents.get(1);
            String parameterName = ident.getWordName();
            if (parameterNames.contains(parameterName)) {
                errorArrayList.add(ident.getCol(), ident.getRow(), "b");
            } else {
                parameterNames.add(parameterName);
            }
            IdentSymbol parameterSymbol = new IdentSymbol(parameterName, false);
            int length = parameterComponents.size();
            int numberOfDimensions = 0;
            if (length == 2) {
                numberOfDimensions = 0; // 0维
            } else if (length == 4) {
                numberOfDimensions = 1; // 1维
                parameterSymbol.add(0);
            } else if (length == 7) {
                numberOfDimensions = 2; // 2维
                parameterSymbol.add(0);
                NonTerminalWord constExp = (NonTerminalWord) parameterComponents.get(5);
                Integer dimension = calculateExp(constExp.toString());
                parameterSymbol.add(dimension);
            }
            parameterSymbols.add(parameterSymbol);
        }
        return parameterSymbols;
    }

    private void checkLVal(NonTerminalWord lVal) {
        ArrayList<Word> components = lVal.getComponents();
        TerminalWord ident = (TerminalWord) components.get(0);
        String name = ident.getWordName();
        IdentSymbol identSymbol = symbolTable.searchIdentInAllLayers(name);
        if (identSymbol == null) {
            errorArrayList.add(ident.getCol(), ident.getRow(), "c");
        }
        for (int i = 1; i < components.size(); i++) {
            analyse(components.get(i));
        }
    }

    private void checkUnaryExp(NonTerminalWord unaryExp) {
        ArrayList<Word> components = unaryExp.getComponents();
        // 这个表达式是否是函数
        if (components.get(0) instanceof TerminalWord) {
            TerminalWord ident = (TerminalWord) components.get(0);
            String funcName = ident.getWordName();
            FuncSymbol funcSymbol = symbolTable.searchFunc(funcName);
            if (funcSymbol == null) {
                errorArrayList.add(ident.getCol(), ident.getRow(), "c");
            } else {
                int rightNumberOfParameters = funcSymbol.numberOfParameters();
                int realNumberOfParameters;
                if (components.get(2) instanceof NonTerminalWord) {
                    NonTerminalWord funcParams = (NonTerminalWord) components.get(2);
                    realNumberOfParameters = (funcParams.getComponents().size() + 1) / 2;
                } else {
                    realNumberOfParameters = 0;
                }
                if (realNumberOfParameters != rightNumberOfParameters) {
                    errorArrayList.add(ident.getCol(), ident.getRow(), "d");
                } else {
                    if (realNumberOfParameters != 0) {
                        NonTerminalWord funcParams = (NonTerminalWord) components.get(2);
                        ArrayList<Word> paraComponents = funcParams.getComponents();
                        for (int i = 0; i < paraComponents.size(); i += 2) {
                            NonTerminalWord exp = (NonTerminalWord) paraComponents.get(i);
                            ExpType expType = getExpType(exp);
                            IdentSymbol identSymbol = funcSymbol.getParameterAttribute(i / 2);
                            // 返回为空说明表达式中存在没有定义的变量，后续讨论
                            if (expType != null) {
                                if (expType.getType().equals("void")) {
                                    errorArrayList.add(ident.getCol(), ident.getRow(), "e");
                                    break;
                                }
                                if (expType.size() != identSymbol.numberOfDimensions()) {
                                    errorArrayList.add(ident.getCol(), ident.getRow(), "e");
                                    break;
                                }
                                boolean bool = true;
                                for (int j = 1; j < expType.size(); j++) {
                                    if (!expType.dimension(j).equals(identSymbol.dimension(j))) {
                                        bool = false;
                                        break;
                                    }
                                }
                                // 某些维度的值没有对齐
                                if (!bool) {
                                    errorArrayList.add(ident.getCol(), ident.getRow(), "e");
                                    break;
                                }
                            }
                        }
                        analyse(funcParams);
                    }
                }
            }
        } else {
            for (Word component : components) {
                analyse(component);
            }
        }
    }

    private ExpType getExpType(NonTerminalWord exp) {
        ArrayList<Word> components = exp.getComponents();
        NonTerminalWord addExp = (NonTerminalWord) components.get(0);
        return getAddExpType(addExp);
    }

    private ExpType getAddExpType(NonTerminalWord addExp) {
        ArrayList<Word> components = addExp.getComponents();
        NonTerminalWord nonTerminalWord = (NonTerminalWord) components.get(0);
        return getMulExpType(nonTerminalWord);
    }

    private ExpType getMulExpType(NonTerminalWord mulExp) {
        ArrayList<Word> components = mulExp.getComponents();
        NonTerminalWord nonTerminalWord = (NonTerminalWord) components.get(0);
        return getUnaryExpType(nonTerminalWord);
    }

    private ExpType getUnaryExpType(NonTerminalWord unaryExp) {
        ArrayList<Word> components = unaryExp.getComponents();
        if (components.get(0) instanceof TerminalWord) {
            TerminalWord ident = (TerminalWord) components.get(0);
            FuncSymbol funcSymbol = symbolTable.searchFunc(ident.getWordName());
            if (funcSymbol == null) {
                return null;
            } else {
                return new ExpType(funcSymbol.getReturnType());
            }
        } else {
            NonTerminalWord nonTerminalWord = (NonTerminalWord) components.get(0);
            if (nonTerminalWord.getType().equals("<UnaryOp>")) {
                return new ExpType("int");
            } else {
                return getPrimaryExpType(nonTerminalWord);
            }
        }
    }

    private ExpType getPrimaryExpType(NonTerminalWord primaryExp) {
        ArrayList<Word> components = primaryExp.getComponents();
        if (components.get(0) instanceof TerminalWord) {
            TerminalWord terminalWord = (TerminalWord) components.get(0);
            NonTerminalWord exp = (NonTerminalWord) components.get(1);
            return getExpType(exp);
        } else {
            NonTerminalWord nonTerminalWord = (NonTerminalWord) components.get(0);
            if (nonTerminalWord.getType().equals("<Number>")) {
                return new ExpType("int");
            } else {
                return getLValType(nonTerminalWord);
            }
        }
    }

    private ExpType getLValType(NonTerminalWord lVal) {
        ArrayList<Word> components = lVal.getComponents();
        TerminalWord ident = (TerminalWord) components.get(0);
        String name = ident.getWordName();
        IdentSymbol identSymbol = symbolTable.searchIdentInAllLayers(name);
        if (identSymbol == null) {
            return null;
        } else {
            ExpType expType = new ExpType("int");
            int length = components.size();
            int numberOfDimensions = (length - 1) / 3;
            for (int i = numberOfDimensions; i < identSymbol.numberOfDimensions(); i++) {
                expType.add(identSymbol.dimension(i));
            }
            return expType;
        }
    }

    private void checkStmt(NonTerminalWord stmt) {
        ArrayList<Word> components = stmt.getComponents();
        if (components.get(0) instanceof TerminalWord) {
            TerminalWord terminalWord = (TerminalWord) components.get(0);
            if (terminalWord.getWordName().equals("return")) {
                hasReturn = true;
                FuncSymbol funcSymbol = symbolTable.searchLatestFunc();
                // 添加>2,表示一定返回了值(=2的时候是直接返回，没有值)
                if (funcSymbol.getReturnType().equals("void") && components.size() > 2) {
                    errorArrayList.add(terminalWord.getCol(), terminalWord.getRow(), "f");
                }
            }
            if (terminalWord.getWordName().equals("printf")) {
                TerminalWord formatString = (TerminalWord) components.get(2);
                String string = formatString.getWordName();
                String string1 = string.replaceAll("%d", "");
                int desiredNumber = (string.length() - string1.length()) / 2;
                int realNumber = (components.size() - 5) / 2;
                if (desiredNumber != realNumber) {
                    errorArrayList.add(terminalWord.getCol(), terminalWord.getRow(), "l");
                }
            }
            if (terminalWord.getWordName().equals("while")) {
                inWhile += 1;
            }
            if (terminalWord.getWordName().equals("break") || terminalWord.getWordName().equals("continue")) {
                if (inWhile == 0) {
                    errorArrayList.add(terminalWord.getCol(), terminalWord.getRow(), "m");
                }
            }
            for (Word word : components) {
                analyse(word);
            }
            if (terminalWord.getWordName().equals("while")) {
                inWhile -= 1;
            }
        } else {
            NonTerminalWord nonTerminalWord = (NonTerminalWord) components.get(0);
            if (nonTerminalWord.getType().equals("<LVal>")) {
                TerminalWord ident = (TerminalWord) nonTerminalWord.getComponents().get(0);
                String name = ident.getWordName();
                IdentSymbol identSymbol = symbolTable.searchIdentInAllLayers(name);
                if (identSymbol != null) {
                    if (identSymbol.isConst()) {
                        errorArrayList.add(ident.getCol(), ident.getRow(), "h");
                    }
                }
            }
            if (nonTerminalWord.getType().equals("<Block>")) {
                symbolTable.addNewLayer();
            }
            for (Word word : components) {
                analyse(word);
            }
            if (nonTerminalWord.getType().equals("<Block>")) {
                symbolTable.removeCurrentLayer();
            }
        }
    }

    private static class ExpType {
        private final String type;
        private final ArrayList<Integer> dimensions = new ArrayList<>();

        public ExpType(String type) {
            this.type = type;
        }

        public void add(Integer dimension) {
            dimensions.add(dimension);
        }

        public String getType() {
            return type;
        }

        public Integer dimension(int index) {
            return dimensions.get(index);
        }

        public int size() {
            return dimensions.size();
        }
    }

    public static void main(String[] args) {
        ArrayList<String> strings = new ArrayList<>();
        String a = "\\n";
        System.out.println(a.length());
    }
}
