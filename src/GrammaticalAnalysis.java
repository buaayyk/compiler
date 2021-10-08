import sun.util.resources.no.CurrencyNames_no_NO;

import java.util.ArrayList;

public class GrammaticalAnalysis {
    private final ArrayList<TerminalWord> terminalWords;
    private int index;
    private NonTerminalWord compUnit;

    public GrammaticalAnalysis(ArrayList<TerminalWord> terminalWords) {
        this.terminalWords = new ArrayList<>(terminalWords);
        this.index = 0;
        this.compUnit = new NonTerminalWord("<CompUnit>");
    }

    public NonTerminalWord getCompUnit() {
        return compUnit;
    }

    public void analyse() {
        NonTerminalWord nonTerminalWord = Decl();
        while (nonTerminalWord != null) {
            compUnit.add(nonTerminalWord);
            nonTerminalWord = Decl();
        }
        nonTerminalWord = FuncDef();
        while (nonTerminalWord != null) {
            compUnit.add(nonTerminalWord);
            nonTerminalWord = FuncDef();
        }
        nonTerminalWord = MainFuncDef();
        if (nonTerminalWord == null) {
            compUnit = null;
        } else {
            compUnit.add(nonTerminalWord);
        }
    }

    private NonTerminalWord Decl() {
        NonTerminalWord decl = new NonTerminalWord("<Decl>");
        NonTerminalWord nonTerminalWord;
        nonTerminalWord = ConstDecl();
        if (nonTerminalWord != null) {
            decl.add(nonTerminalWord);
            return decl;
        }
        nonTerminalWord = VarDecl();
        if (nonTerminalWord != null) {
            decl.add(nonTerminalWord);
            return decl;
        }
        return null;
    }

    private NonTerminalWord ConstDecl() {
        NonTerminalWord constDecl = new NonTerminalWord("<ConstDecl>");
        int start = index;
        TerminalWord terminalWord;
        NonTerminalWord nonTerminalWord;
        terminalWord = terminalWords.get(index);
        index += 1;
        if (!terminalWord.getWordName().equals("const") || !terminalWord.getWordType().equals("CONSTTK")) {
            index = start;
            return null;
        }
        constDecl.add(terminalWord);
        nonTerminalWord = BType();
        if (nonTerminalWord == null) {
            index = start;
            return null;
        }
        constDecl.add(nonTerminalWord);
        nonTerminalWord = ConstDef();
        if (nonTerminalWord == null) {
            index = start;
            return null;
        }
        constDecl.add(nonTerminalWord);
        terminalWord = terminalWords.get(index);
        while (terminalWord.getWordName().equals(",") && terminalWord.getWordType().equals("COMMA")) {
            index += 1;
            constDecl.add(terminalWord);
            nonTerminalWord = ConstDef();
            if (nonTerminalWord == null) {
                index = start;
                return null;
            }
            constDecl.add(nonTerminalWord);
            terminalWord = terminalWords.get(index);
        }
        terminalWord = terminalWords.get(index);
        if (!terminalWord.getWordName().equals(";") || !terminalWord.getWordType().equals("SEMICN")) {
            index = start;
            return null;
        }
        constDecl.add(terminalWord);
        return constDecl;
    }

    private NonTerminalWord BType() {
        NonTerminalWord btype = new NonTerminalWord("<Btype>");
        int start = index;
        TerminalWord terminalWord;
        terminalWord = terminalWords.get(index);
        index += 1;
        if (terminalWord == null) {
            index = start;
            return null;
        }
        btype.add(terminalWord);
        return btype;
    }

    private NonTerminalWord ConstDef() {
        NonTerminalWord constDef = new NonTerminalWord("<ConstDef>");
        int start = index;
        TerminalWord terminalWord;
        NonTerminalWord nonTerminalWord;
        terminalWord = terminalWords.get(index);
        index += 1;
        if (!terminalWord.getWordType().equals("IDENFR")) {
            index = start;
            return null;
        }
        constDef.add(terminalWord);
        terminalWord = terminalWords.get(index);
        while (terminalWord.getWordName().equals("[") && terminalWord.getWordType().equals("LBRACK")) {
            index += 1;
            constDef.add(terminalWord);
            nonTerminalWord = ConstExp();
            if (nonTerminalWord == null) {
                index = start;
                return null;
            }
            constDef.add(nonTerminalWord);
            terminalWord = terminalWords.get(index);
            index += 1;
            if (!terminalWord.getWordName().equals("]") || !terminalWord.getWordType().equals("RBRACK")) {
                index = start;
                return null;
            }
            constDef.add(terminalWord);
            terminalWord = terminalWords.get(index);
        }
        terminalWord = terminalWords.get(index);
        index += 1;
        if (!terminalWord.getWordName().equals("=") || !terminalWord.getWordType().equals("ASSIGN")) {
            index = start;
            return null;
        }
        constDef.add(terminalWord);
        nonTerminalWord = ConstInitVal();
        if (nonTerminalWord == null) {
            index = start;
            return null;
        }
        constDef.add(nonTerminalWord);
        return constDef;
    }

