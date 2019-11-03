package org.john.interpreter.dto;

import org.john.interpreter.Service.ExecUtils.ASTNode;
import org.john.interpreter.Service.ExecUtils.LexiNode;

import java.util.List;

public class Wrapper {
    private List<LexiNode> lexiNodes;
    private ASTNode astNodes;

    public Wrapper() {
    }

    public Wrapper(List<LexiNode> lexiNodes, ASTNode astNodes) {
        this.lexiNodes = lexiNodes;
        this.astNodes = astNodes;
    }

    public List<LexiNode> getLexiNodes() {
        return lexiNodes;
    }

    public void setLexiNodes(List<LexiNode> lexiNodes) {
        this.lexiNodes = lexiNodes;
    }

    public ASTNode getAstNodes() {
        return astNodes;
    }

    public void setAstNodes(ASTNode astNodes) {
        this.astNodes = astNodes;
    }
}
