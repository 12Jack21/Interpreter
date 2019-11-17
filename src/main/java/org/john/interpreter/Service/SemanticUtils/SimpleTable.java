package org.john.interpreter.Service.SemanticUtils;

import java.util.ArrayList;


// 变量符号表
public class SimpleTable {
    private String errorMessage = "";
    private ArrayList<SimpleVariable> table = new ArrayList<>();
    public SimpleTable(){}
    public String getErrorMessage(){
        return errorMessage;
    }
    public void addError(String message){
        errorMessage += message;
    }

    /* add new variable, if the variable has been defined throw error message*/
    public boolean addVariable(SimpleVariable var){
        int i = 0;
        while(i < table.size()){
            SimpleVariable varInTable = table.get(i);
            if(varInTable.getName().equals(var.getName())
                && varInTable.getLevel() == var.getLevel()){
                addError("Error: variable " + var.getName() + " has been defined before\n");
                return false;
            }
            i++;
        }
        table.add(var);
        //System.out.println("variable " + var.getName()+ " is added\n");
        return true;
    }

    /* ask for variable which has name name*/
    public SimpleVariable getVar(String name){
        if(table.size() > 0){
            int i = table.size() - 1;
            while(i >= 0){
                if(table.get(i).getName().equals(name)){
                    return table.get(i);
                }
                i--;
            }
            addError("Error: variable " + name + " is not defined\n");
            return null;
        }else{
            addError("Error: variable " + name + " is not defined\n");
            //addError("Error: no variables has been defined\n");
            return null;
        }
    }

    /* delete all variables in any level*/
    public void deleteVariable(int level){
        int i = 0;
        while(i < table.size()){
            if(table.get(i).getLevel() == level)
                table.remove(i);
            i++;
        }
    }


}
