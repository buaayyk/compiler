import javax.swing.text.LabelView;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReplaceDuplicate {
    // 替换重名变量名
    private final ArrayList<String> midCodes;
    private final ParseMidCode parseMidCode = new ParseMidCode();
    private int idx;
    private final SymbolTable symbolTable = new SymbolTable();

    public ReplaceDuplicate(ArrayList<String> midCodes) {
        this.midCodes = new ArrayList<>(midCodes);
    }

    private boolean isDigit(String s) {
        Pattern pattern = Pattern.compile("^[-+]?[\\d]*$");
        Matcher matcher = pattern.matcher(s);
        return matcher.matches();
    }

    public ArrayList<String> getMidCodes() {
        return midCodes;
    }

    public void replaceDuplicate() {
        symbolTable.addNewLayer();
        String midCode;
        String[] five;
        for (idx = 0; idx < midCodes.size(); idx++) {
            midCode = midCodes.get(idx);
            five = parseMidCode.parseMidCode(midCode);
            switch (five[0]) {
                case "3":
                case "19":
                    symbolTable.addNewLayer();
                    break;
                case "4":
                case "20":
                    symbolTable.removeCurrentLayer();
                    break;
                case "5":
                    paraDef(five);
                    break;
                case "9":
                    varDef(five);
                    break;
                case "10":
                    constDef(five);
                    break;
                case "14":
                    arrDef(five);
                    break;
                case "1":
                    exp(five);
                    break;
                case "6":
                    paraPush(five);
                    break;
                case "8":
                    ret(five);
                    break;
                case "12":
                    beq(five);
                    break;
                case "15":
                    lefArr(five);
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
                default:
                    break;
            }
        }
    }

    private void varDef(String[] five) {
        String name = five[1];
        IdentSymbol identSymbol = symbolTable.searchIdentInAllLayers(name);
        IdentSymbol identSymbol1 = new IdentSymbol(name, false);
        if (identSymbol != null) {
            midCodes.set(idx, "var int " + name + "#" + idx);
            identSymbol1.setIndex(idx);
        }
        symbolTable.add(identSymbol1);
    }


    private void constDef(String[] five) {
        String name = five[1];
        IdentSymbol identSymbol = symbolTable.searchIdentInAllLayers(name);
        IdentSymbol identSymbol1 = new IdentSymbol(name, true);
        if (identSymbol != null) {
            midCodes.set(idx, "const int " + name + "#" + idx);
            identSymbol1.setIndex(idx);
        }
        symbolTable.add(identSymbol1);
    }

    private void paraDef(String[] five) {
        symbolTable.addNewLayer();
        String midCode;
        String[] five1;
        String name;
        five1 = five;
        while (five1[0].equals("5")) {
            name = five1[1];
            IdentSymbol identSymbol = symbolTable.searchIdentInAllLayers(name);
            IdentSymbol identSymbol1 = new IdentSymbol(name, false);
            if (identSymbol != null) {
                midCodes.set(idx, "para int " + name + "#" + idx);
                identSymbol1.setIndex(idx);
            }
            symbolTable.add(identSymbol1);
            idx += 1;
            midCode = midCodes.get(idx);
            five1 = parseMidCode.parseMidCode(midCode);
        }
    }

    private void arrDef(String[] five) {
        String name = five[1];
        IdentSymbol identSymbol = symbolTable.searchIdentInAllLayers(name);
        IdentSymbol identSymbol1 = new IdentSymbol(name, false);
        if (identSymbol != null) {
            midCodes.set(idx, "arr int " + name + "#" + idx + "[" + five[2] + "]");
            identSymbol1.setIndex(idx);
        }
        symbolTable.add(identSymbol1);
    }

    private void exp(String[] five) {
        String lVal = five[2];
        String op = five[1];
        String r1 = five[3];
        String r2 = five[4];
        IdentSymbol identSymbol;
        if (!lVal.startsWith("#") && !lVal.equals("RET")) {
            identSymbol = symbolTable.searchIdentInAllLayers(lVal);
            if (identSymbol.getIndex() != -1) {
                lVal = lVal + "#" + identSymbol.getIndex();
            }
        }
        if (!isDigit(r1) && !r1.startsWith("#") && !r1.equals("RET")) {
            identSymbol = symbolTable.searchIdentInAllLayers(r1);
            if (identSymbol.getIndex() != -1) {
                r1 = r1 + "#" + identSymbol.getIndex();
            }
        }
        if (!isDigit(r2) && !r2.startsWith("#") && !r2.equals("RET")) {
            identSymbol = symbolTable.searchIdentInAllLayers(r2);
            if (identSymbol.getIndex() != -1) {
                r2 = r2 + "#" + identSymbol.getIndex();
            }
        }
        if (r1.equals("0") && op.equals("+")) {
            midCodes.set(idx, lVal + " = " + r2);
        } else if (r1.equals("0") && (op.equals("-") || op.equals("!"))) {
            midCodes.set(idx, lVal + " = " + op + " " + r2 + " ");
        } else {
            midCodes.set(idx, lVal + " = " + r1 + " " + op + " " + r2);
        }
    }

    private void paraPush(String[] five) {
        String name = five[1];
        if (!isDigit(name) && !name.startsWith("#") && !name.equals("RET")) {
            IdentSymbol identSymbol;
            identSymbol = symbolTable.searchIdentInAllLayers(name);
            int index = identSymbol.getIndex();
            if (index != -1) {
                if (five[2].equals("")) {
                    midCodes.set(idx, "push " + name + "#" + index);
                } else {
                    midCodes.set(idx, "push " + name + "#" + index + "[" + five[2] + "]");
                }
            }
        }
    }

    private void ret(String[] five) {
        if (!five[1].equals("")) {
            String name = five[1];
            IdentSymbol identSymbol;
            if (!isDigit(name) && !name.startsWith("#") && !name.equals("RET")) {
                identSymbol = symbolTable.searchIdentInAllLayers(name);
                if (identSymbol.getIndex() != -1) {
                    name = name + "#" + identSymbol.getIndex();
                }
                midCodes.set(idx, "ret " + name);
            }
        }
    }

    private void beq(String[] five) {
        String r1 = five[1];
        String r2 = five[2];
        String label = five[3];
        IdentSymbol identSymbol;
        if (!isDigit(r1) && !r1.startsWith("#") && !r1.equals("RET")) {
            identSymbol = symbolTable.searchIdentInAllLayers(r1);
            if (identSymbol.getIndex() != -1) {
                r1 = r1 + "#" + identSymbol.getIndex();
            }
        }
        if (!isDigit(r2) && !r2.startsWith("#") && !r2.equals("RET")) {
            identSymbol = symbolTable.searchIdentInAllLayers(r2);
            if (identSymbol.getIndex() != -1) {
                r2 = r2 + "#" + identSymbol.getIndex();
            }
        }
        midCodes.set(idx, "beq " + r1 + " " + r2 + " " + label);
    }

    private void lefArr(String[] five) {
        String arrName = five[1];
        String offset = five[2];
        String r = five[3];
        IdentSymbol identSymbol;
        if (!arrName.startsWith("#") && !arrName.equals("RET")) {
            identSymbol = symbolTable.searchIdentInAllLayers(arrName);
            if (identSymbol.getIndex() != -1) {
                arrName = arrName + "#" + identSymbol.getIndex();
            }
        }
        if (!isDigit(offset) && !offset.startsWith("#") && !offset.equals("RET")) {
            identSymbol = symbolTable.searchIdentInAllLayers(offset);
            if (identSymbol.getIndex() != -1) {
                offset = offset + "#" + identSymbol.getIndex();
            }
        }
        if (!isDigit(r) && !r.startsWith("#") && !r.equals("RET")) {
            identSymbol = symbolTable.searchIdentInAllLayers(r);
            if (identSymbol.getIndex() != -1) {
                r = r + "#" + identSymbol.getIndex();
            }
        }
        midCodes.set(idx, arrName + "[" + offset + "]" + " = " + r);
    }

    private void rightArr(String[] five) {
        String l = five[1];
        String arrName = five[2];
        String offset = five[3];
        IdentSymbol identSymbol;
        if (!isDigit(l) && !l.startsWith("#") && !l.equals("RET")) {
            identSymbol = symbolTable.searchIdentInAllLayers(l);
            if (identSymbol.getIndex() != -1) {
                l = l + "#" + identSymbol.getIndex();
            }
        }
        if (!arrName.startsWith("#") && !arrName.equals("RET")) {
            identSymbol = symbolTable.searchIdentInAllLayers(arrName);
            if (identSymbol.getIndex() != -1) {
                arrName = arrName + "#" + identSymbol.getIndex();
            }
        }
        if (!isDigit(offset) && !offset.startsWith("#") && !offset.equals("RET")) {
            identSymbol = symbolTable.searchIdentInAllLayers(offset);
            if (identSymbol.getIndex() != -1) {
                offset = offset + "#" + identSymbol.getIndex();
            }
        }
        midCodes.set(idx, l + " = " + arrName + "[" + offset + "]");
    }

    private void getint(String[] five) {
        String name = five[1];
        IdentSymbol identSymbol;
        if (!isDigit(name) && !name.startsWith("#") && !name.equals("RET")) {
            identSymbol = symbolTable.searchIdentInAllLayers(name);
            if (identSymbol.getIndex() != -1) {
                name = name + "#" + identSymbol.getIndex();
            }
        }
        midCodes.set(idx, "@getint " + name);
    }

    private void printf(String[] five) {
        if (five[1].equals("%d")) {
            String name = five[2];
            IdentSymbol identSymbol;
            if (!isDigit(name) && !name.startsWith("#") && !name.equals("RET")) {
                identSymbol = symbolTable.searchIdentInAllLayers(name);
                if (identSymbol.getIndex() != -1) {
                    name = name + "#" + identSymbol.getIndex();
                }
            }
            midCodes.set(idx, "@printf %d " + name);
        }
    }
}
