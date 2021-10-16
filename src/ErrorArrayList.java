import java.util.ArrayList;

public class ErrorArrayList {
    private ArrayList<String[]> errors = new ArrayList<>();

    public void add(String errorCol, String errorRow, String errorType) {
        String[] ss = new String[3];
        ss[0] = errorCol;
        ss[1] = errorRow;
        ss[2] = errorType;

        int col1 = Integer.parseInt(errorCol);
        int row1 = Integer.parseInt(errorRow);
        int index = 0;
        for (String[] strings : errors) {
            int col2 = Integer.parseInt(strings[0]);
            int row2 = Integer.parseInt(strings[1]);
            if (col1 > col2) {
                index += 1;
            } else if (col1 == col2) {
                if (row1 > row2) {
                    index += 1;
                } else {
                    break;
                }
            } else {
                break;
            }
        }
        if (index < errors.size()) {
            errors.add(index, ss);
        } else {
            errors.add(ss);
        }
    }

    public ArrayList<String[]> getErrors() {
        return new ArrayList<>(errors);
    }
}
