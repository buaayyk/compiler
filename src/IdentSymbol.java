import java.util.ArrayList;

public class IdentSymbol extends Symbol {
    private final ArrayList<Integer> dimensions = new ArrayList<>();
    private final boolean isConst;

    public IdentSymbol(String name, boolean isConst) {
        super(name, "ident");
        this.isConst = isConst;
    }

    public boolean isConst() {
        return isConst;
    }

    public void add(Integer dimension) {
        dimensions.add(dimension);
    }

    public int numberOfDimensions() {
        return dimensions.size();
    }

    public Integer dimension(int index) {
        return dimensions.get(index);
    }
}