    private NonTerminalWord ConstInitVal() {
        NonTerminalWord constInitVal = new NonTerminalWord("<ConstInitVal>");
        int start = index;
        TerminalWord terminalWord;
        NonTerminalWord nonTerminalWord;
        terminalWord = terminalWords.get(index);
        if (terminalWord.getWordName().equals("{") && terminalWord.getWordType().equals("RBRACE")) {
            index += 1;
            constInitVal.add(terminalWord);
            nonTerminalWord = ConstInitVal();
            if (nonTerminalWord != null) {
                constInitVal.add(nonTerminalWord);
                terminalWord = terminalWords.get(index);
                while (terminalWord.getWordName().equals(",") && terminalWord.getWordType().equals("COMMA")) {
                    index += 1;
                    constInitVal.add(terminalWord);
                    nonTerminalWord = ConstInitVal();
                    if (nonTerminalWord == null) {
                        index = start;
                        return null;
                    }
                    constInitVal.add(nonTerminalWord);
                    terminalWord = terminalWords.get(index);
                }
            }
            terminalWord = terminalWords.get(index);
            index += 1;
            if (!terminalWord.getWordName().equals("}") || !terminalWord.getWordType().equals("RBRACE")) {
                index = start;
                return null;
            }
            constInitVal.add(terminalWord);
            return constInitVal;
        } else {
            nonTerminalWord = ConstExp();
            if (nonTerminalWord == null) {
                index = start;
                return null;
            }
            constInitVal.add(nonTerminalWord);
            return constInitVal;
        }
    }

    private NonTerminalWord VarDecl() {
        NonTerminalWord varDecl = new NonTerminalWord("<VarDecl>");
        int start = index;
        TerminalWord terminalWord;
        NonTerminalWord nonTerminalWord;
        nonTerminalWord = BType();
        if (nonTerminalWord == null) {
            index = start;
            return null;
        }
        varDecl.add(nonTerminalWord);
        nonTerminalWord = VarDef();
        if (nonTerminalWord == null) {
            index = start;
            return null;
        }
        varDecl.add(nonTerminalWord);
        terminalWord = terminalWords.get(index);
        while (terminalWord.getWordName().equals(",") && terminalWord.getWordType().equals("COMMA")) {
            index += 1;
            varDecl.add(terminalWord);
            nonTerminalWord = VarDef();
            if (nonTerminalWord == null) {
                index = start;
                return null;
            }
            varDecl.add(nonTerminalWord);
            terminalWord = terminalWords.get(index);
        }
        terminalWord = terminalWords.get(index);
        if (!terminalWord.getWordName().equals(";") || !terminalWord.getWordType().equals("SEMICN")) {
            index = start;
            return null;
        }
        varDecl.add(terminalWord);
        return varDecl;
    }

    private NonTerminalWord VarDef() {
        NonTerminalWord varDef = new NonTerminalWord("<VarDef>");
        int start = index;
        TerminalWord terminalWord;
        NonTerminalWord nonTerminalWord;
        terminalWord = terminalWords.get(index);
        index += 1;
        if (!terminalWord.getWordType().equals("IDENFR")) {
            index = start;
            return null;
        }
        varDef.add(terminalWord);
        terminalWord = terminalWords.get(index);
        //TODO 这里可能需要加限制--最多两个[]
        while (terminalWord.getWordName().equals("[") && terminalWord.getWordType().equals("LBRACK")) {
            index += 1;
            varDef.add(terminalWord);
            nonTerminalWord = ConstExp();
            if (nonTerminalWord == null) {
                index = start;
                return null;
            }
            varDef.add(nonTerminalWord);
            terminalWord = terminalWords.get(index);
            index += 1;
            if (!terminalWord.getWordName().equals("]") || !terminalWord.getWordType().equals("RBRACK")) {
                index = start;
                return null;
            }
            varDef.add(terminalWord);
            terminalWord = terminalWords.get(index);
        }
        int start2 = index;
        //从这里开始比较特殊，因为前面已经算是识别成功了，即使后面识别失败，也可以返回前面的成功结果
        terminalWord = terminalWords.get(index);
        index += 1;
        if (!terminalWord.getWordName().equals("=") || !terminalWord.getWordType().equals("ASSIGN")) {
            index = start2;
            return varDef;
        }
        varDef.add(terminalWord);
        nonTerminalWord = InitVal();
        if (nonTerminalWord == null) {
            index = start2;
            varDef.remove();
            return varDef;
        }
        varDef.add(nonTerminalWord);
        return varDef;
    }

