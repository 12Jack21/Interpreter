package org.john.interpreter.dto;

import org.john.interpreter.Service.ExecUtils.ASTNode;
import org.springframework.boot.configurationprocessor.json.JSONObject;

public class Wrapper {
    private String lexiResult;
    private ASTNode astNode;

    public Wrapper() {
    }

    public Wrapper(String lexiResult, ASTNode astNode) {
        this.lexiResult = lexiResult;
        this.astNode = astNode;
    }

    public String getLexiResult() {
        return lexiResult;
    }

    public void setLexiResult(String lexiResult) {
        this.lexiResult = lexiResult;
    }

    public ASTNode getAstNode() {
        return astNode;
    }

    public void setAstNode(ASTNode astNode) {
        this.astNode = astNode;
    }

}
