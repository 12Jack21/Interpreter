package org.john.interpreter.Service.SemanticUtils;

import java.util.ArrayList;

public class FunctionTable {
    private ArrayList<FunctionVariable> table = new ArrayList<>();

    /* add new variable, if the variable has been defined throw error message*/
    public boolean addVariable(FunctionVariable var){
        int i = 0;
        while(i < table.size()){
            FunctionVariable varInTable = table.get(i);
            // 考虑到了level
            if(varInTable.getName().equals(var.getName())
                    && varInTable.getLevel() == var.getLevel()){
                return false;
            }
            i++;
        }
        table.add(var);
        //System.out.println("variable " + var.getName()+ " is added\n");
        return true;
    }

    /* ask for variable which has name name*/
    public FunctionVariable getVar(String name){
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
        }else{
            //addError("Error: no variables has been defined\n");
            return null;
        }
    }

    /* delete all variables in any level TODO 不存在多层级的函数声明*/
    public void deleteVariable(int level){
        int i = 0;
        while(i < table.size()){
            if(table.get(i).getLevel() == level)
                table.remove(i);
            i++;
        }
    }
}