    private NonTerminalWord InitVal() {
        NonTerminalWord initVal = new NonTerminalWord("<InitVal>");
        int start = index;
        TerminalWord terminalWord;
        NonTerminalWord nonTerminalWord;
        terminalWord = terminalWords.get(index);
        if (terminalWord.getWordName().equals("{") && terminalWord.getWordType().equals("RBRACE")) {
            index += 1;
            nonTerminalWord = InitVal();
            if (nonTerminalWord != null) {
                initVal.add(nonTerminalWord);
                terminalWord = terminalWords.get(index);
                while (terminalWord.getWordName().equals(",") && terminalWord.getWordType().equals("COMMA")) {
                    index += 1;
                    initVal.add(terminalWord);
                    nonTerminalWord = InitVal();
                    if (nonTerminalWord == null) {
                        index = start;
                        return null;
                    }
                    initVal.add(nonTerminalWord);
                    terminalWord = terminalWords.get(index);
                }
            }
            terminalWord = terminalWords.get(index);
            index += 1;
            if (!terminalWord.getWordName().equals("}") || !terminalWord.getWordType().equals("RBRACE")) {
                index = start;
                return null;
            }
            initVal.add(terminalWord);
            return initVal;
        } else {
            nonTerminalWord = Exp();
            if (nonTerminalWord == null) {
                index = start;
                return null;
            }
            initVal.add(nonTerminalWord);
            return initVal;
        }
    }

    private NonTerminalWord FuncDef() {
        NonTerminalWord funcDef = new NonTerminalWord("<FuncDef>");
        int start = index;
        TerminalWord terminalWord;
        NonTerminalWord nonTerminalWord;
        nonTerminalWord = FuncType();
        if (nonTerminalWord == null) {
            index = start;
            return null;
        }
        funcDef.add(nonTerminalWord);
        terminalWord = terminalWords.get(index);
        index += 1;
        if (!terminalWord.getWordType().equals("IDENFR")) {
            index = start;
            return null;
        }
        funcDef.add(terminalWord);
        terminalWord = terminalWords.get(index);
        index += 1;
        if (!terminalWord.getWordName().equals("(") || !terminalWord.getWordType().equals("LPARENT")) {
            index = start;
            return null;
        }
        funcDef.add(terminalWord);
        nonTerminalWord = FuncFParams();
        if (nonTerminalWord != null) {
            funcDef.add(nonTerminalWord);
        }
        terminalWord = terminalWords.get(index);
        index += 1;
        if (!terminalWord.getWordName().equals(")") || !terminalWord.getWordType().equals("RPARENT")) {
            index = start;
            return null;
        }
        funcDef.add(terminalWord);
        nonTerminalWord = Block();
        if (nonTerminalWord == null) {
            index = start;
            return null;
        }
        funcDef.add(nonTerminalWord);
        return funcDef;
    }

    private NonTerminalWord MainFuncDef() {
        NonTerminalWord mainFuncDef = new NonTerminalWord("<MainFuncDef>");
        int start = index;
        NonTerminalWord nonTerminalWord;
        TerminalWord terminalWord;
        terminalWord = terminalWords.get(index);
        index += 1;
        if (!terminalWord.getWordName().equals("int") || !terminalWord.getWordType().equals("INTTK")) {
            index = start;
            return null;
        }
        mainFuncDef.add(terminalWord);
        terminalWord = terminalWords.get(index);
        index += 1;
        if (!terminalWord.getWordName().equals("main") || !terminalWord.getWordType().equals("MAINTK")) {
            index = start;
            return null;
        }
        mainFuncDef.add(terminalWord);
        terminalWord = terminalWords.get(index);
        index += 1;
        if (!terminalWord.getWordName().equals("(") || !terminalWord.getWordType().equals("LPARENT")) {
            index = start;
            return null;
        }
        mainFuncDef.add(terminalWord);
        terminalWord = terminalWords.get(index);
        index += 1;
        if (!terminalWord.getWordName().equals(")") || !terminalWord.getWordType().equals("RPARENT")) {
            index = start;
            return null;
        }
        mainFuncDef.add(terminalWord);
        nonTerminalWord = Block();
        if (nonTerminalWord == null) {
            index = start;
            return null;
        }
        mainFuncDef.add(nonTerminalWord);
        return mainFuncDef;
    }

    private NonTerminalWord FuncType() {
        NonTerminalWord funcType = new NonTerminalWord("<FuncType>");
        int start = index;
        TerminalWord terminalWord;
        terminalWord = terminalWords.get(index);
        index += 1;
        if (terminalWord.getWordName().equals("void") && terminalWord.getWordType().equals("VOIDTK")) {
            funcType.add(terminalWord);
            return funcType;
        } else if (terminalWord.getWordName().equals("int") && terminalWord.getWordType().equals("INTTK")) {
            funcType.add(terminalWord);
            return funcType;
        } else {
            index = start;
            return null;
        }
    }

