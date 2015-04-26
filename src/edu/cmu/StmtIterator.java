package edu.cmu;

import AST.ASTNode;
import AST.Stmt;
import de.fosd.jdime.common.ASTNodeArtifact;

import java.util.ArrayList;

/**
 * @author: chupanw
 */
public class StmtIterator {
    private ASTNodeArtifact progNode;
    private ArrayList<String> instList;
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


    private void parseInst(ASTNodeArtifact cur) {
        if (isStmt(cur)) {
            instList.add(cur.prettyPrint());
        }
        else{
            for (int i = 0; i < cur.getNumChildren(); i++) {
                parseInst(cur.getChild(i));
            }
        }

    }

    private boolean isStmt(ASTNodeArtifact astNode) {
        if (astNode.getASTNode() instanceof Stmt) {
            return true;
        }
        return false;
    }
}