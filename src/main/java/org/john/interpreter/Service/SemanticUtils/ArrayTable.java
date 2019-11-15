package org.john.interpreter.Service.SemanticUtils;

import java.util.ArrayList;

public class ArrayTable {
    private String errorMessage = "";
    private ArrayList<ArrayVariable> table = new ArrayList<ArrayVariable>();
    public ArrayTable(){}
    public void addError(String message){
        errorMessage += message;
    }

    /* add new defined array */
    public boolean addVariable(ArrayVariable array){
        int i = 0;
        while(i < table.size()) {
            ArrayVariable varInTable = table.get(i);
            if (varInTable.getArrayName().equals(array.getArrayName())
                    && varInTable.getLevel() == array.getLevel()) {
                addError("Error: Array " + array.getArrayName() + " has been defined before\n");
                return false;
            }
            i++;
        }
        table.add(array);
        return true;
    }
    public String getErrorMessage(){
        return errorMessage;
    }
    public ArrayVariable getArray(String name){
        if(table.size() > 0){
            int i = table.size() - 1;
            while(i >= 0) {
                if(name.equals(table.get(i).getArrayName()))
                    return table.get(i);
                i--;
            }
            addError("Error: Array " + name + " is not defined\n");
        }else{
            addError("Error: Array " + name + " is not defined\n");
        }
        return null;
    }

    /* delete all arrays in any level */
    public void deleteArrays(int level){
        int i = 0;
        while(i < table.size()){
            if(table.get(i).getLevel() == level)
                table.remove(i);
            i++;
        }
    }
}