    private NonTerminalWord FuncFParams() {
        NonTerminalWord funcFParams = new NonTerminalWord("<FuncFParams>");
        int start = index;
        NonTerminalWord nonTerminalWord;
        TerminalWord terminalWord;
        nonTerminalWord = FuncFParam();
        if (nonTerminalWord == null) {
            index = start;
            return null;
        }
        funcFParams.add(nonTerminalWord);
        terminalWord = terminalWords.get(index);
        while (terminalWord.getWordName().equals(",") && terminalWord.getWordType().equals("COMMA")) {
            index += 1;
            funcFParams.add(terminalWord);
            nonTerminalWord = FuncFParam();
            if (nonTerminalWord == null) {
                index = start;
                return null;
            }
            funcFParams.add(nonTerminalWord);
            terminalWord = terminalWords.get(index);
        }
        return funcFParams;
    }

    private NonTerminalWord FuncFParam() {
        NonTerminalWord funcFParam = new NonTerminalWord("<FuncFParam>");
        int start = index;
        NonTerminalWord nonTerminalWord;
        TerminalWord terminalWord;
        nonTerminalWord = BType();
        if (nonTerminalWord == null) {
            index = start;
            return null;
        }
        funcFParam.add(nonTerminalWord);
        terminalWord = terminalWords.get(index);
        index += 1;
        if (!terminalWord.getWordType().equals("IDENFR")) {
            index = start;
            return null;
        }
        funcFParam.add(terminalWord);
        terminalWord = terminalWords.get(index);
        if (terminalWord.getWordName().equals("[") && terminalWord.getWordType().equals("LBRACK")) {
            index += 1;
            funcFParam.add(terminalWord);
            terminalWord = terminalWords.get(index);
            index += 1;
            if (!terminalWord.getWordName().equals("]") || !terminalWord.getWordType().equals("RBRACK")) {
                index = start;
                return null;
            }
            funcFParam.add(terminalWord);
            terminalWord = terminalWords.get(index);
            while (terminalWord.getWordName().equals("[") && terminalWord.getWordType().equals("LBRACK")) {
                index += 1;
                funcFParam.add(terminalWord);
                nonTerminalWord = ConstExp();
                if (nonTerminalWord == null) {
                    index = start;
                    return null;
                }
                funcFParam.add(nonTerminalWord);
                terminalWord = terminalWords.get(index);
                index += 1;
                if (terminalWord.getWordName().equals("]") && terminalWord.getWordType().equals("RBRACK")) {
                    index = start;
                    return null;
                }
                funcFParam.add(terminalWord);
                terminalWord = terminalWords.get(index);
            }
        }
        return funcFParam;
    }

    private NonTerminalWord Block() {
        NonTerminalWord block = new NonTerminalWord("<Block>");
        int start = index;
        NonTerminalWord nonTerminalWord;
        TerminalWord terminalWord;
        terminalWord = terminalWords.get(index);
        index += 1;
        if (!terminalWord.getWordName().equals("{") || !terminalWord.getWordType().equals("LBRACE")) {
            index = start;
            return null;
        }
        block.add(terminalWord);
        nonTerminalWord = BlockItem();
        while (nonTerminalWord != null) {
            block.add(nonTerminalWord);
            nonTerminalWord = BlockItem();
        }
        terminalWord = terminalWords.get(index);
        index += 1;
        if (!terminalWord.getWordName().equals("}") || !terminalWord.getWordType().equals("RBRACE")) {
            index = start;
            return null;
        }
        block.add(terminalWord);
        return block;
    }

    private NonTerminalWord BlockItem() {
        NonTerminalWord blockItem = new NonTerminalWord("<BlockItem>");
        NonTerminalWord nonTerminalWord;
        nonTerminalWord = Decl();
        if (nonTerminalWord != null) {
            blockItem.add(nonTerminalWord);
            return blockItem;
        }
        nonTerminalWord = Stmt();
        if (nonTerminalWord != null) {
            blockItem.add(nonTerminalWord);
            return blockItem;
        }
        return null;
    }

