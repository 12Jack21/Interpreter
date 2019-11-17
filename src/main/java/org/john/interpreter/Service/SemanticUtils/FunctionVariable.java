package org.john.interpreter.Service.SemanticUtils;

import org.john.interpreter.Service.ExecUtils.ASTNode;

import java.util.ArrayList;

public class FunctionVariable {
    private String name; // 需要 level吗

    private ArrayList<Object> parameters; // object instanceof Class 来判断是哪种变量
    private ASTNode F_node;

    public FunctionVariable(String name, ArrayList<Object> parameters, ASTNode f_node) {
        this.name = name;
        this.parameters = parameters;
        F_node = f_node;
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

    public ASTNode getF_node() {
        return F_node;
    }

    public void setF_node(ASTNode f_node) {
        F_node = f_node;
    }
}
