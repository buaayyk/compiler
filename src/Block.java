import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Block {
    private final int index;
    public final ArrayList<String> splitCodes;
    private ArrayList<ArrayList<HashSet<String>>> codes = new ArrayList<>();
    private final HashSet<Block> pre = new HashSet<>();
    private final HashSet<Block> next = new HashSet<>();
    private final ParseMidCode parseMidCode = new ParseMidCode();
    private final HashSet<String> def = new HashSet<>();
    private final HashSet<String> use = new HashSet<>();
    private HashSet<String> activeIn = new HashSet<>();
    private HashSet<String> activeOut = new HashSet<>();
    private ArrayList<HashSet<String>> killD = new ArrayList<>();
    private HashSet<String> kill = new HashSet<>();
    private HashSet<String> gen = new HashSet<>();
    private HashSet<String> arriveIn = new HashSet<>();
    private HashSet<String> arriveOut = new HashSet<>();
    private HashSet<String> otherVars;

    public Block(Integer index, ArrayList<String> splitCodes, ArrayList<String> otherVars) {
        this.otherVars = new HashSet<>(otherVars);
        this.index = index;
        this.splitCodes = splitCodes;
        String[] five;
        for (String splitCode : splitCodes) {
            five = parseMidCode.parseMidCode(splitCode);
            switch (five[0]) {
                // 值得注意的是变量、常量声明的时候不需要认为它们已经产生,但是数组声明的时候由于需要给指针赋值，因此认为已经产生
                case "1":
                    exp(five);
                    break;
                case "5":
                    paraDef(five);
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
                default:
                    other(five);
                    break;
            }
        }
        calDefAndUse();
    }

    public int size() {
        return codes.size();
    }

    private void calDefAndUse() {
        for (ArrayList<HashSet<String>> code : codes) {
            HashSet<String> lSet = code.get(0);
            HashSet<String> rSet = code.get(1);
            for (String a : rSet) {
                //use
                if (!def.contains(a)) {
                    use.add(a);
                }
            }
            for (String b : lSet) {
                // def
                if (!use.contains(b)) {
                    def.add(b);
                }
            }
        }
    }

    public void calActiveOut() {
        HashSet<String> a = new HashSet<>();
        for (Block block : next) {
            a.addAll(block.getActiveIn());
        }
        activeOut = a;
    }

    public boolean calActiveIn() {
        if (index == 40) {
            int b = 1;
        }
        int size = activeIn.size();
        HashSet<String> a = new HashSet<>(use);
        HashSet<String> b = new HashSet<>(activeOut);
        HashSet<String> c = new HashSet<>(def);
        b.removeAll(c);
        a.addAll(b);
        activeIn = a;
        return size == activeIn.size();
    }

    public void calKillD(HashMap<String, HashSet<String>> var2D) {
        ArrayList<HashSet<String>> code;
        for (int i = 0; i < codes.size(); i++) {
            code = codes.get(i);
            HashSet<String> lSet = code.get(0);
            HashSet<String> hashSet = new HashSet<>();
            for (String varName : lSet) {
                if (var2D.containsKey(varName)) {
                    hashSet = new HashSet<>(var2D.get(varName));
                    hashSet.remove("" + index + " " + i);
                }
            }
            killD.add(hashSet);
        }
    }

    public void calGen() {
        ArrayList<HashSet<String>> code;
        HashSet<String> a;
        for (int i = 0; i < codes.size(); i++) {
            code = codes.get(i);
            HashSet<String> lSet = code.get(0);
            if (lSet.size() != 0) {
                a = new HashSet<>();
                a.add("" + index + " " + i);
                for (int j = i + 1; j < codes.size(); j++) {
                    a.removeAll(killD.get(j));
                }
                gen.addAll(a);
            }
        }
    }

    public void calKill() {
        for (int i = 0; i < codes.size(); i++) {
            kill.addAll(killD.get(i));
        }
    }

    public void calArriveIn() {
        HashSet<String> a = new HashSet<>();
        for (Block block : pre) {
            a.addAll(block.getActiveOut());
        }
        activeOut = a;
    }

    public boolean calArriveOut() {
        int size = arriveOut.size();
        HashSet<String> a = new HashSet<>(gen);
        HashSet<String> b = new HashSet<>(arriveIn);
        HashSet<String> c = new HashSet<>(kill);
        b.removeAll(c);
        a.addAll(b);
        activeOut = a;
        return size == arriveOut.size();
    }


    private boolean isDigit(String s) {
        Pattern pattern = Pattern.compile("^[-+]?[\\d]*$");
        Matcher matcher = pattern.matcher(s);
        return matcher.matches();
    }

    public ArrayList<ArrayList<HashSet<String>>> getCodes() {
        return codes;
    }

    public HashSet<Block> getPre() {
        return pre;
    }

    public HashSet<Block> getNext() {
        return next;
    }

    public int getIndex() {
        return index;
    }

    public void addActiveIn(String a) {
        activeIn.add(a);
    }

    public HashSet<String> getActiveIn() {
        return new HashSet<>(activeIn);
    }

    public void addActiveOut(String a) {
        activeOut.add(a);
    }

    public HashSet<String> getActiveOut() {
        return new HashSet<>(activeOut);
    }

    public void addPre(Block block) {
        pre.add(block);
    }

    public void addNext(Block block) {
        next.add(block);
    }

    private void exp(String[] five) {
        HashSet<String> lSet = new HashSet<>();
        HashSet<String> rSet = new HashSet<>();
        String l = five[2];
        String r1 = five[3];
        String r2 = five[4];
        if (!isDigit(l) && !l.startsWith("#") && !l.equals("RET") && !otherVars.contains(l)) {
            lSet.add(l);
        }
        if (!isDigit(r1) && !r1.startsWith("#") && !r1.equals("RET") && !otherVars.contains(r1)) {
            rSet.add(r1);
        }
        if (!isDigit(r2) && !r2.startsWith("#") && !r2.equals("RET") && !otherVars.contains(r2)) {
            rSet.add(r2);
        }
        ArrayList<HashSet<String>> arrayList = new ArrayList<>();
        arrayList.add(lSet);
        arrayList.add(rSet);
        codes.add(arrayList);
    }

    private void paraDef(String[] five) {
        // 函数形参可以看做被赋值
        HashSet<String> lSet = new HashSet<>();
        HashSet<String> rSet = new HashSet<>();
        String l = five[1];
        if (!isDigit(l) && !l.startsWith("#") && !l.equals("RET") && !otherVars.contains(l)) {
            lSet.add(l);
        }
        ArrayList<HashSet<String>> arrayList = new ArrayList<>();
        arrayList.add(lSet);
        arrayList.add(rSet);
        codes.add(arrayList);
    }

    private void paraPush(String[] five) {
        // 实参压栈可以看做引用
        HashSet<String> lSet = new HashSet<>();
        HashSet<String> rSet = new HashSet<>();
        String r = five[1];
        if (!isDigit(r) && !r.startsWith("#") && !r.equals("RET") && !otherVars.contains(r)) {
            rSet.add(r);
        }
        ArrayList<HashSet<String>> arrayList = new ArrayList<>();
        arrayList.add(lSet);
        arrayList.add(rSet);
        codes.add(arrayList);
    }

    private void ret(String[] five) {
        HashSet<String> lSet = new HashSet<>();
        HashSet<String> rSet = new HashSet<>();
        String r = five[1];
        if (!isDigit(r) && !r.startsWith("#") && !r.equals("RET") && !otherVars.contains(r)) {
            rSet.add(r);
        }
        ArrayList<HashSet<String>> arrayList = new ArrayList<>();
        arrayList.add(lSet);
        arrayList.add(rSet);
        codes.add(arrayList);
    }

    private void beq(String[] five) {
        HashSet<String> lSet = new HashSet<>();
        HashSet<String> rSet = new HashSet<>();
        String r1 = five[1];
        String r2 = five[2];
        if (!isDigit(r1) && !r1.startsWith("#") && !r1.equals("RET") && !otherVars.contains(r1)) {
            rSet.add(r1);
        }
        if (!isDigit(r2) && !r2.startsWith("#") && !r2.equals("RET") && !otherVars.contains(r2)) {
            rSet.add(r2);
        }
        ArrayList<HashSet<String>> arrayList = new ArrayList<>();
        arrayList.add(lSet);
        arrayList.add(rSet);
        codes.add(arrayList);
    }

    private void arrDef(String[] five) {
        HashSet<String> lSet = new HashSet<>();
        HashSet<String> rSet = new HashSet<>();
        String arrName = five[1];
        if (!isDigit(arrName) && !arrName.startsWith("#") && !arrName.equals("RET") && !otherVars.contains(arrName)) {
            // 虽然数组在左边，但实际上是一个被引用变量
            lSet.add(arrName);
        }
        ArrayList<HashSet<String>> arrayList = new ArrayList<>();
        arrayList.add(lSet);
        arrayList.add(rSet);
        codes.add(arrayList);
    }

    private void leftArr(String[] five) {
        HashSet<String> lSet = new HashSet<>();
        HashSet<String> rSet = new HashSet<>();
        String l1 = five[1];
        String l2 = five[2];
        String r = five[3];
        if (!isDigit(l1) && !l1.startsWith("#") && !l1.equals("RET") && !otherVars.contains(l1)) {
            // 虽然数组在左边，但实际上是一个被引用变量
            rSet.add(l1);
        }
        if (!isDigit(l2) && !l2.startsWith("#") && !l2.equals("RET") && !otherVars.contains(l2)) {
            // 虽然偏移在左边，但实际上是一个被引用变量
            rSet.add(l2);
        }
        if (!isDigit(r) && !r.startsWith("#") && !r.equals("RET") && !otherVars.contains(r)) {
            rSet.add(r);
        }
        ArrayList<HashSet<String>> arrayList = new ArrayList<>();
        arrayList.add(lSet);
        arrayList.add(rSet);
        codes.add(arrayList);
    }

    private void rightArr(String[] five) {
        HashSet<String> lSet = new HashSet<>();
        HashSet<String> rSet = new HashSet<>();
        String l = five[1];
        String r1 = five[2];
        String r2 = five[3];
        if (!isDigit(l) && !l.startsWith("#") && !l.equals("RET") && !otherVars.contains(l)) {
            lSet.add(l);
        }
        if (!isDigit(r1) && !r1.startsWith("#") && !r1.equals("RET") && !otherVars.contains(r1)) {
            rSet.add(r1);
        }
        if (!isDigit(r2) && !r2.startsWith("#") && !r2.equals("RET") && !otherVars.contains(r2)) {
            rSet.add(r2);
        }
        ArrayList<HashSet<String>> arrayList = new ArrayList<>();
        arrayList.add(lSet);
        arrayList.add(rSet);
        codes.add(arrayList);
    }

    private void getint(String[] five) {
        HashSet<String> lSet = new HashSet<>();
        HashSet<String> rSet = new HashSet<>();
        String r = five[1];
        if (!isDigit(r) && !r.startsWith("#") && !r.equals("RET") && !otherVars.contains(r)) {
            rSet.add(r);
        }
        ArrayList<HashSet<String>> arrayList = new ArrayList<>();
        arrayList.add(lSet);
        arrayList.add(rSet);
        codes.add(arrayList);
    }

    private void printf(String[] five) {
        HashSet<String> lSet = new HashSet<>();
        HashSet<String> rSet = new HashSet<>();
        if (five[1].equals("%d")) {
            String r = five[2];
            if (!isDigit(r) && !r.startsWith("#") && !r.equals("RET") && !otherVars.contains(r)) {
                rSet.add(r);
            }
        }
        ArrayList<HashSet<String>> arrayList = new ArrayList<>();
        arrayList.add(lSet);
        arrayList.add(rSet);
        codes.add(arrayList);
    }

    private void other(String[] five) {
        HashSet<String> lSet = new HashSet<>();
        HashSet<String> rSet = new HashSet<>();
        ArrayList<HashSet<String>> arrayList = new ArrayList<>();
        arrayList.add(lSet);
        arrayList.add(rSet);
        codes.add(arrayList);
    }
}
