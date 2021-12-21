import sun.util.resources.cldr.zh.CalendarData_zh_Hans_HK;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DagGraph {
    private final ArrayList<String> optimizedMidCodes;
    private final ArrayList<String> midCodes;
    private final ParseMidCode parseMidCode = new ParseMidCode();
    private int number = 0; // 当前分析的block中已经生成的node数量
    private final ArrayList<ArrayList<HashSet<String>>> codes1 = new ArrayList<>(); // 经过处理的中间代码，只保留定义和引用信息

    public DagGraph(ArrayList<String> midCodes) {
        this.midCodes = midCodes;
        optimizedMidCodes = new ArrayList<>();
    }

    public ArrayList<String> getOptimizedMidCodes() {
        return optimizedMidCodes;
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
            if (five[0].equals("2")) {
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
                blockSplit.blockSplit(true);
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
        System.out.println(codes);
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
        System.out.println("!!!!!!!!!!!!!!!!!!!");
        System.out.println(firsts);
        System.out.println(codes);
        System.out.println(lasts);
        optimizedMidCodes.addAll(firsts);
        HashMap<String, Integer> name2Node = new HashMap<>();
        HashMap<Integer, Node> nodes = new HashMap<>();
        ArrayList<Integer> orders = new ArrayList<>();
        HashSet<String> defs = new HashSet<>(); // 使用前先被定义
        HashSet<String> uses = new HashSet<>(); // 定义前先被使用
        for (int i = 0; i < codes.size(); i++) {
            String code = codes.get(i);
            String[] five = parseMidCode.parseMidCode(code);
            if (five[0].equals("1")) {
                int b = 1;
            }
            switch (five[0]) {
                case "1":
                    exp(five, name2Node, nodes);
                    break;
                case "6":
                    paraR(five, name2Node, nodes);
                    break;
                case "7":
                    // push和call都需要写回DAG图
                    // TODO 先活跃变量分析，然后计算出DAG图还原的代码，最后写回
                    this.codes1.clear();
                    for (int idx = i; idx < codes.size(); idx++) {
                        // 计算出后面语句的被定义和被使用变量
                        five = parseMidCode.parseMidCode(codes.get(idx));
                        updateCode(five);
                    }
                    defs.clear();
                    uses.clear();
                    for (ArrayList<HashSet<String>> code1 : this.codes1) {
                        HashSet<String> defSet = code1.get(0);
                        HashSet<String> useSet = code1.get(1);
                        for (String a : useSet) {
                            //use
                            if (!defs.contains(a)) {
                                uses.add(a);
                            }
                        }
                        for (String b : defSet) {
                            // def
                            if (!uses.contains(b)) {
                                defs.add(b);
                            }
                        }
                    }
                    if (uses.contains("#t11")) {
                        int b = 1;
                    }
                    // uses中的变量(定义前先被使用的变量)加上C源代码中可见的变量就是此时所有的活跃变量
                    deriveCodes(nodes, uses, orders);
                    name2Node.clear();
                    nodes.clear();
                    orders.clear();
                    while (i < codes.size()) {
                        code = codes.get(i);
                        five = parseMidCode.parseMidCode(code);
                        System.out.println(code);
                        optimizedMidCodes.add(code);
                        if (five[0].equals("7")) {
                            break;
                        }
                        i += 1;
                    }
                    if (i + 1 < codes.size()) {
                        code = codes.get(i + 1);
                        if (code.contains("RET")) {
                            i += 1;
                            optimizedMidCodes.add(code);
                        }
                    }
                    break;
                case "8":
                    funcRet(five, name2Node, nodes, orders);
                    break;
                case "12":
                    beq(five, name2Node, nodes, orders);
                    break;
                case "15":
                    leftArr(five, name2Node, nodes);
                    break;
                case "16":
                    rightArr(five, name2Node, nodes);
                    break;
                case "17":
                    getint(five, name2Node, nodes, orders);
                    break;
                case "18":
                    printf(five, name2Node, nodes, orders);
                    break;
                default:
                    break;
            }
        }
        deriveCodes(nodes, uses, orders);
        optimizedMidCodes.addAll(lasts);
    }

    private void deriveCodes(HashMap<Integer, Node> nodes, HashSet<String> uses, ArrayList<Integer> orders) {
//        System.out.println("orders:");
//        System.out.println(orders);
//        System.out.println("@@@@@@@@@@@@@@@@");
//        for (Node node : nodes.values()) {
//            System.out.println(node.op);
//            System.out.println(node.vars);
//        }
//        System.out.println(nodes.size());
//        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!");
//        if (orders.size() == 1 && nodes.size() == 2) {
//            int b = 1;
//        }
//        HashMap<Integer, Node> tempNodes = new HashMap<>();
//        // 给所有结点存一个临时备份
//        for (Integer key : nodes.keySet()) {
//            Node node = nodes.get(key);
//            Node node1 = node.myClone();
//            tempNodes.put(key, node1);
//        }
        ArrayList<Integer> sequence = new ArrayList<>(nodes.keySet());
        Node node = null;
//        while (tempNodes.size() != 0) {
//            ArrayList<Integer> arrayList = new ArrayList<>(tempNodes.keySet());
//            for (int i = arrayList.size() - 1; i >= 0; i--) {
//                int index = arrayList.get(i);
//                node = tempNodes.get(index);
//                if (node.fathers.size() == 0) {
//                    // 对于printf和getint要保持顺序
//                    if (node.op.equals("@getint") || node.op.equals("@printf") || node.op.equals("ret") || node.op.equals("beq")) {
//                        if (orders.get(orders.size() - 1).equals(node.index)) {
//                            orders.remove(orders.size() - 1);
//                            break;
//                        }
//                    } else {
//                        break;
//                    }
//                }
//            }
//            tempNodes.remove(node.index);
//            sequence.add(node.index);
//            Node lNode;
//            Node rNode;
//            if (node.leftSon != -1) {
//                lNode = tempNodes.get(node.leftSon);
//                lNode.removeFather(node.index);
//                node.leftSon = -1;
//            }
//            if (node.rightSon != -1) {
//                rNode = tempNodes.get(node.rightSon);
//                rNode.removeFather(node.index);
//                node.rightSon = -1;
//            }
//        }
        Node son1;
        Node son2;
        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        System.out.println(nodes.keySet());
        System.out.println(sequence);
        Collections.sort(sequence);
        for (int i = 0; i < sequence.size(); i++) {
            System.out.println(sequence.get(i));
            node = nodes.get(sequence.get(i));
            switch (node.op) {
                case "@getint":
                    String first = null;
                    for (String name : node.vars) {
                        if (first == null) {
                            first = name;
                            optimizedMidCodes.add("@getint " + first);
                            node.addExplicitVar(first);
                        } else {
                            if (!name.startsWith("#") || uses.contains(name)) {
                                optimizedMidCodes.add(name + " = " + first);
                                node.addExplicitVar(name);
                            }
                        }
                    }
                    break;
                case "@printf":
                    if (node.leftSon == -1) {
                        optimizedMidCodes.add("@printf " + node.vars.get(0));
                    } else {
                        Node son = nodes.get(node.leftSon);
                        optimizedMidCodes.add("@printf %d " + son.explicitVars.get(0));
                    }
                    break;
                case "ret":
                    if (node.leftSon != -1) {
                        Node son = nodes.get(node.leftSon);
                        optimizedMidCodes.add("ret " + son.explicitVars.get(0));
                    } else {
                        optimizedMidCodes.add("ret");
                    }
                    break;
                case "beq":
                    son1 = nodes.get(node.leftSon);
                    son2 = nodes.get(node.rightSon);
                    optimizedMidCodes.add("beq " + son1.explicitVars.get(0) + " " + son2.explicitVars.get(0) + " " + node.vars.get(0));
                    break;
                case "push":
                    if (node.rightSon == -1) {
                        Node pushSon = nodes.get(node.leftSon);
                        optimizedMidCodes.add("push " + pushSon.explicitVars.get(0));
                    } else {
                        Node pushSon1 = nodes.get(node.leftSon);
                        Node pushSon2 = nodes.get(node.rightSon);
                        optimizedMidCodes.add("push " + pushSon1.explicitVars.get(0) + "[" + pushSon2.explicitVars.get(0) + "]");
                    }
                    break;
                case "":
                    boolean bool = false;
                    String first1 = null;
                    for (String name : node.vars) {
                        if (first1 == null) {
                            first1 = name;
                        }
                        if (isDigit(name)) {
                            node.addExplicitVar(name);
                            bool = true;
                        }
                    }
                    for (String name : node.vars) {
                        if (!name.startsWith("#") || uses.contains(name)) {
                            node.addExplicitVar(name);
                            if (first1 != null && isDigit(first1) && !isDigit(name)) {
                                optimizedMidCodes.add(name + " = " + first1);
                            }
                            bool = true;
                        }
                    }
                    if (!bool) {
                        node.addExplicitVar(node.vars.get(0));
                    }
                    break;
                default:
                    boolean bool2 = false;
                    son1 = nodes.get(node.leftSon);
                    son2 = nodes.get(node.rightSon);
                    for (String name : node.vars) {
                        if (!name.startsWith("#") || uses.contains(name)) {
                            if (node.op.equals("[]")) {
                                optimizedMidCodes.
                                        add(name + " = " + son1.explicitVars.get(0)
                                                + "[" + son2.explicitVars.get(0) + "]");
                                node.addExplicitVar(name);
                            } else if (node.op.equals("[]=")) {
                                optimizedMidCodes.
                                        add(name + "[" + son1.explicitVars.get(0) + "]" + " = " + son2.explicitVars.get(0));
                                node.addExplicitVar(name);
                            } else {
                                optimizedMidCodes.
                                        add(name + " = " + son1.explicitVars.get(0)
                                                + " " + node.op + " " + son2.explicitVars.get(0));
                                node.addExplicitVar(name);
                            }
                            bool2 = true;
                        }
                    }
                    if (!bool2) {
                        if (node.op.equals("[]")) {
                            optimizedMidCodes.
                                    add(node.vars.get(0) + " = " + son1.explicitVars.get(0)
                                            + "[" + son2.explicitVars.get(0) + "]");
                            node.addExplicitVar(node.vars.get(0));
                        } else if (node.op.equals("[]=")) {
                            optimizedMidCodes.
                                    add(node.vars.get(0) + "[" + son1.explicitVars.get(0) + "]" + " = " + son2.explicitVars.get(0));
                            node.addExplicitVar(node.vars.get(0));
                        } else {

                            optimizedMidCodes.
                                    add(node.vars.get(0) + " = " + son1.explicitVars.get(0)
                                            + " " + node.op + " " + son2.explicitVars.get(0));
                            node.addExplicitVar(node.vars.get(0));
                        }
                    }
                    break;
            }
        }
    }

    private void exp(String[] five, HashMap<String, Integer> name2Node, HashMap<Integer, Node> nodes) {
        // 对于类似 z = RET，由于根据函数划分了DAG分析的区域，因此保证了每个区域只有一个RET
        // 表达式看作z = x op y
        String op = five[1];
        String x = five[3];
        String y = five[4];
        String z = five[2];
//        if (op.equals("+") && x.equals("0") && z.startsWith("#") && !y.equals("RET")) {
//            if (name2Node.containsKey(y)) {
//                int index = name2Node.get(y);
//                Node node = nodes.get(index);
//                node.addVars(z);
//                name2Node.put(z, index);
//            } else {
//                Node node = new Node(number);
//                node.addVars(y);
//                node.addVars(z);
//                name2Node.put(y, number);
//                name2Node.put(z, number);
//                nodes.put(number, node);
//                number += 1;
//            }
//        } else if (op.equals("+") && y.equals("0") && z.startsWith("#") && !x.equals("RET")) {
//            if (name2Node.containsKey(x)) {
//                int index = name2Node.get(x);
//                Node node = nodes.get(index);
//                node.addVars(z);
//                name2Node.put(z, index);
//            } else {
//                Node node = new Node(number);
//                node.addVars(x);
//                node.addVars(z);
//                name2Node.put(x, number);
//                name2Node.put(z, number);
//                nodes.put(number, node);
//                number += 1;
//            }
//        } else {
        if (op.equals("+") && x.equals("0") && z.startsWith("#") && y.startsWith("#")) {
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
                if (node.op.equals(op) && node.leftSon == nodeX.index && node.rightSon == nodeY.index) {
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
                node.leftSon = nodeX.index;
                node.rightSon = nodeY.index;
                nodeX.addFather(node.index);
                nodeY.addFather(node.index);
            }
        }
//        }
    }

    private void paraR(String[] five, HashMap<String, Integer> name2Node, HashMap<Integer, Node> nodes) {
        if (five[2].equals("")) {
            String paraR = five[1];
            Node paraNode;
            if (name2Node.containsKey(paraR)) {
                paraNode = nodes.get(name2Node.get(paraR));
            } else {
                paraNode = new Node(number);
                paraNode.addVars(paraR);
                name2Node.put(paraR, number);
                nodes.put(number, paraNode);
                number += 1;
            }
            Node node = new Node(number, "push"); // 设置一个push节点,该节点只会在这里出现一次
            node.addVars(paraR);
            nodes.put(number, node);
            number += 1;
            node.leftSon = paraNode.index;
            paraNode.addFather(node.index);
        } else {
            String arrName = five[1];
            String offset = five[2];
            Node node1;
            Node node2;
            // arrName永远重新创建一个节点
            node1 = new Node(number);
            node1.addVars(arrName);
            name2Node.put(arrName, number);
            nodes.put(number, node1);
            number += 1;
            if (name2Node.containsKey(offset)) {
                node2 = nodes.get(name2Node.get(offset));
            } else {
                node2 = new Node(number);
                node2.addVars(offset);
                name2Node.put(offset, number);
                nodes.put(number, node2);
                number += 1;
            }
            Node node = new Node(number, "push");
            node.addVars(arrName);
            node.addVars(offset);
            nodes.put(number, node);
            number += 1;
            node.leftSon = node1.index;
            node1.addFather(node.index);
            node.rightSon = node2.index;
            node2.addFather(node.index);
        }
    }

    private void funcRet(String[] five, HashMap<String, Integer> name2Node, HashMap<Integer, Node> nodes, ArrayList<Integer> orders) {
        String retName = five[1];
        if (!retName.equals("")) {
            Node paraNode;
            if (name2Node.containsKey(retName)) {
                paraNode = nodes.get(name2Node.get(retName));
            } else {
                paraNode = new Node(number);
                paraNode.addVars(retName);
                name2Node.put(retName, number);
                nodes.put(number, paraNode);
                number += 1;
            }
            Node node = new Node(number, "ret"); // 设置一个push节点,该节点只会在这里出现一次
            node.addVars(retName);
            nodes.put(number, node);
            orders.add(node.index);
            number += 1;
            node.leftSon = paraNode.index;
            paraNode.addFather(node.index);
        } else {
            Node node = new Node(number, "ret"); // 设置一个push节点,该节点只会在这里出现一次
            node.addVars(retName);
            nodes.put(number, node);
            orders.add(node.index);
            number += 1;
        }
    }

    private void beq(String[] five, HashMap<String, Integer> name2Node, HashMap<Integer, Node> nodes, ArrayList<Integer> orders) {
        String var1 = five[1];
        String var2 = five[2];
        String label = five[3];
        Node node1;
        Node node2;
        if (name2Node.containsKey(var1)) {
            node1 = nodes.get(name2Node.get(var1));
        } else {
            node1 = new Node(number);
            node1.addVars(var1);
            name2Node.put(var1, number);
            nodes.put(number, node1);
            number += 1;
        }
        if (name2Node.containsKey(var2)) {
            node2 = nodes.get(name2Node.get(var2));
        } else {
            node2 = new Node(number);
            node2.addVars(var2);
            name2Node.put(var2, number);
            nodes.put(number, node2);
            number += 1;
        }
        Node node = new Node(number, "beq");
        node.addVars(label);
        nodes.put(number, node);
        orders.add(node.index);
        number += 1;
        node.leftSon = node1.index;
        node1.addFather(node.index);
        node.rightSon = node2.index;
        node2.addFather(node.index);
    }

    private void leftArr(String[] five, HashMap<String, Integer> name2Node, HashMap<Integer, Node> nodes) {
        // 表达式看作 arrName = offset []= right
        String arrName = five[1];
        String offset = five[2];
        String right = five[3];
        Node node1;
        Node node2;
        if (name2Node.containsKey(offset)) {
            node1 = nodes.get(name2Node.get(offset));
        } else {
            node1 = new Node(number);
            node1.addVars(offset);
            name2Node.put(offset, number);
            nodes.put(number, node1);
            number += 1;
        }

        if (name2Node.containsKey(right)) {
            node2 = nodes.get(name2Node.get(right));
        } else {
            node2 = new Node(number);
            node2.addVars(right);
            name2Node.put(right, number);
            nodes.put(number, node2);
            number += 1;
        }
        boolean bool = false;
        for (Integer index : node1.fathers) {
            Node node = nodes.get(index);
            if (node.op.equals("[]=") && node.leftSon == node1.index && node.rightSon == node2.index) {
                node.addVars(arrName);
                name2Node.put(arrName, node.index);
                bool = true;
                break;
            }
        }
        if (!bool) {
            Node node = new Node(number, "[]=");
            node.addVars(arrName);
            name2Node.put(arrName, number);
            nodes.put(number, node);
            number += 1;
            // 添加关系
            node.leftSon = node1.index;
            node.rightSon = node2.index;
            node1.addFather(node.index);
            node2.addFather(node.index);
        }
    }

    private void rightArr(String[] five, HashMap<String, Integer> name2Node, HashMap<Integer, Node> nodes) {
        // 表达式看作 left = arrName [] offset
        String left = five[1];
        String arrName = five[2];
        String offset = five[3];
        Node node1;
        Node node2;
        if (name2Node.containsKey(arrName)) {
            node1 = nodes.get(name2Node.get(arrName));
        } else {
            node1 = new Node(number);
            node1.addVars(arrName);
            name2Node.put(arrName, number);
            nodes.put(number, node1);
            number += 1;
        }
        if (name2Node.containsKey(offset)) {
            node2 = nodes.get(name2Node.get(offset));
        } else {
            node2 = new Node(number);
            node2.addVars(offset);
            name2Node.put(offset, number);
            nodes.put(number, node2);
            number += 1;
        }
        boolean bool = false;
        for (Integer index : node1.fathers) {
            Node node = nodes.get(index);
            if (node.op.equals("[]") && node.leftSon == node1.index && node.rightSon == node2.index) {
                node.addVars(left);
                name2Node.put(left, node.index);
                bool = true;
                break;
            }
        }
        if (!bool) {
            Node node = new Node(number, "[]");
            node.addVars(left);
            name2Node.put(left, number);
            nodes.put(number, node);
            number += 1;
            // 添加关系
            node.leftSon = node1.index;
            node.rightSon = node2.index;
            node1.addFather(node.index);
            node2.addFather(node.index);
        }
    }

    private void getint(String[] five, HashMap<String, Integer> name2Node, HashMap<Integer, Node> nodes,
                        ArrayList<Integer> orders) {
        Node node = new Node(number, "@getint");
        node.addVars(five[1]);
        name2Node.put(five[1], number);
        nodes.put(number, node);
        orders.add(node.index);
        number += 1;
    }

    private void printf(String[] five, HashMap<String, Integer> name2Node, HashMap<Integer, Node> nodes,
                        ArrayList<Integer> orders) {
        if (five[1].equals("%d")) {
            Node node1;
            if (name2Node.containsKey(five[2])) {
                node1 = nodes.get(name2Node.get(five[2]));
            } else {
                node1 = new Node(number);
                node1.addVars(five[2]);
                name2Node.put(five[2], number);
                nodes.put(number, node1);
                number += 1;
            }
            Node node = new Node(number, "@printf");
            nodes.put(number, node);
            orders.add(number);
            number += 1;
            node.addVars(five[2]);
            node.leftSon = node1.index;
            node1.addFather(node.index);
        } else {
            Node node = new Node(number, "@printf");
            nodes.put(number, node);
            orders.add(number);
            number += 1;
            node.addVars(five[1]);
        }
    }

    private boolean isDigit(String s) {
        Pattern pattern = Pattern.compile("^[-+]?[\\d]*$");
        Matcher matcher = pattern.matcher(s);
        return matcher.matches();
    }

    private void updateCode(String[] five) {
        switch (five[0]) {
            case "1":
                exp1(five);
                break;
            case "6":
                paraPush1(five);
                break;
            case "8":
                ret1(five);
                break;
            case "12":
                beq1(five);
                break;
            case "15":
                leftArr1(five);
                break;
            case "16":
                rightArr1(five);
                break;
            case "17":
                getint1(five);
                break;
            case "18":
                printf1(five);
                break;
            default:
                break;
        }
    }

    private void exp1(String[] five) {
        HashSet<String> defSet = new HashSet<>();
        HashSet<String> useSet = new HashSet<>();
        String l = five[2];
        String r1 = five[3];
        String r2 = five[4];
        if (!isDigit(l)) {
            defSet.add(l);
        }
        if (!isDigit(r1)) {
            useSet.add(r1);
        }
        if (!isDigit(r2)) {
            useSet.add(r2);
        }
        ArrayList<HashSet<String>> arrayList = new ArrayList<>();
        arrayList.add(defSet);
        arrayList.add(useSet);
        codes1.add(arrayList);
    }

    private void paraPush1(String[] five) {
        // 实参压栈可以看做引用
        HashSet<String> defSet = new HashSet<>();
        HashSet<String> useSet = new HashSet<>();
        String r = five[1];
        System.out.println("push:" + r);
        if (r.equals("#t11")) {
            int b = 1;
        }
        if (!isDigit(r)) {
            useSet.add(r);
        }
        ArrayList<HashSet<String>> arrayList = new ArrayList<>();
        arrayList.add(defSet);
        arrayList.add(useSet);
        codes1.add(arrayList);
    }

    private void ret1(String[] five) {
        HashSet<String> defSet = new HashSet<>();
        HashSet<String> useSet = new HashSet<>();
        String r = five[1];
        if (!isDigit(r)) {
            useSet.add(r);
        }
        ArrayList<HashSet<String>> arrayList = new ArrayList<>();
        arrayList.add(defSet);
        arrayList.add(useSet);
        codes1.add(arrayList);
    }

    private void beq1(String[] five) {
        HashSet<String> defSet = new HashSet<>();
        HashSet<String> useSet = new HashSet<>();
        String r1 = five[1];
        String r2 = five[2];
        if (!isDigit(r1)) {
            useSet.add(r1);
        }
        if (!isDigit(r2)) {
            useSet.add(r2);
        }
        ArrayList<HashSet<String>> arrayList = new ArrayList<>();
        arrayList.add(defSet);
        arrayList.add(useSet);
        codes1.add(arrayList);
    }

    private void leftArr1(String[] five) {
        HashSet<String> defSet = new HashSet<>();
        HashSet<String> useSet = new HashSet<>();
        String l1 = five[1];
        String l2 = five[2];
        String r = five[3];
        if (!isDigit(l1)) {
            // 数组在左边，可以看做一个被使用变量 l1 = l2 [] r
            defSet.add(l1);
        }
        if (!isDigit(l2)) {
            // 虽然偏移在左边，但实际上是一个被引用变量
            useSet.add(l2);
        }
        if (!isDigit(r)) {
            useSet.add(r);
        }
        ArrayList<HashSet<String>> arrayList = new ArrayList<>();
        arrayList.add(defSet);
        arrayList.add(useSet);
        codes1.add(arrayList);
    }

    private void rightArr1(String[] five) {
        HashSet<String> defSet = new HashSet<>();
        HashSet<String> useSet = new HashSet<>();
        String l = five[1];
        String r1 = five[2];
        String r2 = five[3];
        if (!isDigit(l)) {
            defSet.add(l);
        }
        if (!isDigit(r1)) {
            useSet.add(r1);
        }
        if (!isDigit(r2)) {
            useSet.add(r2);
        }
        ArrayList<HashSet<String>> arrayList = new ArrayList<>();
        arrayList.add(defSet);
        arrayList.add(useSet);
        codes1.add(arrayList);
    }

    private void getint1(String[] five) {
        HashSet<String> defSet = new HashSet<>();
        HashSet<String> useSet = new HashSet<>();
        String r = five[1];
        if (!isDigit(r)) {
            useSet.add(r);
        }
        ArrayList<HashSet<String>> arrayList = new ArrayList<>();
        arrayList.add(defSet);
        arrayList.add(useSet);
        codes1.add(arrayList);
    }

    private void printf1(String[] five) {
        HashSet<String> defSet = new HashSet<>();
        HashSet<String> useSet = new HashSet<>();
        if (five[1].equals("%d")) {
            String r = five[2];
            if (!isDigit(r)) {
                useSet.add(r);
            }
        }
        ArrayList<HashSet<String>> arrayList = new ArrayList<>();
        arrayList.add(defSet);
        arrayList.add(useSet);
        codes1.add(arrayList);
    }

    private class Node {
        private int index;
        private ArrayList<String> vars = new ArrayList<>(); // 保证前面是不带#的变量，后面是带#的变量
        private String op;
        private ArrayList<Integer> fathers = new ArrayList<>();
        private int leftSon = -1;
        protected int rightSon = -1;
        private ArrayList<String> explicitVars = new ArrayList<>();
        // 在中间代码中会出现的变量，保证前面是不带#的变量，后面是带#的变量

        private Node(int index) {
            this.index = index;
            this.op = "";
        }

        private Node(int index, String op) {
            this.index = index;
            this.op = op;
        }

        private void addVars(String name) {
            if (!vars.contains(name)) {
                if (name.startsWith("#")) {
                    vars.add(name);
                } else {
                    int index;
                    for (index = 0; index < vars.size(); index++) {
                        if (vars.get(index).startsWith("#")) {
                            break;
                        }
                    }
                    if (index == vars.size()) {
                        vars.add(name);
                    } else {
                        vars.add(index, name);
                    }
                }
            }
        }

        private void addFather(int idx) {
            fathers.add(idx);
        }

        private void removeFather(int idx) {
            for (int i = 0; i < fathers.size(); i++) {
                if (fathers.get(i) == idx) {
                    fathers.remove(i);
                    i--;
                }
            }
        }

//        private void addSon(int idx) {
//            sons.add(idx);
//        }
//
//        private void removeSon(int idx) {
//            for (int i = 0; i < sons.size(); i++) {
//                if (sons.get(i) == idx) {
//                    sons.remove(i);
//                    i--;
//                }
//            }
//        }

        private void addExplicitVar(String explicitVar) {
            if (!explicitVars.contains(explicitVar)) {
                if (explicitVar.startsWith("#")) {
                    explicitVars.add(explicitVar);
                } else {
                    explicitVars.add(0, explicitVar);
                }
            }
        }


        public Node myClone() {
            Node node = new Node(this.index, this.op);
            node.vars = new ArrayList<>(this.vars);
            node.fathers = new ArrayList<>(this.fathers);
            node.leftSon = this.leftSon;
            node.rightSon = this.rightSon;
            return node;
        }
    }
}
