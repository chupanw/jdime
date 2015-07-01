package edu.cmu;

import AST.*;
import AST.List;
import de.fosd.jdime.common.ASTNodeArtifact;
import de.fosd.jdime.common.ArtifactList;
import edu.cmu.utility.GraphvizGenerator;
import org.apache.commons.lang3.ClassUtils;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * @author: chupanw
 */
public class NASTMerge {
    private ArrayList<ASTNodeArtifact> astArray;
    private ArrayList<Integer> patchNumArray;
    private ASTNode baseAST;
    // To-be-delete node -> patch set
    private HashMap<ASTNode, HashSet<Integer>> delMap;
    // To-be-insert node -> patchNum -> StmtList
    private HashMap<ASTNodeArtifact, HashMap<Integer, ArrayList<ASTNode>>> addMap;
    private HashMap<ASTNodeArtifact, Integer> patchNumMap;
    private HashSet<String> mtdNames;
    private static final Logger LOG = Logger.getLogger(ClassUtils
            .getShortClassName(NASTMerge.class));


    public NASTMerge(ArrayList<ASTNodeArtifact> astArray, ASTNodeArtifact base, ArrayList<Integer> patchNumArray) {
        this.delMap = new HashMap<>();
        this.addMap = new HashMap<>();
        this.patchNumMap = new HashMap<>();
        this.astArray = astArray;
        this.baseAST = base.getASTNode();
        this.mtdNames = new HashSet<>();
        this.patchNumArray = patchNumArray;

        getMethods(mtdNames, base);
        checkMtds();
        rebuildASTs();
    }


