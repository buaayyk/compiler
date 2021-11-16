import java.util.ArrayList;

public class FuncSymbol extends Symbol {
    private final ArrayList<IdentSymbol> parameters = new ArrayList<>();
    private final String returnType;

    public FuncSymbol(String name, String returnType) {
        super(name, "func");
        this.returnType = returnType;
    }

    public void add(IdentSymbol identAttribute) {
        parameters.add(identAttribute);
    }

    public String getReturnType() {
        return returnType;
    }

    public int numberOfParameters() {
        return parameters.size();
    }

    public IdentSymbol getParameterAttribute(int index) {
        return parameters.get(index);
    }

    public ArrayList<IdentSymbol> getParameters() {
        return new ArrayList<>(parameters);
    }
}
