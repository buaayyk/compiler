import java.util.ArrayList;

public class SymbolTable {
    private final ArrayList<Symbol> symbols = new ArrayList<>();
    private final ArrayList<Integer> indexes = new ArrayList<>();
    private final ArrayList<Integer> spaces = new ArrayList<>(); // 每一个运行栈目前开辟空间的大小
    private final ArrayList<Integer> ras = new ArrayList<>(); // 每一个运行栈对应的返回地址ra的值


    public int getCurrentRaAddress() {
        return ras.get(ras.size() - 1);
    }

    // 获得包括当前层向前数n个层目前占据的空间
    public int getLatestSpace(int n) {
        int space = 0;
        for (int i = spaces.size() - 1; i >= spaces.size() - n; i--) {
            space += spaces.get(i);
        }
        return space;
    }

    public void add(Symbol symbol) {
        symbols.add(symbol);
        int space = spaces.get(spaces.size() - 1);
        if (symbol instanceof IdentSymbol) {
            IdentSymbol identSymbol = (IdentSymbol) symbol;
            if (identSymbol.numberOfDimensions() == 0) {
                space += 4;
                if (identSymbol.getName().equals("$ra")) {
                    ras.set(ras.size() - 1, spaces.get(spaces.size() - 1) + 4);
                }
            } else if (identSymbol.numberOfDimensions() == 1) {
                space += 4 * identSymbol.dimension(0);
            } else {
                space += 4 * (identSymbol.dimension(0) * identSymbol.dimension(1));
            }
            identSymbol.setAddress(space);
        }
        spaces.set(spaces.size() - 1, space);
    }

    public void addNewLayer() {
        indexes.add(symbols.size());
        spaces.add(0);
        ras.add(0);
    }

    public void removeCurrentLayer() {
        int index = indexes.get(indexes.size() - 1);
        indexes.remove(indexes.get(indexes.size() - 1));
        int length = symbols.size() - 1;
        while (length >= index) {
            symbols.remove(symbols.get(length));
            length -= 1;
        }
        spaces.remove(spaces.size() - 1);
        ras.remove(ras.size() - 1);
    }

    public IdentSymbol searchIdentInCurrentLayer(String name) {
        int index = indexes.get(indexes.size() - 1);
        for (int i = symbols.size() - 1; i >= index; i--) {
            Symbol symbol = symbols.get(i);
            if (symbol.getName().equals(name) && symbol.getType().equals("ident")) {
                return (IdentSymbol) symbol;
            }
        }
        return null;
    }

    public IdentSymbol searchIdentInAllLayers(String name) {
        for (int i = symbols.size() - 1; i >= 0; i--) {
            Symbol symbol = symbols.get(i);
            if (symbol.getName().equals(name) && symbol.getType().equals("ident")) {
                return (IdentSymbol) symbol;
            }
        }
        return null;
    }

    public FuncSymbol searchFunc(String name) {
        for (int i = symbols.size() - 1; i >= 0; i--) {
            Symbol symbol = symbols.get(i);
            if (symbol.getName().equals(name) && symbol.getType().equals("func")) {
                return (FuncSymbol) symbol;
            }
        }
        return null;
    }

    public FuncSymbol searchLatestFunc() {
        for (int i = symbols.size() - 1; i >= 0; i--) {
            Symbol symbol = symbols.get(i);
            if (symbol.getType().equals("func")) {
                return (FuncSymbol) symbol;
            }
        }
        return null;
    }

    public int getIdentAddress(String name) {
        int offset = 0;
        boolean bool = false;
        for (int i = symbols.size() - 1; i >= 0; i--) {
            Symbol symbol = symbols.get(i);
            if (symbol instanceof IdentSymbol) {
                IdentSymbol identSymbol = (IdentSymbol) symbol;
                if (identSymbol.getName().equals(name)) {
                    bool = true;
                    break;
                }
                if (identSymbol.numberOfDimensions() == 0) {
                    offset += 4;
                } else if (identSymbol.numberOfDimensions() == 1) {
                    offset += 4 * identSymbol.dimension(0);
                } else {
                    offset += 4 * (identSymbol.dimension(0) * identSymbol.dimension(1));
                }
            }
        }
        if (!bool) {
            // 没找到标识符，则返回-1
            return -1;
        } else {
            return offset;
        }
    }
}
