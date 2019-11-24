package org.john.interpreter.dto;

import org.john.interpreter.Service.ExecUtils.ASTNode;

import java.util.List;

public class Wrapper {
    private String lexiResult;
    private ASTNode astNode;
    private List<String> errors;
    private List<String> messages;
    private List<String> outputList;

    public Wrapper() {
    }


    public Wrapper(String lexiResult, ASTNode astNode, List<String> errors, List<String> messages, List<String> outputList) {
        this.lexiResult = lexiResult;
        this.astNode = astNode;
        this.errors = errors;
        this.messages = messages;
        this.outputList = outputList;
    }

    public List<String> getOutputList() {
        return outputList;
    }

    public void setOutputList(List<String> outputList) {
        this.outputList = outputList;
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

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }
}
