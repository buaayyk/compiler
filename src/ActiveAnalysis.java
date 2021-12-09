import java.util.ArrayList;

public class ActiveAnalysis {
    private ArrayList<Block> blocks;

    public ActiveAnalysis(ArrayList<Block> blocks) {
        this.blocks = blocks;
    }

    public void analyse() {
        boolean bool;
        do {
            bool = true;
            for (Block block : blocks) {
                // 不断更新in和out，直到in不变
                block.calActiveOut();
                bool = bool && block.calActiveIn();
            }
        } while (!bool);
    }
}
