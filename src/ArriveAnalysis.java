import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class ArriveAnalysis {
    private ArrayList<Block> blocks = new ArrayList<>();
    private HashMap<String, HashSet<String>> var2D = new HashMap<>(); // 变量到定义的映射

    public ArriveAnalysis(ArrayList<Block> blocks) {
        this.blocks = blocks;
        for (Block block : blocks) {
            ArrayList<ArrayList<HashSet<String>>> codes = block.getCodes();
            ArrayList<HashSet<String>> code;
            for (int i = 0; i < codes.size(); i++) {
                code = codes.get(i);
                HashSet<String> lSet = code.get(0); // 被定义的变量
                for (String varName : lSet) {
                    HashSet<String> hashSet;
                    if (var2D.containsKey(varName)) {
                        hashSet = var2D.get(varName);
                    } else {
                        hashSet = new HashSet<>();
                    }
                    hashSet.add("" + block.getIndex() + " " + i);
                    var2D.put(varName, hashSet);
                }
            }
        }
        for (Block block : blocks) {
            block.calKillD(var2D);
        }
        for (Block block : blocks) {
            block.calGen();
            block.calKill();
        }
    }

    public void analyse() {
        boolean bool;
        do {
            bool = true;
            for (Block block : blocks) {
                // 不断更新in和out，直到in不变
                block.calArriveIn();
                bool = bool && block.calArriveOut();
            }
        } while (!bool);
    }
}
