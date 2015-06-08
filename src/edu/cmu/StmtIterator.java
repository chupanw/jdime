package edu.cmu;

import AST.ASTNode;
import AST.Block;
import AST.MethodDecl;
import AST.Stmt;
import de.fosd.jdime.common.ASTNodeArtifact;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author: chupanw
 */
public class StmtIterator {
    private ASTNodeArtifact progNode;
    // Method Name -> Inst list
    private ArrayList<ASTNodeArtifact> instList;
    private int curInst;
    private String mtdName;

    public StmtIterator(ASTNodeArtifact astNode, String mtdName) {
        this.progNode = astNode;
        this.instList = new ArrayList<>();
        this.curInst = 0;
        this.mtdName = mtdName;
        ASTNodeArtifact mtdNode = getMethod(progNode);
        parseInst(mtdNode);
    }

    public boolean hasNext() {
        if (curInst < instList.size()) {
            return true;
        } else {
            return false;
        }
    }

    public ASTNodeArtifact next() {
        curInst++;
        return instList.get(curInst - 1);
    }

    public int getOldIndex(ASTNodeArtifact astNode) {
        int index = 0;
        for (int i = 0; i < instList.size(); i++) {
            if (astNode == instList.get(i)) {
                return index;
            } else if (!instList.get(i).isAdded()) {
                index++;
            }
        }
        return -1;
    }


    private ASTNodeArtifact getMethod(ASTNodeArtifact cur) {
        if (isMethod(cur)) {
            MethodDecl mtdDecl = (MethodDecl) cur.getASTNode();
            if (mtdDecl.signature().equals(mtdName)) {
                return cur;
            }
        }
        else{
            for (int i = 0; i < cur.getNumChildren(); i++) {
                ASTNodeArtifact astNode = getMethod(cur.getChild(i));
                if (astNode != null) {
                    return astNode;
                }
            }
        }
        return null;
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

    public static boolean isMethod(ASTNodeArtifact astNode) {
        if (astNode.getASTNode() instanceof MethodDecl) {
            return true;
        }
        return false;
    }


    public static boolean isStmt(ASTNodeArtifact astNode) {
        if (astNode.getASTNode() instanceof Stmt && !(astNode.getASTNode() instanceof Block)) {
            return true;
        }
        return false;
    }
}