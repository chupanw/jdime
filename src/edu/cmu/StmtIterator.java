package edu.cmu;

import AST.ASTNode;
import AST.Block;
import AST.Stmt;
import de.fosd.jdime.common.ASTNodeArtifact;

import java.util.ArrayList;

/**
 * @author: chupanw
 */
public class StmtIterator {
    private ASTNodeArtifact progNode;
    private ArrayList<ASTNodeArtifact> instList;
    private int curInst;

    public StmtIterator(ASTNodeArtifact astNode) {
        this.progNode = astNode;
        this.instList = new ArrayList<>();
        this.curInst = 0;
        parseInst(progNode);
    }

    public boolean hasNext() {
        if (curInst < instList.size()) {
            return true;
        }
        else{
            return false;
        }
    }

    public ASTNodeArtifact next(){
        curInst++;
        return instList.get(curInst-1);
    }

    public int getOldIndex(ASTNodeArtifact astNode) {
        int index = 0;
        for (int i = 0; i < instList.size(); i++) {
            if (astNode == instList.get(i)) {
                return index;
            }
            else if (!instList.get(i).isAdded()){
                index++;
            }
        }
        return -1;
    }


    private void parseInst(ASTNodeArtifact cur) {
        if (isStmt(cur)) {
            instList.add(cur);
        }
        else{
            for (int i = 0; i < cur.getNumChildren(); i++) {
                parseInst(cur.getChild(i));
            }
        }

    }

    private boolean isStmt(ASTNodeArtifact astNode) {
        if (astNode.getASTNode() instanceof Stmt && !(astNode.getASTNode() instanceof Block)) {
            return true;
        }
        return false;
    }
}