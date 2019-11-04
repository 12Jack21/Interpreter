package org.john.interpreter.dto;

import org.john.interpreter.Service.ExecUtils.ASTNode;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import java.util.List;

public class Wrapper {
    private String lexiResult;
    private ASTNode astNode;
    private List<String> errors;

    public Wrapper() {
    }

    public Wrapper(String lexiResult, ASTNode astNode, List<String> errors) {
        this.lexiResult = lexiResult;
        this.astNode = astNode;
        this.errors = errors;
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

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
