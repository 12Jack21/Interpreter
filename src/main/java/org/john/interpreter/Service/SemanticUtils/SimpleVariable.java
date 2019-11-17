package org.john.interpreter.Service.SemanticUtils;

public class FunctionVariable {
    private String name;
    private String type;
    private String value;
    private int level; // 作用域级别
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
}
