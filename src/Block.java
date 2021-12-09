import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Block {
    private final int index;
    private ArrayList<ArrayList<HashSet<String>>> codes = new ArrayList<>();
    private final HashSet<Block> pre = new HashSet<>();
    private final HashSet<Block> next = new HashSet<>();
    private final ParseMidCode parseMidCode = new ParseMidCode();
    private final HashSet<String> def = new HashSet<>();
    private final HashSet<String> use = new HashSet<>();
    private HashSet<String> activeIn = new HashSet<>();
    private HashSet<String> activeOut = new HashSet<>();

    public Block(Integer index, ArrayList<String> initialCodes) {
        this.index = index;
        String[] five;
        for (String initialCode : initialCodes) {
            five = parseMidCode.parseMidCode(initialCode);
            switch (five[0]) {
                // 值得注意的是变量、常量、数组声明的时候不需要认为它们已经产生
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
                    break;
            }
        }
        calDefAndUse();
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
        int size = activeIn.size();
        HashSet<String> a = new HashSet<>(use);
        HashSet<String> b = new HashSet<>(activeOut);
        HashSet<String> c = new HashSet<>(def);
        b.removeAll(c);
        a.addAll(b);
        activeIn = a;
        return size == activeIn.size();
    }

    private boolean isDigit(String s) {
        Pattern pattern = Pattern.compile("^[-+]?[\\d]*$");
        Matcher matcher = pattern.matcher(s);
        return matcher.matches();
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
        return activeIn;
    }

    public void addActiveOut(String a) {
        activeOut.add(a);
    }

    public HashSet<String> getActiveOut() {
        return activeOut;
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
        if (!isDigit(l) && !l.startsWith("#") && !l.equals("RET")) {
            lSet.add(l);
        }
        if (!isDigit(r1) && !r1.startsWith("#") && !r1.equals("RET")) {
            rSet.add(r1);
        }
        if (!isDigit(r2) && !r2.startsWith("#") && !r2.equals("RET")) {
            rSet.add(r2);
        }
        ArrayList<HashSet<String>> arrayList = new ArrayList<>();
        arrayList.add(lSet);
        arrayList.add(rSet);
        if (lSet.size() != 0 || rSet.size() != 0) {
            codes.add(arrayList);
        }
    }

    private void paraDef(String[] five) {
        // 函数形参可以看做被赋值
        HashSet<String> lSet = new HashSet<>();
        HashSet<String> rSet = new HashSet<>();
        String l = five[1];
        if (!isDigit(l) && !l.startsWith("#") && !l.equals("RET")) {
            lSet.add(l);
        }
        ArrayList<HashSet<String>> arrayList = new ArrayList<>();
        arrayList.add(lSet);
        arrayList.add(rSet);
        if (lSet.size() != 0 || rSet.size() != 0) {
            codes.add(arrayList);
        }
    }

    private void paraPush(String[] five) {
        // 实参压栈可以看做引用
        HashSet<String> lSet = new HashSet<>();
        HashSet<String> rSet = new HashSet<>();
        String r = five[1];
        if (!isDigit(r) && !r.startsWith("#") && !r.equals("RET")) {
            rSet.add(r);
        }
        ArrayList<HashSet<String>> arrayList = new ArrayList<>();
        arrayList.add(lSet);
        arrayList.add(rSet);
        if (lSet.size() != 0 || rSet.size() != 0) {
            codes.add(arrayList);
        }
    }

    private void ret(String[] five) {
        HashSet<String> lSet = new HashSet<>();
        HashSet<String> rSet = new HashSet<>();
        String r = five[1];
        if (!isDigit(r) && !r.startsWith("#") && !r.equals("RET")) {
            rSet.add(r);
        }
        ArrayList<HashSet<String>> arrayList = new ArrayList<>();
        arrayList.add(lSet);
        arrayList.add(rSet);
        if (lSet.size() != 0 || rSet.size() != 0) {
            codes.add(arrayList);
        }
    }

    private void beq(String[] five) {
        HashSet<String> lSet = new HashSet<>();
        HashSet<String> rSet = new HashSet<>();
        String r1 = five[1];
        String r2 = five[2];
        if (!isDigit(r1) && !r1.startsWith("#") && !r1.equals("RET")) {
            rSet.add(r1);
        }
        if (!isDigit(r2) && !r2.startsWith("#") && !r2.equals("RET")) {
            rSet.add(r2);
        }
        ArrayList<HashSet<String>> arrayList = new ArrayList<>();
        arrayList.add(lSet);
        arrayList.add(rSet);
        if (lSet.size() != 0 || rSet.size() != 0) {
            codes.add(arrayList);
        }
    }

    private void leftArr(String[] five) {
        HashSet<String> lSet = new HashSet<>();
        HashSet<String> rSet = new HashSet<>();
        String l1 = five[1];
        String l2 = five[2];
        String r = five[3];
        if (!isDigit(l1) && !l1.startsWith("#") && !l1.equals("RET")) {
            lSet.add(l1);
        }
        if (!isDigit(l2) && !l2.startsWith("#") && !l2.equals("RET")) {
            lSet.add(l2);
        }
        if (!isDigit(r) && !r.startsWith("#") && !r.equals("RET")) {
            rSet.add(r);
        }
        ArrayList<HashSet<String>> arrayList = new ArrayList<>();
        arrayList.add(lSet);
        arrayList.add(rSet);
        if (lSet.size() != 0 || rSet.size() != 0) {
            codes.add(arrayList);
        }
    }

    private void rightArr(String[] five) {
        HashSet<String> lSet = new HashSet<>();
        HashSet<String> rSet = new HashSet<>();
        String l = five[1];
        String r1 = five[2];
        String r2 = five[3];
        if (!isDigit(l) && !l.startsWith("#") && !l.equals("RET")) {
            lSet.add(l);
        }
        if (!isDigit(r1) && !r1.startsWith("#") && !r1.equals("RET")) {
            rSet.add(r1);
        }
        if (!isDigit(r2) && !r2.startsWith("#") && !r2.equals("RET")) {
            rSet.add(r2);
        }
        ArrayList<HashSet<String>> arrayList = new ArrayList<>();
        arrayList.add(lSet);
        arrayList.add(rSet);
        if (lSet.size() != 0 || rSet.size() != 0) {
            codes.add(arrayList);
        }
    }

    private void getint(String[] five) {
        HashSet<String> lSet = new HashSet<>();
        HashSet<String> rSet = new HashSet<>();
        String r = five[1];
        if (!isDigit(r) && !r.startsWith("#") && !r.equals("RET")) {
            rSet.add(r);
        }
        ArrayList<HashSet<String>> arrayList = new ArrayList<>();
        arrayList.add(lSet);
        arrayList.add(rSet);
        if (lSet.size() != 0 || rSet.size() != 0) {
            codes.add(arrayList);
        }
    }

    private void printf(String[] five) {
        HashSet<String> lSet = new HashSet<>();
        HashSet<String> rSet = new HashSet<>();
        if (five[1].equals("%d")) {
            String r = five[2];
            if (!isDigit(r) && !r.startsWith("#") && !r.equals("RET")) {
                rSet.add(r);
            }
        }
        ArrayList<HashSet<String>> arrayList = new ArrayList<>();
        arrayList.add(lSet);
        arrayList.add(rSet);
        if (lSet.size() != 0 || rSet.size() != 0) {
            codes.add(arrayList);
        }
    }
}
