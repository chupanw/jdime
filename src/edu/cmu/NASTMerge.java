package edu.cmu;

import AST.ASTNode;
import AST.Block;
import AST.Stmt;
import de.fosd.jdime.common.ASTNodeArtifact;
import de.fosd.jdime.common.ArtifactList;
import edu.cmu.utility.GraphvizGenerator;

import java.io.IOException;
import java.util.ArrayList;

/**
 * @author: chupanw
 */
public class NASTMerge {
    private ArrayList<ASTNodeArtifact> astArray;
    private ASTNodeArtifact base;

    public NASTMerge(ArrayList<ASTNodeArtifact> astArray, ASTNodeArtifact base) {
        this.astArray = astArray;
        this.base = base;
        rebuildASTs();
    }

    // Fix the incomplete handle of ConflictOp
    private void rebuildASTs(){
        for (int i = 0; i < astArray.size(); i++) {
            astArray.get(i).rebuildAST();
            rebuildAST(astArray.get(i));
            astArray.get(i).forceRenumbering();
            astArray.get(i).rebuildAST();
        }
    }

    private void rebuildAST(ASTNodeArtifact cur) {
        if (isStmt(cur)) {
            if (isConflict(cur)) {
                ASTNodeArtifact parent = cur.getParent();
                int index = parent.getChildren().indexOf(cur);
                ASTNodeArtifact cloneNode = clone(cur);
                parent.getChildren().add(index, cloneNode);
            }
        }
        else{
            for (int i = 0; i < cur.getNumChildren(); i++) {
                rebuildAST(cur.getChild(i));
            }
        }
    }

    private ASTNodeArtifact clone(ASTNodeArtifact node) {
        ASTNode cloneNodeAST = null;
        try {
            cloneNodeAST = node.getASTNode().clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        ASTNodeArtifact cloneNode = new ASTNodeArtifact(cloneNodeAST);
        ArtifactList<ASTNodeArtifact> children;
        ASTNode<?>[] astChildren;
        // Assume that original node represents Add
        node.setAdded();
        cloneNode.setDeleted();
        boolean conflict1 = false;
        boolean conflict2 = false;
        boolean conflict = false;
        if (node.getNumChildren() == 2){
            conflict1 = node.getChild(0).isAdded() && node.getChild(1).isDeleted();
            conflict2 = node.getChild(0).isDeleted() && node.getChild(1).isAdded();
            assert !(conflict1 && conflict2);
            conflict = conflict1 || conflict2;
        }
        if (conflict) {
            // only one type of conflict is possible
            children = new ArtifactList<>();
            astChildren = new ASTNode[1];
            if (conflict1) {
                ASTNodeArtifact cloneChild = clone(node.getChild(1));
                ASTNode cloneChildAST = cloneChild.getASTNode();
                node.removeChild(node.getChild(1));
                node.getASTNode().removeChild(1);
                cloneChild.setParent(cloneNode);
                cloneChildAST.setParent(cloneNode.getASTNode());
                children.add(cloneChild);
                astChildren[0] = cloneChildAST;
            }
            else if (conflict2) {
                ASTNodeArtifact cloneChild = clone(node.getChild(0));
                ASTNode cloneChildAST = cloneChild.getASTNode();
                node.removeChild(node.getChild(0));
                node.getASTNode().removeChild(0);
                cloneChild.setParent(cloneNode);
                cloneChildAST.setParent(cloneNode.getASTNode());
                children.add(cloneChild);
                astChildren[0] = cloneChildAST;
            }
            cloneNode.setChildren(children);
            cloneNode.getASTNode().setChildren(astChildren);
        }
        else {
            children = new ArtifactList<>();
            astChildren = new ASTNode[node.getNumChildren()];
            for (int i = 0; i < node.getNumChildren(); i++) {
                ASTNodeArtifact child = node.getChild(i);
                ASTNodeArtifact cloneChild = clone(child);
                // Assume that cloneNode represent Delete
                ASTNode cloneChildAST = cloneChild.getASTNode();
                cloneChild.setParent(cloneNode);
                cloneChildAST.setParent(cloneNode.getASTNode());
                children.add(cloneChild);
                astChildren[i] = cloneChildAST;
            }
            cloneNode.setChildren(children);
            cloneNode.getASTNode().setChildren(astChildren);
        }
        return cloneNode;
    }

    private boolean isConflict(ASTNodeArtifact cur) {
        boolean conflict = false;
        if (cur.getNumChildren() == 2) {
            ASTNodeArtifact leftChild = cur.getChild(0);
            ASTNodeArtifact rightChild = cur.getChild(1);
            if (leftChild.isAdded() && rightChild.isDeleted()) {
                conflict = true;
            }
            else if (leftChild.isDeleted() && rightChild.isAdded()) {
                conflict = true;
            }
        }
        if (conflict){
            return conflict;
        }
        else{
            for (int i = 0; i < cur.getNumChildren(); i++) {
                conflict |= isConflict(cur.getChild(i));
            }
        }
        return conflict;
    }

    private boolean isStmt(ASTNodeArtifact astNode) {
        if (astNode.getASTNode() instanceof Stmt && !(astNode.getASTNode() instanceof Block)) {
            return true;
        }
        else {
            return false;
        }
    }

    public void merge() {
        int num = astArray.size();

        for (int i = 0; i < num; i++) {
            ASTNodeArtifact ast = astArray.get(i);
            try {
                GraphvizGenerator.toPDF(ast, "diff" + i);
            } catch (IOException e) {
                e.printStackTrace();
            }
            ASTNode astNode = ast.getASTNode();
            ast.rebuildAST();
            System.out.println(astNode.prettyPrint());
        }
    }
}
