import java.util.HashSet;

public class Chain {
    private String name;
    private HashSet<String> idxs = new HashSet<>();
    private HashSet<String> firsts = new HashSet<>();
    private HashSet<Chain> conflicts = new HashSet<>();

    public Chain(String name) {
        this.name = name;
    }

    public Chain(String name, HashSet<String> firsts, HashSet<String> idxs) {
        this.name = name;
        this.firsts = new HashSet<>(firsts);
        this.idxs = new HashSet<>(idxs);
    }

    public String getName() {
        return name;
    }

    public void addFirst(String first) {
        firsts.add(first);
    }

    public void addIdx(String idx) {
        idxs.add(idx);
    }

    public void addConflict(Chain chain) {
        conflicts.add(chain);
    }

    public boolean intersection(Chain chain) {
        // 检查两条链是否有交集
        if (!chain.name.equals(this.name)) {
            return false;
        }
        HashSet<String> hashSet1 = new HashSet<>(this.firsts);
        HashSet<String> hashSet2 = new HashSet<>(chain.firsts);
        int size1 = hashSet1.size();
        int size2 = hashSet2.size();
        hashSet1.addAll(hashSet2);
        int size = hashSet1.size();
        if (size1 + size2 != size) {
            return true;
        }

        hashSet1 = new HashSet<>(this.idxs);
        hashSet2 = new HashSet<>(chain.idxs);
        size1 = hashSet1.size();
        size2 = hashSet2.size();
        hashSet1.addAll(hashSet2);
        size = hashSet1.size();
        return (size1 + size2 != size);
    }

    public Chain merge(Chain chain) {
        HashSet<String> hashSet1 = new HashSet<>(this.firsts);
        HashSet<String> hashSet2 = new HashSet<>(chain.firsts);
        hashSet1.addAll(hashSet2);
        HashSet<String> hashSet3 = new HashSet<>(this.idxs);
        HashSet<String> hashSet4 = new HashSet<>(chain.idxs);
        hashSet3.addAll(hashSet4);
        return new Chain(name, hashSet1, hashSet3);
    }

    //是定义语句
    public boolean containFirst(String first) {
        return firsts.contains(first);
    }

    //是引用语句
    public boolean containIdx(String idx) {
        return idxs.contains(idx);
    }
}
