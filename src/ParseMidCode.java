public class ParseMidCode {
    public String[] parseMidCode(String midCode) {
        // 保证返回一个五元式，第0位表示类型，后四位根据类型定义
        if (midCode == null) {
            return null;
        }
        String[] five = new String[5];
        String[] splitMidCodeNow = midCode.split(" ");
        for (int i = 0; i < 5; i++) {
            five[i] = null;
        }

        if (midCode.contains(" = ") && !(midCode.contains("[") && midCode.contains("]"))) {
            // 1 表达式 [操作符，左值，操作数1，操作数2]
            five[0] = "1";
            String[] ss = midCode.split(" = ");
            five[2] = ss[0];
            String[] ss1 = ss[1].split(" ");
            if (ss1.length == 1) {
                // 类似于t1 = t2这种表达式，那么可以理解为+ t1, 0, t2
                five[1] = "+";
                five[3] = "0";
                five[4] = ss1[0];
            } else if (ss1.length == 2) {
                five[1] = ss1[0];
                five[3] = "0";
                five[4] = ss1[1];
            } else {
                five[1] = ss1[1];
                five[3] = ss1[0];
                five[4] = ss1[2];
            }
        } else if (splitMidCodeNow[0].equals("int") || splitMidCodeNow[0].equals("void")) {
            // 2 函数声明 [函数类型，函数名]
            five[0] = "2";
            five[1] = splitMidCodeNow[0];
            five[2] = splitMidCodeNow[1].substring(0, splitMidCodeNow[1].length() - 2);
        } else if (splitMidCodeNow[0].equals("@func_begin")) {
            // 3 函数块开始 [函数名]
            five[0] = "3";
            five[1] = splitMidCodeNow[1];
        } else if (splitMidCodeNow[0].equals("@func_end")) {
            // 4 函数块结束 [函数名]
            five[0] = "4";
            five[1] = splitMidCodeNow[1];
        } else if (splitMidCodeNow[0].equals("para")) {
            // 5 函数形参 [形参名]
            five[0] = "5";
            five[1] = splitMidCodeNow[2];
        } else if (splitMidCodeNow[0].equals("push")) {
            // 6 实参压栈 [实参名,数组位置]
            five[0] = "6";
            if (splitMidCodeNow[1].contains("[")) {
                five[1] = splitMidCodeNow[1].split("\\[")[0];
                five[2] = splitMidCodeNow[1].
                        split("\\[")[1].substring(0, splitMidCodeNow[1].split("\\[")[1].length() - 1);
            } else {
                five[1] = splitMidCodeNow[1];
                five[2] = "";
            }
        } else if (splitMidCodeNow[0].equals("call")) {
            // 7  函数调用 [被调用函数名]
            five[0] = "7";
            five[1] = splitMidCodeNow[1];
        } else if (splitMidCodeNow[0].equals("ret")) {
            // 8 函数返回 [返回值]
            five[0] = "8";
            if (splitMidCodeNow.length == 2) {
                // 存在返回值
                five[1] = splitMidCodeNow[1];
            } else {
                five[1] = "";
            }
        } else if (splitMidCodeNow[0].equals("var")) {
            // 9 变量声明 [声明变量名]
            five[0] = "9";
            five[1] = splitMidCodeNow[2];
        } else if (splitMidCodeNow[0].equals("const")) {
            // 10 常量声明 [声明变量名]
            five[0] = "10";
            five[1] = splitMidCodeNow[2];
        } else if (splitMidCodeNow[0].equals("@label")) {
            // 11 标签 [标签名]
            five[0] = "11";
            five[1] = splitMidCodeNow[1];
        } else if (splitMidCodeNow[0].equals("beq")) {
            // TODO 可能后续需要关注
            // 12 比较跳转语句 [比较变量1，比较变量2，跳转标签]
            five[0] = "12";
            five[1] = splitMidCodeNow[1];
            five[2] = splitMidCodeNow[2];
            five[3] = splitMidCodeNow[3];
        } else if (splitMidCodeNow[0].equals("goto")) {
            // 13 直接跳转语句 [跳转标签]
            five[0] = "13";
            five[1] = splitMidCodeNow[1];
        } else if (splitMidCodeNow[0].equals("arr")) {
            // 14 数组定义 [数组名，数组长度]
            five[0] = "14";
            String[] ss = splitMidCodeNow[2].split("[\\[\\]]");
            five[1] = ss[0];
            five[2] = ss[1];
        } else if (midCode.contains(" = ") && midCode.contains("[") && midCode.contains("]")) {
            String[] ss = midCode.split(" = ");
            // 由于在midCodeGenerator中的操作，一个最多只有一边含有数组
            if (ss[0].contains("[")) {
                // 数组在左边
                // 15 数组被传值 [数组名，偏移，右值]
                five[0] = "15";
                five[1] = ss[0].split("[\\[\\]]")[0];
                five[2] = ss[0].split("[\\[\\]]")[1];
                five[3] = ss[1];
            } else {
                // 数组在右边，这种情况保证右边只有一个数组
                // 16 数组传值 [左值，数组名，偏移]
                five[0] = "16";
                five[1] = ss[0];
                five[2] = ss[1].split("[\\[\\]]")[0];
                five[3] = ss[1].split("[\\[\\]]")[1];
            }
        } else if (splitMidCodeNow[0].equals("@getint")) {
            // 17 getint [被赋值的变量名]
            five[0] = "17";
            five[1] = splitMidCodeNow[1];
        } else if (splitMidCodeNow[0].equals("@printf")) {
            // 18 printf [格式化字符串，变量]
            five[0] = "18";
            if (splitMidCodeNow[1].equals("%d")) {
                five[1] = "%d";
                five[2] = splitMidCodeNow[2];
            } else {
                five[1] = splitMidCodeNow[1];
                five[2] = "";
            }
        } else if (splitMidCodeNow[0].equals("@block_begin")) {
            five[0] = "19";
        } else if (splitMidCodeNow[0].equals("@block_end")) {
            five[0] = "20";
        }
        return five;
    }
}
