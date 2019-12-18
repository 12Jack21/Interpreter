package org.john.interpreter.Service.SemanticUtils;

import java.util.ArrayList;


// 变量符号表
public class SimpleTable {
    private ArrayList<SimpleVariable> table = new ArrayList<>();
    public SimpleTable(){}

    /* add new variable, if the variable has been defined throw error message*/
    public boolean addVariable(SimpleVariable var){
        int i = 0;
        while(i < table.size()){
            SimpleVariable varInTable = table.get(i);
            // 考虑到了level
            if(varInTable.getName().equals(var.getName())
                && varInTable.getLevel() == var.getLevel()){
                return false;
            }
            i++;
        }
        table.add(var);
        return true;
    }

    /* ask for variable which has name name*/
    public SimpleVariable getVar(String name){
        // 由于 level较大者为后来的 变量，从后往前遍历时会先拿到
        if(table.size() > 0){
            int i = table.size() - 1;
            while(i >= 0){
                if(table.get(i).getName().equals(name)){
                    return table.get(i);
                }
                i--;
            }
            return null;
        }else
            return null;
    }

    /* delete all variables in any specified level*/
    public void deleteVariable(int level){
        // 遍历过程中删除有副作用
        SimpleVariable[] variables = table.toArray(new SimpleVariable[0]);
        for (SimpleVariable var:variables){
            if (var.getLevel() == level)
                table.remove(var);
        }
    }


}
