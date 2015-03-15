package edu.cmu;

import AST.ASTNode;

/**
 * @author: chupanw
 */
public class Pair {
    private ASTNode node;
    private Integer index;

    public Pair(ASTNode node, Integer index) {
        this.node = node;
        this.index = index;
    }

    public ASTNode getNode() {
        return node;
    }

    public Integer getIndex() {
        return index;
    }
}