    private NonTerminalWord Stmt() {
        NonTerminalWord stmt = new NonTerminalWord("<Stmt>");
        int start = index;
        NonTerminalWord nonTerminalWord;
        TerminalWord terminalWord;
        terminalWord = terminalWords.get(index);
        if (terminalWord.getWordName().equals("if") && terminalWord.getWordType().equals("IFTK")) {
            index += 1;
            stmt.add(terminalWord);
            terminalWord = terminalWords.get(index);
            index += 1;
            if (!terminalWord.getWordName().equals("(") || !terminalWord.getWordType().equals("LPARENT")) {
                index = start;
                return null;
            }
            stmt.add(terminalWord);
            nonTerminalWord = Cond();
            if (nonTerminalWord == null) {
                index = start;
                return null;
            }
            stmt.add(nonTerminalWord);
            terminalWord = terminalWords.get(index);
            index += 1;
            if (!terminalWord.getWordName().equals(")") || !terminalWord.getWordType().equals("RPARENT")) {
                index = start;
                return null;
            }
            stmt.add(terminalWord);
            nonTerminalWord = Stmt();
            if (nonTerminalWord == null) {
                index = start;
                return null;
            }
            terminalWord = terminalWords.get(index);
            if (terminalWord.getWordName().equals("else") && terminalWord.getWordType().equals("ELSETK")) {
                index += 1;
                stmt.add(terminalWord);
                nonTerminalWord = Stmt();
                if (nonTerminalWord == null) {
                    index = start;
                    return null;
                }
                stmt.add(nonTerminalWord);
            }
            return stmt;
        } else if (terminalWord.getWordName().equals("while") && terminalWord.getWordType().equals("WHILETK")) {
            index += 1;
            stmt.add(terminalWord);
            terminalWord = terminalWords.get(index);
            index += 1;
            if (!terminalWord.getWordName().equals("(") || !terminalWord.getWordType().equals("LPARENT")) {
                index = start;
                return null;
            }
            stmt.add(terminalWord);
            nonTerminalWord = Cond();
            if (nonTerminalWord == null) {
                index = start;
                return null;
            }
            stmt.add(nonTerminalWord);
            terminalWord = terminalWords.get(index);
            index += 1;
            if (!terminalWord.getWordName().equals(")") || !terminalWord.getWordType().equals("RPARENT")) {
                index = start;
                return null;
            }
            stmt.add(terminalWord);
            nonTerminalWord = Stmt();
            if (nonTerminalWord == null) {
                index = start;
                return null;
            }
            return stmt;
        } else if (terminalWord.getWordName().equals("break") && terminalWord.getWordType().equals("BREAKTK")) {
            index += 1;
            stmt.add(terminalWord);
            terminalWord = terminalWords.get(index);
            index += 1;
            if (!terminalWord.getWordName().equals(";") || !terminalWord.getWordType().equals("SEMICN")) {
                index = start;
                return null;
            }
            stmt.add(terminalWord);
            return stmt;
        } else if (terminalWord.getWordName().equals("continue") && terminalWord.getWordType().equals("CONTINUETK")) {
            index += 1;
            stmt.add(terminalWord);
            terminalWord = terminalWords.get(index);
            index += 1;
            if (!terminalWord.getWordName().equals(";") || !terminalWord.getWordType().equals("SEMICN")) {
                index = start;
                return null;
            }
            stmt.add(terminalWord);
            return stmt;
        } else if (terminalWord.getWordName().equals("return") && terminalWord.getWordType().equals("RETURNTK")) {
            index += 1;
            stmt.add(terminalWord);
            nonTerminalWord = Exp();
            if (nonTerminalWord != null) {
                stmt.add(nonTerminalWord);
            }
            terminalWord = terminalWords.get(index);
            index += 1;
            if (!terminalWord.getWordName().equals(";") || !terminalWord.getWordType().equals("SEMICN")) {
                index = start;
                return null;
            }
            stmt.add(terminalWord);
            return stmt;
        } else if (terminalWord.getWordName().equals("printf") && terminalWord.getWordType().equals("PRINTFTK")) {
            index += 1;
            stmt.add(terminalWord);
            terminalWord = terminalWords.get(index);
            index += 1;
            if (!terminalWord.getWordName().equals("(") || !terminalWord.getWordType().equals("LPARENT")) {
                index = start;
                return null;
            }
            stmt.add(terminalWord);
            terminalWord = terminalWords.get(index);
            index += 1;
            if (!terminalWord.getWordType().equals("STRCON")) {
                index = start;
                return null;
            }
            stmt.add(terminalWord);
            terminalWord = terminalWords.get(index);
            while (terminalWord.getWordName().equals(",") && terminalWord.getWordType().equals("COMMA")) {
                index += 1;
                stmt.add(terminalWord);
                nonTerminalWord = Exp();
                if (nonTerminalWord == null) {
                    index = start;
                    return null;
                }
                stmt.add(nonTerminalWord);
                terminalWord = terminalWords.get(index);
            }
            terminalWord = terminalWords.get(index);
            index += 1;
            if (!terminalWord.getWordName().equals(")") || !terminalWord.getWordType().equals("RPARENT")) {
                index = start;
                return null;
            }
            stmt.add(terminalWord);
            terminalWord = terminalWords.get(index);
            index += 1;
            if (!terminalWord.getWordName().equals(";") || !terminalWord.getWordType().equals("SEMICN")) {
                index = start;
                return null;
            }
            stmt.add(terminalWord);
            return stmt;
        }
        nonTerminalWord = LVal();
        if (nonTerminalWord != null) {
            stmt.add(nonTerminalWord);
            terminalWord = terminalWords.get(index);
            index += 1;
            if (!terminalWord.getWordName().equals("=") || !terminalWord.getWordType().equals("ASSIGN")) {
                index = start;
                return null;
            }
            stmt.add(terminalWord);
            nonTerminalWord = Exp();
            terminalWord = terminalWords.get(index);
            if (nonTerminalWord != null) {
                stmt.add(nonTerminalWord);
            } else if (terminalWord.getWordName().equals("getint") && terminalWord.getWordType().equals("GETINTTK")) {
                index += 1;
                stmt.add(terminalWord);
                terminalWord = terminalWords.get(index);
                index += 1;
                if (!terminalWord.getWordName().equals("(") || !terminalWord.getWordType().equals("LPARENT")) {
                    index = start;
                    return null;
                }
                stmt.add(terminalWord);
                terminalWord = terminalWords.get(index);
                index += 1;
                if (!terminalWord.getWordName().equals(")") || !terminalWord.getWordType().equals("RPARENT")) {
                    index = start;
                    return null;
                }
                stmt.add(terminalWord);
            } else {
                index = start;
                return null;
            }
            terminalWord = terminalWords.get(index);
            index += 1;
            if (!terminalWord.getWordName().equals(";") || !terminalWord.getWordType().equals("SEMICN")) {
                index = start;
                return null;
            }
            stmt.add(terminalWord);
        }
        nonTerminalWord = Block();
        if (nonTerminalWord != null) {
            stmt.add(nonTerminalWord);
            return stmt;
        }
        nonTerminalWord = Exp();
        if (nonTerminalWord != null) {
            stmt.add(nonTerminalWord);
        }
        terminalWord = terminalWords.get(index);
        index += 1;
        if (!terminalWord.getWordName().equals(";") || !terminalWord.getWordType().equals("SEMICN")) {
            index = start;
            return null;
        }
        stmt.add(terminalWord);
        return stmt;
    }

