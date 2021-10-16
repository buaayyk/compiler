import java.util.ArrayList;

public class SymbolTable {
    private final ArrayList<Symbol> symbols = new ArrayList<>();
    private final ArrayList<Integer> indexes = new ArrayList<>();

    public void add(Symbol symbol) {
        symbols.add(symbol);
    }

    public void addNewLayer() {
        indexes.add(symbols.size());
    }

    public void removeCurrentLayer() {
        int index = indexes.get(indexes.size() - 1);
        indexes.remove(indexes.get(indexes.size() - 1));
        int length = symbols.size() - 1;
        while (length >= index) {
            symbols.remove(symbols.get(length));
            length -= 1;
        }
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
}
