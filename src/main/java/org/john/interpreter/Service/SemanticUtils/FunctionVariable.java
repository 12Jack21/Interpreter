package org.john.interpreter.Service.SemanticUtils;

import org.john.interpreter.Service.ExecUtils.ASTNode;

import java.util.ArrayList;

public class FunctionVariable {
    private String type; //函数返回类型
    private String name; //函数名
    private ArrayList<Object> parameters; // 函数的形参列表
    private ASTNode pro_node; //函数体的子语法树根节点

    public FunctionVariable(String type, String name, ArrayList<Object> parameters, ASTNode pro_node) {
        this.type = type;
        this.name = name;
        this.parameters = parameters;
        this.pro_node = pro_node;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<Object> getParameters() {
        return parameters;
    }

    public void setParameters(ArrayList<Object> parameters) {
        this.parameters = parameters;
    }

    public ASTNode getPro_node() {
        return pro_node;
    }

    public void setPro_node(ASTNode pro_node) {
        this.pro_node = pro_node;
    }
}