    private NonTerminalWord Exp() {
        NonTerminalWord exp = new NonTerminalWord("<Exp>");
        int start = index;
        NonTerminalWord nonTerminalWord;
        nonTerminalWord = AddExp();
        if (nonTerminalWord == null) {
            index = start;
            return null;
        }
        exp.add(nonTerminalWord);
        return exp;
    }

    private NonTerminalWord Cond() {
        NonTerminalWord cond = new NonTerminalWord("<Cond>");
        int start = index;
        NonTerminalWord nonTerminalWord;
        nonTerminalWord = LOrExp();
        if (nonTerminalWord == null) {
            index = start;
            return null;
        }
        cond.add(nonTerminalWord);
        return cond;
    }

    private NonTerminalWord LVal() {
        NonTerminalWord lVal = new NonTerminalWord("<LVal>");
        int start = index;
        NonTerminalWord nonTerminalWord;
        TerminalWord terminalWord;
        terminalWord = terminalWords.get(index);
        index += 1;
        if (!terminalWord.getWordType().equals("IDENFR")) {
            index = start;
            return null;
        }
        terminalWord = terminalWords.get(index);
        lVal.add(terminalWord);
        while (terminalWord.getWordName().equals("[") && terminalWord.getWordType().equals("LBRACK")) {
            index += 1;
            lVal.add(terminalWord);
            nonTerminalWord = Exp();
            if (nonTerminalWord == null) {
                index = start;
                return null;
            }
            lVal.add(nonTerminalWord);
            terminalWord = terminalWords.get(index);
            index += 1;
            if (!terminalWord.getWordName().equals("]") || !terminalWord.getWordType().equals("RBRACK")) {
                index = start;
                return null;
            }
            lVal.add(terminalWord);
        }
        return lVal;
    }

    private NonTerminalWord PrimaryExp() {
        NonTerminalWord primaryExp = new NonTerminalWord("<PrimaryExp>");
        int start = index;
        NonTerminalWord nonTerminalWord;
        TerminalWord terminalWord;
        terminalWord = terminalWords.get(index);
        if (terminalWord.getWordName().equals("(") && terminalWord.getWordType().equals("LPARENT")) {
            index += 1;
            primaryExp.add(terminalWord);
            nonTerminalWord = Exp();
            if (nonTerminalWord == null) {
                index = start;
                return null;
            }
            primaryExp.add(nonTerminalWord);
            terminalWord = terminalWords.get(index);
            index += 1;
            if (!terminalWord.getWordName().equals(")") || !terminalWord.getWordType().equals("RPARENT")) {
                index = start;
                return null;
            }
            primaryExp.add(terminalWord);
            return primaryExp;
        }
        nonTerminalWord = LVal();
        if (nonTerminalWord != null) {
            primaryExp.add(nonTerminalWord);
            return primaryExp;
        }
        nonTerminalWord = Number();
        if (nonTerminalWord != null) {
            primaryExp.add(nonTerminalWord);
            return primaryExp;
        }
        return null;
    }

