import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class ConflictMap {
    private final ArrayList<Block> blocks;
    private final HashMap<String, Var> conflictGraph = new HashMap<>();
    private final HashMap<String, Var> tempConflictGraph = new HashMap<>();
    private final HashMap<String, String> var2sReg = new HashMap<>();

    public ConflictMap(ArrayList<Block> blocks) {
        this.blocks = blocks;
        HashSet<String> varName = new HashSet<>();
        for (Block block : blocks) {
            ArrayList<ArrayList<HashSet<String>>> codes = block.getCodes();
            for (ArrayList<HashSet<String>> code : codes) {
                HashSet<String> defs = code.get(0);
                HashSet<String> uses = code.get(1);
                varName.addAll(defs);
                varName.addAll(uses);
            }
        }
        for (String name : varName) {
            Var var1 = new Var(name);
            Var var2 = new Var(name);
            conflictGraph.put(name, var1);
            tempConflictGraph.put(name, var2);
        }
        for (Block block : blocks) {
            HashSet<String> activeVars = new HashSet<>(block.getActiveOut());
            for (String activeName1 : activeVars) {
                Var var1 = conflictGraph.get(activeName1);
                Var var2 = tempConflictGraph.get(activeName1);
                for (String activeName2 : activeVars) {
                    if (!activeName1.equals(activeName2)) {
                        var1.addConflict(activeName2);
                        var2.addConflict(activeName2);
                    }
                }
            }
            ArrayList<ArrayList<HashSet<String>>> codes = block.getCodes();
            for (int i = codes.size() - 1; i >= 0; i--) {
                ArrayList<HashSet<String>> code = codes.get(i);
                HashSet<String> defs = code.get(0);
                HashSet<String> uses = code.get(1);
                activeVars.removeAll(defs); // 理论上defs中只有一个变量，因此删除defs放在找冲突前和找冲突后影响不大
                activeVars.addAll(uses); // 显然uses中的变量活跃，因此应该提前加入
                for (String defName : defs) {
                    Var var1 = conflictGraph.get(defName);
                    Var var2 = tempConflictGraph.get(defName);
                    for (String activeName : activeVars) {
                        if (!activeName.equals(defName)) {
                            Var var3 = conflictGraph.get(activeName);
                            Var var4 = tempConflictGraph.get(activeName);
                            var1.addConflict(activeName);
                            var2.addConflict(activeName);
                            var3.addConflict(defName);
                            var4.addConflict(defName);
                        }
                    }
                }
            }
        }
    }

    public HashMap<String, String> getVar2sReg() {
        return var2sReg;
    }

    public void graphColoring(int k) {
        boolean bool;
        ArrayList<String> sequence = new ArrayList<>();
        while (tempConflictGraph.size() != 0) {
            bool = false;
            for (String name : tempConflictGraph.keySet()) {
                Var var = tempConflictGraph.get(name);
                if (var.edges() < k) {
                    sequence.add(name);
                    for (Var var1 : tempConflictGraph.values()) {
                        var1.removeConflict(name);
                    }
                    tempConflictGraph.remove(name);
                    bool = true;
                    break;
                }
            }
            if (!bool) {
                int max = 0;
                // 找出边最多的变量
                for (String name : tempConflictGraph.keySet()) {
                    Var var = tempConflictGraph.get(name);
                    if (var.edges() > max) {
                        max = var.edges();
                    }
                }
                // 移除边最多的变量
                for (String name : tempConflictGraph.keySet()) {
                    Var var = tempConflictGraph.get(name);
                    if (var.edges() == max) {
                        for (Var var1 : tempConflictGraph.values()) {
                            var1.removeConflict(name);
                        }
                        tempConflictGraph.remove(name);
                        break;
                    }
                }
            }
        }
        for (int i = sequence.size() - 1; i >= 0; i--) {
            String name = sequence.get(i);
            Var var = conflictGraph.get(name);
            HashSet<Integer> deniedColors = new HashSet<>(); // 该点禁止被染的颜色
            for (int j = i + 1; j < sequence.size(); j++) {
                String name1 = sequence.get(j);
                if (var.containConflict(name1)) {
                    Var var1 = conflictGraph.get(name1);
                    deniedColors.add(var1.color);
                }
            }
            int color = -1;
            for (int c = 0; c < k; c++) {
                if (!deniedColors.contains(c)) {
                    color = c;
                    break;
                }
            }
            if (color == -1) {
                try {
                    throw new Exception("图着色算法失败！");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            var.color = color;
            var2sReg.put(name, "$s" + color); // 目前还没有考虑使用a寄存器
        }
    }

    private class Var {
        private final String name;
        private final HashSet<String> conflictVars = new HashSet<>();
        private int color; // 这里用来表示颜色

        private Var(String name) {
            this.name = name;
            this.color = -1;
        }

        private void addConflict(String conflictVar) {
            conflictVars.add(conflictVar);
        }

        private void removeConflict(String conflictVar) {
            conflictVars.remove(conflictVar);
        }

        private boolean containConflict(String name) {
            return conflictVars.contains(name);
        }

        public int edges() {
            return conflictVars.size();
        }
    }
}
