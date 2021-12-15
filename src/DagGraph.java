import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class DagGraph {
    private final ArrayList<String> optimizedMidCodes;
    private final ArrayList<String> midCodes;
    private final ParseMidCode parseMidCode = new ParseMidCode();
    private int number = 0; // 当前分析的block中已经生成的node数量
    private String lastFunc = ""; // 最新被调用的函数名

    public DagGraph(ArrayList<String> midCodes) {
        this.midCodes = midCodes;
        optimizedMidCodes = new ArrayList<>();
    }

    public void optimize() {
        boolean funcStart = false;
        for (int i = 0; i < midCodes.size(); i++) {
            String midCode = midCodes.get(i);
            String[] five = parseMidCode.parseMidCode(midCode);
            if (!funcStart) {
                // 将所有全局变量相关的代码复制一遍
                if (five[0].equals("2")) {
                    funcStart = true;
                }
                if (!funcStart) {
                    optimizedMidCodes.add(midCode);
                    continue;
                }
            }
            if (five[2].equals("2")) {
                int start = i;
                while (i < midCodes.size()) {
                    String midCode1 = midCodes.get(i);
                    String[] five1 = parseMidCode.parseMidCode(midCode1);
                    if (five1[0].equals("4")) {
                        // 找到函数的结尾位置
                        break;
                    }
                    i += 1;
                }
                BlockSplit blockSplit;
                if (i == midCodes.size()) {
                    blockSplit = new BlockSplit(new ArrayList<>(midCodes.subList(start, i)), new ArrayList<>());
                } else {
                    blockSplit = new BlockSplit(new ArrayList<>(midCodes.subList(start, i + 1)), new ArrayList<>());
                }
                ArrayList<Block> blocks = blockSplit.getBlocks();
                for (int j = 0; j < blocks.size(); j++) {
                    Block block = blocks.get(j);
                    optimizeBlock(block);
                }
            }
        }
    }

    private void optimizeBlock(Block block) {
        number = 0;
        ArrayList<String> firsts = new ArrayList<>(); // block中可以前移的代码
        ArrayList<String> lasts = new ArrayList<>(); // block中可以后置的代码
        ArrayList<String> codes = new ArrayList<>(block.getSplitCodes());
        for (int i = 0; i < codes.size(); i++) {
            String code = codes.get(i);
            String[] five = parseMidCode.parseMidCode(code);
            switch (five[0]) {
                case "2":
                case "3":
                case "5":
                case "9":
                case "10":
                case "11":
                case "14":
                case "19":
                    firsts.add(code);
                    codes.remove(i);
                    i -= 1;
                    break;
                case "4":
                case "13":
                case "20":
                    lasts.add(code);
                    codes.remove(i);
                    i -= 1;
                    break;
                default:
                    break;
            }
        }
        HashMap<String, Integer> name2Node = new HashMap<>();
        HashMap<Integer, Node> nodes = new HashMap<>();
        for (int i = 0; i < codes.size(); i++) {
            String code = codes.get(i);
            String[] five = parseMidCode.parseMidCode(code);
            switch (five[0]) {
                case "1":
                    exp(five, name2Node, nodes);
                    break;
                case "6":
                    ArrayList<String[]> fives = new ArrayList<>();
                    int end;
                    for (end = i; end < codes.size(); end++) {
                        code = codes.get(end);
                        five = parseMidCode.parseMidCode(code);
                        fives.add(five);
                        if (five[0].equals("7")) {
                            break;
                        }
                    }
                    i = end;
                    paraR(fives, name2Node, nodes);
                    break;
                case "7":
                    break;
                case "8":
                    break;
                case "12":
                    break;
                case "14":
                    break;
                case "15":
                    break;
                case "16":
                    break;
                case "17":
                    break;
                case "18":
                    break;
                default:
                    break;
            }
        }
    }

    private void exp(String[] five, HashMap<String, Integer> name2Node, HashMap<Integer, Node> nodes) {
        // TODO 对于 a = RET这种的需要更多考虑
        // 表达式看作z = x op y
        String op = five[1];
        String x = five[3];
        String y = five[4];
        String z = five[2];
        if (op.equals("+") && x.equals("0")) {
            if (name2Node.containsKey(y)) {
                int index = name2Node.get(y);
                Node node = nodes.get(index);
                node.addVars(z);
                name2Node.put(z, index);
            } else {
                Node node = new Node(number);
                node.addVars(y);
                node.addVars(z);
                name2Node.put(y, number);
                name2Node.put(z, number);
                nodes.put(number, node);
                number += 1;
            }
        } else if (op.equals("+") && y.equals("0")) {
            if (name2Node.containsKey(x)) {
                int index = name2Node.get(x);
                Node node = nodes.get(index);
                node.addVars(z);
                name2Node.put(z, index);
            } else {
                Node node = new Node(number);
                node.addVars(x);
                node.addVars(z);
                name2Node.put(x, number);
                name2Node.put(z, number);
                nodes.put(number, node);
                number += 1;
            }
        } else if (y.equals("RET")) {
            // 一定是z = RET这种形式
            int index = name2Node.get(lastFunc);
            Node node = nodes.get(index);
            node.addVars(z);
            name2Node.put(z, index);
        } else {
            Node nodeX;
            Node nodeY;
            if (name2Node.containsKey(x)) {
                int index;
                index = name2Node.get(x);
                nodeX = nodes.get(index);
            } else {
                nodeX = new Node(number);
                nodeX.addVars(x);
                name2Node.put(x, number);
                nodes.put(number, nodeX);
                number += 1;
            }

            if (name2Node.containsKey(y)) {
                // 考虑到函数返回值这一特殊情况，出现的RET并不是同一个RET
                int index;
                index = name2Node.get(y);
                nodeY = nodes.get(index);
            } else {
                nodeY = new Node(number);
                nodeY.addVars(y);
                name2Node.put(y, number);
                nodes.put(number, nodeY);
                number += 1;
            }
            boolean bool = false;
            for (Integer index : nodeX.fathers) {
                Node node = nodes.get(index);
                if (node.op.equals(op) && node.sons.contains(nodeY.index)) {
                    node.addVars(z);
                    name2Node.put(z, node.index);
                    bool = true;
                    break;
                }
            }
            if (!bool) {
                Node node = new Node(number, op);
                node.addVars(z);
                name2Node.put(z, number);
                nodes.put(number, node);
                number += 1;
                // 添加关系
                node.addSon(nodeX.index);
                node.addSon(nodeY.index);
                nodeX.addFather(node.index);
                nodeY.addFather(node.index);
            }
        }
    }

    private void paraR(ArrayList<String[]> fives, HashMap<String, Integer> name2Node, HashMap<Integer, Node> nodes) {
        ArrayList<Node> paraNodes = new ArrayList<>();
        String[] five;
        for (int i = 0; i < fives.size() - 1; i++) {
            five = fives.get(i);
            if (five[2].equals("")) {
                Node paraNode;
                if (name2Node.containsKey(five[1])) {
                    int index;
                    index = name2Node.get(five[1]);
                    paraNode = nodes.get(index);
                } else {
                    paraNode = new Node(number);
                    paraNode.addVars(five[1]);
                    name2Node.put(five[1], number);
                    nodes.put(number, paraNode);
                    number += 1;
                }
                paraNodes.add(paraNode);
            } else {
//                Node node1;
//                if()
//                Node node1 = new Node(number);
//                node1.addVars(five[1]);
//                // name2Node.put(five[1],number); 这一句反倒是不需要的，因为这里仅仅代表数组的指针，这个指针永远也不会变。
//                nodes.put(number, node1);
//                number += 1;
//                Node node2;
//                if (name2Node.containsKey(five[2])) {
//                    int index = name2Node.get(five[1]);
//                    node2 = nodes.get(index);
//                } else {
//                    node2 = new Node(number);
//                    node2.addVars(five[2]);
//                    name2Node.put(five[2], number);
//                    nodes.put(number, node2);
//                    number += 1;
//                }

//                paraNodes.add(node1);
//                paraNodes.add(node2);
            }
        }
//        String funcName =
    }

    private class Node {
        private int index;
        private HashSet<String> vars = new HashSet<>();
        private String op;
        private ArrayList<Integer> fathers = new ArrayList<>();
        private ArrayList<Integer> sons = new ArrayList<>();

        private Node(int index) {
            this.index = index;
            this.op = "";
        }

        private Node(int index, String op) {
            this.index = index;
            this.op = op;
        }

        private void addVars(String name) {
            vars.add(name);
        }

        private void addFather(int idx) {
            fathers.add(idx);
        }

        private void addSon(int idx) {
            sons.add(idx);
        }
    }
}
