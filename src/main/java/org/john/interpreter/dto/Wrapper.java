package org.john.interpreter.dto;

import org.john.interpreter.Service.ExecUtils.ASTNode;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Wrapper {
    private String lexiResult;
    private ASTNode astNode;
    private List<String> errors;
    private List<String> messages;
    private List<String> printList;

    public Wrapper() {
    }


    public Wrapper(String lexiResult, ASTNode astNode, List<String> errors, List<String> messages, List<String> printList) {
        this.lexiResult = lexiResult;
        this.astNode = astNode;
        this.errors = errors;
        this.messages = messages;
        this.printList = printList;
    }

    public List<String> getPrintList() {
        return printList;
    }

    public void setPrintList(List<String> printList) {
        this.printList = printList;
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