    private NonTerminalWord Number() {
        NonTerminalWord number = new NonTerminalWord("<Number>");
        int start = index;
        TerminalWord terminalWord;
        terminalWord = terminalWords.get(index);
        index += 1;
        if (!terminalWord.getWordType().equals("INTCON")) {
            index = start;
            return null;
        }
        number.add(terminalWord);
        return number;
    }

    private NonTerminalWord UnaryExp() {
        NonTerminalWord unaryExp = new NonTerminalWord("<UnaryExp>");
        int start = index;
        NonTerminalWord nonTerminalWord;
        TerminalWord terminalWord;
        terminalWord = terminalWords.get(index);
        if (terminalWord.getWordType().equals("IDENFR")) {
            index += 1;
            unaryExp.add(terminalWord);
            terminalWord = terminalWords.get(index);
            index += 1;
            if (!terminalWord.getWordName().equals("(") || !terminalWord.getWordType().equals("LPARENT")) {
                index = start;
                return null;
            }
            unaryExp.add(terminalWord);
            nonTerminalWord = FuncRParams();
            if (nonTerminalWord != null) {
                unaryExp.add(nonTerminalWord);
            }
            terminalWord = terminalWords.get(index);
            index += 1;
            if (!terminalWord.getWordName().equals(")") || !terminalWord.getWordType().equals("RPARENT")) {
                index = start;
                return null;
            }
            unaryExp.add(terminalWord);
            return unaryExp;
        }
        nonTerminalWord = PrimaryExp();
        if (nonTerminalWord != null) {
            unaryExp.add(nonTerminalWord);
            return unaryExp;
        }
        nonTerminalWord = UnaryOp();
        if (nonTerminalWord == null) {
            index = start;
            return null;
        }
        unaryExp.add(nonTerminalWord);
        nonTerminalWord = UnaryExp();
        if (nonTerminalWord == null) {
            index += 1;
            return null;
        }
        unaryExp.add(nonTerminalWord);
        return unaryExp;
    }

    private NonTerminalWord UnaryOp() {
        NonTerminalWord unaryOp = new NonTerminalWord("<UnaryOp>");
        int start = index;
        TerminalWord terminalWord;
        terminalWord = terminalWords.get(index);
        if (terminalWord.getWordName().equals("+") && terminalWord.getWordType().equals("PLUS")
                || terminalWord.getWordName().equals("-") && terminalWord.getWordType().equals("MINU")
                || terminalWord.getWordName().equals("!") && terminalWord.getWordType().equals("NOT")) {
            index += 1;
            unaryOp.add(terminalWord);
            return unaryOp;
        }
        return null;
    }

    private NonTerminalWord FuncRParams() {
        NonTerminalWord funcRParams = new NonTerminalWord("<FuncRParams>");
        int start = index;
        NonTerminalWord nonTerminalWord;
        TerminalWord terminalWord;
        nonTerminalWord = Exp();
        if (nonTerminalWord == null) {
            index = start;
            return null;
        }
        terminalWord = terminalWords.get(index);
        while (terminalWord.getWordName().equals(",") && terminalWord.getWordType().equals("COMMA")) {
            index += 1;
            funcRParams.add(terminalWord);
            nonTerminalWord = Exp();
            if (nonTerminalWord == null) {
                index = start;
                return null;
            }
            funcRParams.add(nonTerminalWord);
            terminalWord = terminalWords.get(index);
        }
        return funcRParams;
    }

    private NonTerminalWord MulExp() {
        NonTerminalWord mulExp = new NonTerminalWord("<MulExp>");
        int start = index;
        NonTerminalWord nonTerminalWord;
        TerminalWord terminalWord;
        nonTerminalWord = UnaryExp();
        if (nonTerminalWord == null) {
            index = start;
            return null;
        }
        mulExp.add(nonTerminalWord);
        terminalWord = terminalWords.get(index);
        while (terminalWord.getWordName().equals("*") && terminalWord.getWordType().equals("MULT")
                || terminalWord.getWordName().equals("/") && terminalWord.getWordType().equals("DIV")
                || terminalWord.getWordName().equals("%") && terminalWord.getWordType().equals("MOD")) {
            index += 1;
            mulExp.add(terminalWord);
            nonTerminalWord = UnaryExp();
            if (nonTerminalWord == null) {
                index = start;
                return null;
            }
            mulExp.add(nonTerminalWord);
            terminalWord = terminalWords.get(index);
        }
        return mulExp;
    }

