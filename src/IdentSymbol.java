import java.util.ArrayList;

public class IdentSymbol extends Symbol {
    private final ArrayList<Integer> dimensions = new ArrayList<>();
    private final boolean isConst;
    private final ArrayList<Integer> values = new ArrayList<>();
    private int address; // 只能存相对于某一层的相对地址

    public IdentSymbol(String name, boolean isConst) {
        super(name, "ident");
        this.isConst = isConst;
        if (isConst) {
            values.add(0);
        }
    }

    public void setAddress(int address) {
        this.address = address;
    }

    public boolean isConst() {
        return isConst;
    }

    public void add(Integer dimension) {
        dimensions.add(dimension);
        if (isConst) {
            if (numberOfDimensions() == 1) {
                for (int i = 1; i < dimension(0); i++) {
                    values.add(0);
                }
            }
            if (numberOfDimensions() == 2) {
                for (int i = dimension(0); i < dimension(0) * dimension(1); i++) {
                    values.add(0);
                }
            }
        }
    }

    public void restoreValue(int value) {
        if (isConst) {
            values.set(0, value);
        }
    }

    public void restoreValue(int value, int i) {
        if (isConst) {
            values.set(i, value);
        }
    }

    public void restoreValue(int value, int i, int j) {
        if (isConst) {
            values.set(i * dimension(1) + j, value);
        }
    }

    public int getValue() {
        if (isConst) {
            return values.get(0);
        } else {
            return 0;
        }
    }

    public int getValue(int i) {
        if (isConst) {
            return values.get(i);
        } else {
            return 0;
        }
    }

    public int getValue(int i, int j) {
        if (isConst) {
            return values.get(i * dimension(1) + j);
        } else {
            return 0;
        }
    }


    public int numberOfDimensions() {
        return dimensions.size();
    }

    public Integer dimension(int index) {
        return dimensions.get(index);
    }
}
