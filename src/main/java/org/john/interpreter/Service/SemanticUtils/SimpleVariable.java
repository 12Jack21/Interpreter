package org.john.interpreter.Service.SemanticUtils;

import java.util.ArrayList;

public class SimpleVariable {
    private String name; //变量名
    private String type; //类型
    private String value; //值
    private int level; // 作用域级别
    private ArrayList<Integer> dimensionIndex = null; // 为了传递数组的维度信息，以供 scan 时使用
    public SimpleVariable(){}
    public SimpleVariable(String name, String type, String value, int level){
        this.name = name;
        this.type = type;
        this.value = value;
        this.level = level;
    }
    public void setName(String name){
        this.name = name;
    }
    public void setType(String type){
        this.type = type;
    }
    public void setValue(String value){
        this.value = value;
    }
    public void setLevel(int level){
        this.level = level;
    }
    public String getName(){
        return name;
    }
    public String getType(){
        return type;
    }
    public String getValue(){
        return value;
    }
    public int getLevel(){
        return level;
    }

    public ArrayList<Integer> getDimensionIndex() {
        return dimensionIndex;
    }

    public void setDimensionIndex(ArrayList<Integer> dimensionIndex) {
        this.dimensionIndex = dimensionIndex;
    }
}