    private NonTerminalWord AddExp() {
        NonTerminalWord addExp = new NonTerminalWord("<AddExp>");
        int start = index;
        NonTerminalWord nonTerminalWord;
        TerminalWord terminalWord;
        nonTerminalWord = MulExp();
        if (nonTerminalWord == null) {
            index = start;
            return null;
        }
        addExp.add(nonTerminalWord);
        terminalWord = terminalWords.get(index);
        while (terminalWord.getWordName().equals("+") && terminalWord.getWordType().equals("PLUS")
                || terminalWord.getWordName().equals("-") && terminalWord.getWordType().equals("MINU")
        ) {
            index += 1;
            addExp.add(terminalWord);
            nonTerminalWord = MulExp();
            if (nonTerminalWord == null) {
                index = start;
                return null;
            }
            addExp.add(nonTerminalWord);
            terminalWord = terminalWords.get(index);
        }
        return addExp;
    }

    private NonTerminalWord RelExp() {
        NonTerminalWord relExp = new NonTerminalWord("<RelExp>");
        int start = index;
        NonTerminalWord nonTerminalWord;
        TerminalWord terminalWord;
        nonTerminalWord = AddExp();
        if (nonTerminalWord == null) {
            index = start;
            return null;
        }
        relExp.add(nonTerminalWord);
        terminalWord = terminalWords.get(index);
        while (terminalWord.getWordName().equals(">") && terminalWord.getWordType().equals("GRE")
                || terminalWord.getWordName().equals("<") && terminalWord.getWordType().equals("LSS")
                || terminalWord.getWordName().equals(">=") && terminalWord.getWordType().equals("GEQ")
                || terminalWord.getWordName().equals("<=") && terminalWord.getWordType().equals("LEQ")
        ) {
            index += 1;
            relExp.add(terminalWord);
            nonTerminalWord = AddExp();
            if (nonTerminalWord == null) {
                index = start;
                return null;
            }
            relExp.add(nonTerminalWord);
            terminalWord = terminalWords.get(index);
        }
        return relExp;
    }

    private NonTerminalWord EqExp() {
        NonTerminalWord eqExp = new NonTerminalWord("<EqExp>");
        int start = index;
        NonTerminalWord nonTerminalWord;
        TerminalWord terminalWord;
        nonTerminalWord = RelExp();
        if (nonTerminalWord == null) {
            index = start;
            return null;
        }
        eqExp.add(nonTerminalWord);
        terminalWord = terminalWords.get(index);
        while (terminalWord.getWordName().equals("==") && terminalWord.getWordType().equals("EQL")
                || terminalWord.getWordName().equals("!=") && terminalWord.getWordType().equals("RBRACE")
        ) {
            index += 1;
            eqExp.add(terminalWord);
            nonTerminalWord = RelExp();
            if (nonTerminalWord == null) {
                index = start;
                return null;
            }
            eqExp.add(nonTerminalWord);
            terminalWord = terminalWords.get(index);
        }
        return eqExp;
    }

    private NonTerminalWord LAndExp() {
        NonTerminalWord lAndExp = new NonTerminalWord("<LAndExp>");
        int start = index;
        NonTerminalWord nonTerminalWord;
        TerminalWord terminalWord;
        nonTerminalWord = EqExp();
        if (nonTerminalWord == null) {
            index = start;
            return null;
        }
        lAndExp.add(nonTerminalWord);
        terminalWord = terminalWords.get(index);
        while (terminalWord.getWordName().equals("&&") && terminalWord.getWordType().equals("AND")
        ) {
            index += 1;
            lAndExp.add(terminalWord);
            nonTerminalWord = EqExp();
            if (nonTerminalWord == null) {
                index = start;
                return null;
            }
            lAndExp.add(nonTerminalWord);
            terminalWord = terminalWords.get(index);
        }
        return lAndExp;
    }

    private NonTerminalWord LOrExp() {
        NonTerminalWord lOrExp = new NonTerminalWord("<LOrExp>");
        int start = index;
        NonTerminalWord nonTerminalWord;
        TerminalWord terminalWord;
        nonTerminalWord = LOrExp();
        if (nonTerminalWord == null) {
            index = start;
            return null;
        }
        lOrExp.add(nonTerminalWord);
        terminalWord = terminalWords.get(index);
        while (terminalWord.getWordName().equals("||") && terminalWord.getWordType().equals("OR")
        ) {
            index += 1;
            lOrExp.add(terminalWord);
            nonTerminalWord = LAndExp();
            if (nonTerminalWord == null) {
                index = start;
                return null;
            }
            lOrExp.add(nonTerminalWord);
            terminalWord = terminalWords.get(index);
        }
        return lOrExp;
    }

    private NonTerminalWord ConstExp() {
        NonTerminalWord constExp = new NonTerminalWord("<ConstExp>");
        int start = index;
        NonTerminalWord nonTerminalWord;
        nonTerminalWord = AddExp();
        if (nonTerminalWord == null) {
            index = start;
            return null;
        }
        constExp.add(nonTerminalWord);
        return constExp;
    }
}