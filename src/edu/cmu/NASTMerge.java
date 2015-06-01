package edu.cmu;

import AST.*;
import AST.List;
import de.fosd.jdime.common.ASTNodeArtifact;
import de.fosd.jdime.common.ArtifactList;
import edu.cmu.utility.GraphvizGenerator;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;

/**
 * @author: chupanw
 */
public class NASTMerge {
    private ArrayList<ASTNodeArtifact> astArray;
    private ASTNode baseAST;
    private HashMap<Integer, HashSet<Integer>> delMap;
    // insertLoc -> patchNum -> StmtList
    private HashMap<Integer, HashMap<Integer, ArrayList<ASTNode>>> addMap;

    public NASTMerge(ArrayList<ASTNodeArtifact> astArray, ASTNodeArtifact base) {
        delMap = new HashMap<>();
        addMap = new HashMap<>();
        this.astArray = astArray;
        baseAST = base.getASTNode();
        delMap = new HashMap<>();

        rebuildASTs();
        for (int i = 0; i < astArray.size(); i++) {
            ASTNodeArtifact ast = astArray.get(i);
            try {
                int patchNum = i;
                GraphvizGenerator.toPDF(ast, "diff" + patchNum);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void merge() {
        int num = astArray.size();
        // Add @Conditional
        for (int i = 0; i < num; i++) {
            ASTNodeArtifact astArtifact = astArray.get(i);
            if (astArtifact.hasChanges()) {
                addConditional(i);
            }
        }
        collectDel();
        applyDel();

        collectAdd();
        applyAdd();


        try {
            GraphvizGenerator.toPDF(baseAST, "merged");
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(baseAST.prettyPrint());

    }

    private void applyAdd() {
        ASTNode astMethodDecl = getMethodDecl(baseAST);
        ASTNode astList = getInstList(astMethodDecl);
        Set<Integer> keySet = addMap.keySet();
        ArrayList<Integer> list = new ArrayList<>(keySet);
        Collections.sort(list);
        for (int i = list.size() - 1; i >= 0; i--) {
            int insertLoc = list.get(i);
            HashMap<Integer, ArrayList<ASTNode>> m = addMap.get(insertLoc);
            Iterator<Integer> iter = m.keySet().iterator();
            while (iter.hasNext()) {
                int patchNum = iter.next();
                ArrayList<ASTNode> instList = m.get(patchNum);
                Expr cond = new VarAccess("patch" + patchNum);
                List<Stmt> bodyList = new List<>();
                for (int j = 0; j < instList.size(); j++) {
                    ASTNode inst = instList.get(j);
                    bodyList.add((Stmt) inst);
                }
                Block bodyBlock = new Block(bodyList);
                IfStmt ifStmt = new IfStmt(cond, bodyBlock);
                astList.insertChild(ifStmt, insertLoc+1);
                ifStmt.setParent(astList);
            }
        }
    }

    private void collectAdd() {
        for (int i = 0; i < astArray.size(); i++) {
            StmtIterator stmtIter = new StmtIterator(astArray.get(i));
            int insertLoc = 0;
            while (stmtIter.hasNext()) {
                ASTNodeArtifact stmt = stmtIter.next();
                if (stmt.isAdded()) {
                    if (addMap.containsKey(insertLoc)) {
                        HashMap<Integer, ArrayList<ASTNode>> m = addMap.get(insertLoc);
                        if (m.containsKey(i)) {
                            ArrayList<ASTNode> s = m.get(i);
                            s.add(stmt.getASTNode());
                        }
                        else {
                            ArrayList<ASTNode> s = new ArrayList<>();
                            s.add(stmt.getASTNode());
                            m.put(i, s);
                        }
                    }
                    else {
                        HashMap<Integer, ArrayList<ASTNode>> m = new HashMap<>();
                        ArrayList<ASTNode> s = new ArrayList<>();
                        s.add(stmt.getASTNode());
                        m.put(i, s);
                        addMap.put(insertLoc, m);
                    }
                }
                insertLoc = stmtIter.getOldIndex(stmt);
            }
        }
    }


    private void collectDel(){
        for (int i = 0; i < astArray.size(); i++) {
            StmtIterator stmtIter = new StmtIterator(astArray.get(i));
            while (stmtIter.hasNext()) {
                ASTNodeArtifact stmt = stmtIter.next();
                if (stmt.isDeleted()) {
                    int oldIndex = stmtIter.getOldIndex(stmt);
                    if (delMap.containsKey(oldIndex)) {
                        HashSet<Integer> patchSet = delMap.get(oldIndex);
                        patchSet.add(i);
                    }
                    else {
                        HashSet<Integer> patchSet = new HashSet<>();
                        patchSet.add(i);
                        delMap.put(oldIndex, patchSet);
                    }
                }
            }
        }
    }

    private void applyDel() {
        ASTNode astMethodDecl = getMethodDecl(baseAST);
        ASTNode astList = getInstList(astMethodDecl);
        Iterator<Integer> posIter = delMap.keySet().iterator();
        while (posIter.hasNext()) {
            int pos = posIter.next();
            HashSet<Integer> patchSet = delMap.get(pos);
            Iterator<Integer> patchIter = patchSet.iterator();
            boolean isFirst = true;
            Expr cond = null;
            while (patchIter.hasNext()) {
                Integer patchNum = patchIter.next();
                if (isFirst) {
                    cond = new LogNotExpr(new VarAccess("patch" + patchNum));
                    isFirst = false;
                }
                else {
                    LogNotExpr notExpr = new LogNotExpr(new VarAccess("patch" + patchNum));
                    cond = new AndLogicalExpr(cond, notExpr);
                }
            }
            List<Stmt> bodyList = new List<Stmt>();
            Stmt origStmt = (Stmt) astList.getChild(pos);
            bodyList.add(origStmt);
            Block thenBody = new Block(bodyList);
            IfStmt ifStmt = new IfStmt(cond, thenBody);
            astList.removeChild(pos);
            astList.insertChild(ifStmt, pos);
            ifStmt.setParent(astList);
            origStmt.setParent(bodyList);
        }
    }

    // Fix the incomplete handle of ConflictOp
    private void rebuildASTs() {
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
        } else {
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
        ArtifactList<ASTNodeArtifact> cloneChildren;
        ASTNode[] cloneChildrenAST;
        // Assume that original node represents Add
        node.setAdded();
        cloneNode.setDeleted();
        boolean conflict = false;
        if (hasAddedChild(node) && hasDeletedChild(node)) {
            conflict = true;
        }
        if (conflict) {
            cloneChildren = new ArtifactList<>();
            cloneChildrenAST = new ASTNode[countDeletedChildren(node)];
            int cloneChildrenIndex = 0;
            for (ASTNodeArtifact child : node.getChildren()) {
                if (child.isDeleted()) {
                    ASTNodeArtifact cloneChild = clone(child);
                    ASTNode cloneChildAST = cloneChild.getASTNode();
                    cloneChild.setParent(cloneNode);
                    cloneChildAST.setParent(cloneNode.getASTNode());
                    cloneChildren.add(cloneChild);
                    cloneChildrenAST[cloneChildrenIndex] = cloneChildAST;
                    cloneChildrenIndex++;
                }
            }
            cloneNode.setChildren(cloneChildren);
            cloneNode.getASTNode().setChildren(cloneChildrenAST);
            boolean hasDel = true;
            while (hasDel) {
                hasDel = false;
                for (ASTNodeArtifact child : node.getChildren()) {
                    if (child.isDeleted()) {
                        int index = node.getChildren().indexOf(child);
                        node.getASTNode().removeChild(index);
                        node.removeChild(child);
                        hasDel = true;
                        break;
                    }
                }
            }
        } else {
            cloneChildren = new ArtifactList<>();
            cloneChildrenAST = new ASTNode[node.getNumChildren()];
            for (int i = 0; i < node.getNumChildren(); i++) {
                ASTNodeArtifact child = node.getChild(i);
                ASTNodeArtifact cloneChild = clone(child);
                // Assume that cloneNode represent Delete
                ASTNode cloneChildAST = cloneChild.getASTNode();
                cloneChild.setParent(cloneNode);
                cloneChildAST.setParent(cloneNode.getASTNode());
                cloneChildren.add(cloneChild);
                cloneChildrenAST[i] = cloneChildAST;
            }
            cloneNode.setChildren(cloneChildren);
            cloneNode.getASTNode().setChildren(cloneChildrenAST);
        }
        return cloneNode;
    }

    private boolean isConflict(ASTNodeArtifact cur) {
        boolean conflict = false;
        if (hasAddedChild(cur) && hasDeletedChild(cur)) {
            conflict = true;
        }
        if (conflict) {
            return conflict;
        } else {
            for (int i = 0; i < cur.getNumChildren(); i++) {
                conflict |= isConflict(cur.getChild(i));
            }
        }
        return conflict;
    }

    private boolean hasAddedChild(ASTNodeArtifact cur) {
        for (ASTNodeArtifact child : cur.getChildren()) {
            if (child.isAdded()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasDeletedChild(ASTNodeArtifact cur) {
        for (ASTNodeArtifact child : cur.getChildren()) {
            if (child.isDeleted()) {
                return true;
            }
        }
        return false;
    }

    private int countDeletedChildren(ASTNodeArtifact cur){
        int count = 0;
        for (ASTNodeArtifact child : cur.getChildren()) {
            if (child.isDeleted()) {
                count++;
            }
        }
        return count;
    }

    private boolean isStmt(ASTNodeArtifact astNode) {
        if (astNode.getASTNode() instanceof Stmt && !(astNode.getASTNode() instanceof Block)) {
            return true;
        } else {
            return false;
        }
    }

    private ASTNode getMethodDecl(ASTNode cur) {
        if (cur instanceof MethodDecl) {
            return cur;
        }
        else{
            for (int i = 0; i < cur.getNumChild(); i++) {
                ASTNode ret = getMethodDecl(cur.getChild(i));
                if (ret != null) {
                    return ret;
                }
            }
        }
        return null;
    }

    private ASTNode getInstList(ASTNode methodDecl) {
        int numChildren = methodDecl.getNumChild();
        ASTNode astOPT = methodDecl.getChild(numChildren - 1);
        assert (astOPT instanceof Opt);
        assert (astOPT.getNumChild() == 1);
        ASTNode astBlock = astOPT.getChild(0);
        assert (astBlock instanceof Block);
        return ((Block) astBlock).getStmtList();
    }


    private ASTNode getClassDecl(ASTNode cur) {
        if (cur instanceof ClassDecl) {
            return cur;
        } else {
            for (int i = 0; i < cur.getNumChild(); i++) {
                ASTNode ret = getClassDecl(cur.getChild(i));
                if (ret != null) {
                    return ret;
                }
            }
        }
        return null;
    }

    private ASTNode getBodyDeclList(ASTNode classDecl) {
        // According to the JastAddJ API, the last child of ClassDecl is BodyDecl
        int index = classDecl.getNumChild() - 1;
        return classDecl.getChild(index);
    }

    private void addConditional(int patchNum) {
        ASTNode classDecl = getClassDecl(baseAST);
        ASTNode bodyDeclList = getBodyDeclList(classDecl);

        /* Create a new AST.FieldDeclaration */
        Modifiers modifiers = new Modifiers();
        Annotation annotation = new Annotation("annotation", new TypeAccess("Conditional"), new List<ElementValuePair>());
        Modifier staticModifier = new Modifier("static");
        modifiers.addModifier(annotation);
        modifiers.addModifier(staticModifier);

        PrimitiveTypeAccess typeAccess = new PrimitiveTypeAccess("boolean");

        String name = "patch" + patchNum;

        BooleanLiteral booleanLiteral = new BooleanLiteral(true);

        FieldDeclaration fieldDeclaration = new FieldDeclaration(modifiers, typeAccess, name, booleanLiteral);

        fieldDeclaration.setParent(bodyDeclList);
        bodyDeclList.insertChild(fieldDeclaration, 0);

    }

}
