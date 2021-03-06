package org.john.interpreter.Service.SemanticUtils;

import java.util.ArrayList;

public class FunctionTable {
    private ArrayList<FunctionVariable> table = new ArrayList<>();

    /* add new variable, if the variable has been defined throw error message*/
    public boolean addVariable(FunctionVariable var) {
        int i = 0;
        while (i < table.size()) {
            FunctionVariable varInTable = table.get(i);
            // 考虑到了level
            if (varInTable.getName().equals(var.getName())) {
                return false;
            }
            i++;
        }
        table.add(var);
        return true;
    }

    /* ask for variable which has name name*/
    public FunctionVariable getVar(String name) {
        // 由于 level较大者为后来的 变量，从后往前遍历时会先拿到
        if (table.size() > 0) {
            int i = table.size() - 1;
            while (i >= 0) {
                if (table.get(i).getName().equals(name)) {
                    return table.get(i);
                }
                i--;
            }
            return null;
        } else
            return null;
    }
}