    public NASTMerge(ArrayList<ASTNodeArtifact> astArray, ASTNodeArtifact base) {
        this.delMap = new HashMap<>();
        this.addMap = new HashMap<>();
        this.patchNumMap = new HashMap<>();
        this.astArray = astArray;
        this.baseAST = base.getASTNode();
        this.mtdNames = new HashSet<>();
        this.patchNumArray = null;

        getMethods(mtdNames, base);
        checkMtds();
        rebuildASTs();
        try {
            GraphvizGenerator.toPDF(this.astArray.get(2), "patch2_noRebuild");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void getMethods(HashSet<String> mtdNames, ASTNodeArtifact curNode) {
        if (StmtIterator.isMethod(curNode)) {
            MethodDecl mtdDecl = (MethodDecl) curNode.getASTNode();
            String mtdSignature = mtdDecl.getModifiers().prettyPrint() + " " + mtdDecl.getTypeAccess().prettyPrint() + " " + mtdDecl.signature();
            mtdNames.add(mtdSignature);
        }
        for (int i = 0; i < curNode.getNumChildren(); i++) {
            getMethods(mtdNames, curNode.getChild(i));
        }
    }

    private void checkMtds(){
        for (int i = 0; i < astArray.size(); i++) {
            HashSet<String> names = new HashSet<>();
            getMethods(names, astArray.get(i));
            if (!names.equals(mtdNames)) {
                System.err.println("WARNING: Methods do not match in patch " + i);
            }
        }
    }

    public void merge() {
        int num = astArray.size();
        // Add @Conditional
        for (int i = 0; i < num; i++) {
            ASTNodeArtifact astArtifact = astArray.get(i);
            if (astArtifact.hasChanges()) {
                if (patchNumArray == null) {
                    addConditional(i);
                }
                else {
                    addConditional(patchNumArray.get(i));
                }
            }
        }

        Iterator<String> mtdItr = mtdNames.iterator();
        while (mtdItr.hasNext()) {
            String mtd = mtdItr.next();
            LOG.info(mtd);
            collectDel(mtd);
            applyDel();
            collectAdd(mtd);
            applyAdd(mtd);
            delMap.clear();
            addMap.clear();
        }

        if (LOG.isInfoEnabled()) {
            try {
                GraphvizGenerator.toPDF(baseAST, "merged");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            GraphvizGenerator.toPDF(baseAST, "result");
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(baseAST.prettyPrint());

        // For JUnit testing
        File testOutput = new File("testOutput");
        if (testOutput.exists()) {
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(testOutput));
                writer.write(baseAST.prettyPrint());
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void applyAdd(String mtdName) {
        // AST.List -> index of adding node -> adding node
        HashMap<ASTNode, HashMap<Integer, ArrayList<ASTNodeArtifact>>> levelMap = new HashMap<>();
        Iterator<ASTNodeArtifact> nodeIter = addMap.keySet().iterator();
        while (nodeIter.hasNext()) {
            ASTNodeArtifact node = nodeIter.next();

            try {
                ASTNodeArtifact listNodeArt = StmtIterator.getNearestList(node);
                if (listNodeArt.isAdded()) {
                    continue;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            ASTNode listNode = StmtIterator.getBaseNode(node);
            assert listNode != null;
            assert listNode instanceof AST.List;
            if (levelMap.containsKey(listNode)) {
                HashMap<Integer, ArrayList<ASTNodeArtifact>> map = levelMap.get(listNode);
                int index = StmtIterator.getOldIndex(node);
                if (map.containsKey(index)) {
                    ArrayList<ASTNodeArtifact> instList = map.get(index);
                    instList.add(node);
                }
                else {
                    ArrayList<ASTNodeArtifact> instList = new ArrayList<>();
                    instList.add(node);
                    map.put(index, instList);
                }
            }
            else {
                HashMap<Integer, ArrayList<ASTNodeArtifact>> map = new HashMap<>();
                int index = StmtIterator.getOldIndex(node);
                ArrayList<ASTNodeArtifact> instList = new ArrayList<>();
                instList.add(node);
                map.put(index, instList);
                levelMap.put(listNode, map);
            }
        }

        Iterator<ASTNode> listIter = levelMap.keySet().iterator();
        while (listIter.hasNext()) {
            ASTNode listNode = listIter.next();
            ASTNode astList = listNode;
            Set<Integer> keySet = levelMap.get(listNode).keySet();
            ArrayList<Integer> list = new ArrayList<>(keySet);
            Collections.sort(list);
            for (int i = list.size() - 1; i >= 0; i--) {
                int insertLoc = list.get(i);
                ArrayList<ASTNodeArtifact> instList = levelMap.get(astList).get(insertLoc);
                for (int j = 0; j < instList.size(); j++) {
                    int pos = patchNumMap.get(instList.get(j));
                    int patchNum;
                    if (patchNumArray != null) {
                        patchNum = patchNumArray.get(pos);
                    } else {
                        patchNum = pos;
                    }
                    Expr cond = new VarAccess("patch" + patchNum);
                    List<Stmt> bodyList = new List<>();
                    ASTNode inst = instList.get(j).getASTNode().fullCopy();
                    bodyList.add((Stmt) inst);
                    Block bodyBlock = new Block(bodyList);
                    IfStmt ifStmt = new IfStmt(cond, bodyBlock);
                    astList.insertChild(ifStmt, insertLoc + 1);
                    ifStmt.setParent(astList);
                }
            }
        }

    }

    private void collectAdd(String mtdName) {
        for (int i = 0; i < astArray.size(); i++) {
            StmtIterator stmtIter = new StmtIterator(baseAST, astArray.get(i), mtdName);
            while (stmtIter.hasNext()) {
                ASTNodeArtifact stmt = stmtIter.next();
                if (stmt.isAdded()) {
                    patchNumMap.put(stmt, i);
                    if (addMap.containsKey(stmt)) {
                        HashMap<Integer, ArrayList<ASTNode>> m = addMap.get(stmt);
                        if (m.containsKey(i)) {
                            ArrayList<ASTNode> s = m.get(i);
                            s.add(stmt.getASTNode());
                        } else {
                            ArrayList<ASTNode> s = new ArrayList<>();
                            s.add(stmt.getASTNode());
                            m.put(i, s);
                        }
                    } else {
                        HashMap<Integer, ArrayList<ASTNode>> m = new HashMap<>();
                        ArrayList<ASTNode> s = new ArrayList<>();
                        s.add(stmt.getASTNode());
                        m.put(i, s);
                        addMap.put(stmt, m);
                    }
                }
            }
        }
    }


    private void collectDel(String mtdName){
        for (int i = 0; i < astArray.size(); i++) {
            StmtIterator stmtIter = new StmtIterator(baseAST, astArray.get(i), mtdName);
            while (stmtIter.hasNext()) {
                ASTNodeArtifact stmt = stmtIter.next();
                ASTNode baseStmt = stmtIter.getBaseNode(stmt);
                if (stmt.isDeleted()) {
                    // Since it is a delete stmt, this should also be in the base AST.
                    assert baseStmt != null;
                    if (delMap.containsKey(baseStmt)) {
                        HashSet<Integer> patchSet = delMap.get(baseStmt);
                        patchSet.add(i);
                    }
                    else {
                        HashSet<Integer> patchSet = new HashSet<>();
                        patchSet.add(i);
                        delMap.put(baseStmt, patchSet);
                    }
                }
            }
        }
    }

    private void applyDel() {
        Iterator<ASTNode> nodeIter = delMap.keySet().iterator();
        while (nodeIter.hasNext()) {
            ASTNode node = nodeIter.next();
            HashSet<Integer> patchSet = delMap.get(node);
            Iterator<Integer> patchIter = patchSet.iterator();
            boolean isFirst = true;
            Expr cond = null;
            while (patchIter.hasNext()) {
                Integer patchNum = patchIter.next();
                if (patchNumArray != null) {
                    patchNum = patchNumArray.get(patchNum);
                }
                if (isFirst) {
                    cond = new LogNotExpr(new VarAccess("patch" + patchNum));
                    isFirst = false;
                } else {
                    LogNotExpr notExpr = new LogNotExpr(new VarAccess("patch" + patchNum));
                    cond = new AndLogicalExpr(cond, notExpr);
                }
            }
            ASTNode astList = null;
            try {
                astList = StmtIterator.getNearestList(node);
            } catch (Exception e) {
                e.printStackTrace();
            }
            assert astList != null;
            int pos = StmtIterator.getOldIndex(node);
            assert pos != -1;

            // Must remove first because removeChild() changes the childIndex field to -1,
            //  which may later cause NullPointerException
            List<Stmt> bodyList = new List<Stmt>();
            Stmt origStmt = (Stmt) astList.getChild(pos);
            astList.removeChild(pos);
            bodyList.add(origStmt);
            Block thenBody = new Block(bodyList);
            IfStmt ifStmt = new IfStmt(cond, thenBody);
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
        if (isStmt(cur) && isConflict(cur, cur)) {
            ASTNodeArtifact parent = cur.getParent();
            int index = parent.getChildren().indexOf(cur);
//                ASTNodeArtifact cloneNode = clone(cur);
            ASTNodeArtifact cloneNode = extractDel(cur);
            removeDel(cur);
            parent.getChildren().add(index, cloneNode);
            cloneNode.setParent(parent);
        } else {
            for (int i = 0; i < cur.getNumChildren(); i++) {
                rebuildAST(cur.getChild(i));
            }
        }
    }

    private ASTNodeArtifact extractDel(ASTNodeArtifact nodeArt) {
        if (nodeArt.isAdded()){
            return null;
        }
        else {
            ASTNode cloneNode = null;
            try {
                cloneNode = nodeArt.getASTNode().clone();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
            ASTNodeArtifact cloneNodeArt = new ASTNodeArtifact(cloneNode);
            cloneNodeArt.setDeleted();
            ArtifactList<ASTNodeArtifact> cloneChildrenArt = new ArtifactList<>();
            for (ASTNodeArtifact child : nodeArt.getChildren()) {
                ASTNodeArtifact art = extractDel(child);
                if (art != null) {
                    cloneChildrenArt.add(art);
                }
            }
            ASTNode[] cloneChildren = new ASTNode[cloneChildrenArt.size()];
            for (int i = 0; i < cloneChildrenArt.size(); i++) {
                cloneChildrenArt.get(i).setParent(cloneNodeArt);
                cloneChildrenArt.get(i).getASTNode().setParent(cloneNode);
                cloneChildren[i] = cloneChildrenArt.get(i).getASTNode();
            }
            cloneNode.setChildren(cloneChildren);
            cloneNodeArt.setChildren(cloneChildrenArt);
            return cloneNodeArt;
        }
    }

    private void removeDel(ASTNodeArtifact nodeArt) {
        nodeArt.setAdded();
        boolean hasDel = true;
        while (hasDel) {
            hasDel = false;
            for (ASTNodeArtifact child : nodeArt.getChildren()) {
                if (child.isDeleted()) {
                    ASTNode cld = child.getASTNode();
                    int index = child.getASTNode().getParent().getIndexOfChild(cld);
                    child.getASTNode().getParent().removeChild(index);
                    nodeArt.removeChild(child);
                    hasDel = true;
                    break;
                }
            }
        }
        for (ASTNodeArtifact child : nodeArt.getChildren()) {
            removeDel(child);
        }
    }

//    private ASTNodeArtifact clone(ASTNodeArtifact node) {
//        ASTNode cloneNodeAST = null;
//        try {
//            cloneNodeAST = node.getASTNode().clone();
//        } catch (CloneNotSupportedException e) {
//            e.printStackTrace();
//        }
//        ASTNodeArtifact cloneNode = new ASTNodeArtifact(cloneNodeAST);
//        ArtifactList<ASTNodeArtifact> cloneChildren;
//        ASTNode[] cloneChildrenAST;
//        // Assume that original node represents Add
//        node.setAdded();
//        cloneNode.setDeleted();
//        boolean conflict = false;
//        if (hasAddedChild(node) && hasDeletedChild(node)) {
//            conflict = true;
//        }
//        if (conflict) {
//            cloneChildren = new ArtifactList<>();
//            cloneChildrenAST = new ASTNode[countDeletedChildren(node)];
//            int cloneChildrenIndex = 0;
//            for (ASTNodeArtifact child : node.getChildren()) {
//                if (child.isDeleted()) {
//                    ASTNodeArtifact cloneChild = clone(child);
//                    ASTNode cloneChildAST = cloneChild.getASTNode();
//                    cloneChild.setParent(cloneNode);
//                    cloneChildAST.setParent(cloneNode.getASTNode());
//                    cloneChildren.add(cloneChild);
//                    cloneChildrenAST[cloneChildrenIndex] = cloneChildAST;
//                    cloneChildrenIndex++;
//                }
//            }
//            cloneNode.setChildren(cloneChildren);
//            cloneNode.getASTNode().setChildren(cloneChildrenAST);
//            boolean hasDel = true;
//            while (hasDel) {
//                hasDel = false;
//                for (ASTNodeArtifact child : node.getChildren()) {
//                    if (child.isDeleted()) {
//                        int index = node.getChildren().indexOf(child);
//                        node.getASTNode().removeChild(index);
//                        node.removeChild(child);
//                        hasDel = true;
//                        break;
//                    }
//                }
//            }
//        } else {
//            cloneChildren = new ArtifactList<>();
//            cloneChildrenAST = new ASTNode[node.getNumChildren()];
//            for (int i = 0; i < node.getNumChildren(); i++) {
//                ASTNodeArtifact child = node.getChild(i);
//                ASTNodeArtifact cloneChild = clone(child);
//                // Assume that cloneNode represent Delete
//                ASTNode cloneChildAST = cloneChild.getASTNode();
//                cloneChild.setParent(cloneNode);
//                cloneChildAST.setParent(cloneNode.getASTNode());
//                cloneChildren.add(cloneChild);
//                cloneChildrenAST[i] = cloneChildAST;
//            }
//            cloneNode.setChildren(cloneChildren);
//            cloneNode.getASTNode().setChildren(cloneChildrenAST);
//        }
//        return cloneNode;
//    }

    private ASTNodeArtifact nearestStmtParent(ASTNodeArtifact astNode) {
        if (isStmt(astNode)) {
            return astNode;
        }
        ASTNodeArtifact parent = astNode.getParent();
        while (!isStmt(parent)) {
            parent = parent.getParent();
        }
        return parent;
    }

    private boolean isConflict(ASTNodeArtifact cur, ASTNodeArtifact stmtNode) {
        boolean conflict = false;
        if (hasAddedChild(cur) && hasDeletedChild(cur)) {
            ASTNodeArtifact parent = nearestStmtParent(cur);
            if (parent == stmtNode) {
                return true;
            }
        }
        else {
            for (ASTNodeArtifact child : cur.getChildren()) {
                conflict |= isConflict(child, stmtNode);
            }
        }
        return conflict;
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

    private ASTNode getMethodDecl(ASTNode cur, String mtdName) {
        if (cur instanceof MethodDecl) {
            MethodDecl mtdDecl = (MethodDecl) cur;
            String mtdSignature = mtdDecl.getModifiers().prettyPrint() + " " + mtdDecl.getTypeAccess().prettyPrint() + " " + mtdDecl.signature();
            if (mtdSignature.equals(mtdName)){
                return cur;
            }
        }
        for (int i = 0; i < cur.getNumChild(); i++) {
            ASTNode ret = getMethodDecl(cur.getChild(i), mtdName);
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    private ASTNode getInstList(ASTNode methodDecl) {
        int numChildren = methodDecl.getNumChild();
        ASTNode astOPT = methodDecl.getChild(numChildren - 1);
        assert (astOPT instanceof Opt);
        // If there is no method body
        if (astOPT.getNumChild() == 0) {
            return null;
        }
        // otherwise, OPT node should have exactly one child
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

    public String getResult() {
        return baseAST.prettyPrint();
    }

}
