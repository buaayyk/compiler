import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class DU {
    // 到达-定义链
    private final ArrayList<Block> blocks;
    private final HashMap<String, HashSet<Chain>> name2Chains = new HashMap<>();
    private final HashMap<String, ArrayList<HashSet<String>>> idx2Code = new HashMap<>();
    private final ArrayList<Chain> chains = new ArrayList<>();

    public DU(ArrayList<Block> blocks) {
        this.blocks = blocks;
        for (int i = 0; i < blocks.size(); i++) {
            Block block = blocks.get(i);
            ArrayList<ArrayList<HashSet<String>>> codes = block.getCodes();
            for (int j = 0; j < codes.size(); j++) {
                ArrayList<HashSet<String>> code = new ArrayList<>(codes.get(j));
                idx2Code.put("" + i + " " + j, code);
            }
        }
    }

    public void analyse() {
        analyse1();
        analyse2();
    }

    private void analyse1() {
        for (int i = 0; i < blocks.size(); i++) {
            Block block = blocks.get(i);
            HashSet<String> arriveIn = block.getActiveIn();
            HashSet<String> lSet;
            HashSet<String> rSet;
            for (String idx : arriveIn) {
                ArrayList<HashSet<String>> code = idx2Code.get(idx);
                lSet = code.get(0);
                String name = (String) lSet.toArray()[0];
                HashSet<Chain> chains;
                if (name2Chains.containsKey(name)) {
                    chains = name2Chains.get(name);
                } else {
                    chains = new HashSet<>();
                }
                Chain chain = new Chain(name);
                chain.addFirst(idx);
                chains.add(chain);
                name2Chains.put(name, chains);
                for (int j = 0; j < block.getCodes().size(); j++) {
                    code = idx2Code.get("" + i + " " + j);
                    lSet = code.get(0);
                    rSet = code.get(1);
                    if (rSet.contains(chain.getName())) {
                        chain.addIdx("" + i + " " + j);
                    }
                    if (lSet.contains(chain.getName())) {
                        chain = new Chain(chain.getName());
                        chain.addFirst("" + i + " " + j);
                        chains = name2Chains.get(chain.getName());
                        chains.add(chain);
                        name2Chains.put(chain.getName(), chains);
                    }
                }
            }
        }
        // 将链合并
        for (String name : name2Chains.keySet()) {
            ArrayList<Chain> chains1 = new ArrayList<>(name2Chains.get(name));
            boolean bool;
            do {
                bool = false;
                Chain chain1;
                Chain chain2;
                for (int i = 0; i < chains1.size(); i++) {
                    chain1 = chains1.get(i);
                    for (int j = i + 1; j < chains1.size(); j++) {
                        chain2 = chains1.get(j);
                        if (chain1.intersection(chain2)) {
                            Chain chain = chain1.merge(chain2);
                            chains1.remove(chain1);
                            chains1.remove(chain2);
                            chains1.add(chain);
                            bool = true;
                            break;
                        }
                    }
                }
            } while (bool);
            chains.addAll(chains1);
        }
        name2Chains.clear();
        for (Chain chain : chains) {
            String name = chain.getName();
            HashSet<Chain> chains1;
            if (name2Chains.containsKey(name)) {
                chains1 = name2Chains.get(name);
            } else {
                chains1 = new HashSet<>();
            }
            chains1.add(chain);
            name2Chains.put(name, chains1);
        }
    }

    private void analyse2() {
        HashSet<Chain> activeChains = new HashSet<>(); // 当前正活跃的链
        for (int i = 0; i < blocks.size(); i++) {
            activeChains.clear();
            Block block = blocks.get(i);
            ArrayList<ArrayList<HashSet<String>>> codes = block.getCodes();
            ArrayList<HashSet<String>> code;
            for (String idx : block.getActiveOut()) {
                code = idx2Code.get(idx);
                String name = (String) code.get(0).toArray()[0];
                HashSet<Chain> hashSet = name2Chains.get(name);
                for (Chain chain : hashSet) {
                    if (chain.containFirst(idx)) {
                        activeChains.add(chain);
                        break;
                    }
                }
            }
            for (Chain chain : activeChains) {
                for (Chain chain1 : activeChains) {
                    if (chain != chain1) {
                        chain.addConflict(chain1);
                    }
                }
            }
            for (int j = codes.size() - 1; j >= 0; j--) {
                code = codes.get(j);
                HashSet<String> lSet = code.get(0);
                HashSet<String> rSet = code.get(1);
                for (String name : rSet) {
                    HashSet<Chain> hashSet = name2Chains.get(name);
                    for (Chain chain : hashSet) {
                        if (chain.containIdx("" + i + " " + j)) {
                            activeChains.add(chain);
                        }
                    }
                }
                if (lSet.size() != 0) {
                    Chain chain = null;
                    String name = (String) lSet.toArray()[0];
                    HashSet<Chain> hashSet = name2Chains.get(name);
                    for (Chain chain1 : hashSet) {
                        if (chain1.containFirst("" + i + " " + j)) {
                            activeChains.remove(chain1);
                            chain = chain1;
                            break;
                        }
                    }
                    for (Chain chain1 : activeChains) {
                        chain.addConflict(chain1);
                        chain1.addConflict(chain);
                    }
                }
            }
        }
    }

    public ArrayList<Chain> getChains() {
        return chains;
    }
}
