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
    private ASTNode baseProgNode;
    // Method Name -> Inst list
    private ArrayList<ASTNodeArtifact> instList;
    private int curInst;
    private String mtdName;
    private static HashMap<ASTNodeArtifact, ASTNode> baseMap = new HashMap<>();

    // Cache the history of parseInst
    private static HashMap<ASTNodeArtifact, HashMap<String, ArrayList<ASTNodeArtifact>>> instListCache = new HashMap<>();

    public StmtIterator(ASTNode baseProgNode, ASTNodeArtifact astNode, String mtdName) {
        if (instListCache.containsKey(astNode)) {
            if (instListCache.get(astNode).containsKey(mtdName)) {
                this.instList = instListCache.get(astNode).get(mtdName);
                this.curInst = 0;
                this.mtdName = mtdName;
                return;
            }
        }

        this.baseProgNode = baseProgNode;
        this.progNode = astNode;
        this.instList = new ArrayList<>();
        this.curInst = 0;
        this.mtdName = mtdName;
        ASTNodeArtifact mtdNode = getMethod(progNode);
        ASTNode baseMtdNode = getBaseMethod(baseProgNode);
        parseInst(baseMtdNode, mtdNode);

        if (instListCache.containsKey(astNode)) {
            HashMap<String, ArrayList<ASTNodeArtifact>> map = instListCache.get(astNode);
            map.put(mtdName, instList);
        }
        else{
            HashMap<String, ArrayList<ASTNodeArtifact>> map = new HashMap<>();
            map.put(mtdName, instList);
            instListCache.put(astNode, map);
        }
    }

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

    public static ASTNode getBaseNode(ASTNodeArtifact astNodeArtifact) {
        if (baseMap.containsKey(astNodeArtifact)) {
            return baseMap.get(astNodeArtifact);
        }
        return null;
    }

    public ASTNodeArtifact next() {
        curInst++;
        return instList.get(curInst - 1);
    }

//    public int getOldIndex(ASTNodeArtifact astNode) {
//        int index = 0;
//        for (int i = 0; i < instList.size(); i++) {
//            if (astNode == instList.get(i)) {
//                return index;
//            } else if (!instList.get(i).isAdded()) {
//                index++;
//            }
//        }
//        return -1;
//    }

    // old index means the corresponding index in the base AST
    public static int getOldIndex(ASTNodeArtifact astNode) {
        ASTNodeArtifact listNode = null;
        try {
            listNode = getNearestList(astNode);
        } catch (Exception e) {
            e.printStackTrace();
        }
        assert listNode != null;

        int index = -1;
        for (int i = 0; i < listNode.getNumChildren(); i++) {
            if (astNode == listNode.getChild(i)) {
                return index;
            } else if (!listNode.getChild(i).isAdded()) {
                index++;
            }
        }
        // should not happen
        return -1;
    }

    public static int getOldIndex(ASTNode astNode) {
        ASTNode listNode = null;
        try {
            listNode = getNearestList(astNode);
        } catch (Exception e) {
            e.printStackTrace();
        }
        assert listNode != null;
        for (int i = 0; i < listNode.getNumChild(); i++) {
            if (astNode == listNode.getChild(i)) {
                return i;
            }
        }
        // should not happen
        return -1;
    }

    public static ASTNode getNearestList(ASTNode astNode) throws Exception {
        if (astNode instanceof AST.List) {
            return astNode;
        } else {
            ASTNode parent = astNode.getParent();
            if (!(parent instanceof AST.List)) {
                throw new Exception("Parent of stmt is not AST.List");
            }
            return parent;
        }
    }

    // Used to calculate the old index
    public static ASTNodeArtifact getNearestList(ASTNodeArtifact astNodeArt) throws Exception {
        if (astNodeArt.getASTNode() instanceof AST.List) {
            return astNodeArt;
        } else {
            ASTNodeArtifact parent = astNodeArt.getParent();
            if (!(parent.getASTNode() instanceof AST.List)) {
                throw new Exception("Parent of stmt is not AST.List");
            }
            return parent;
        }
    }

    private ASTNodeArtifact getMethod(ASTNodeArtifact cur) {
        if (isMethod(cur)) {
            MethodDecl mtdDecl = (MethodDecl) cur.getASTNode();
            String signature = mtdDecl.getModifiers().prettyPrint() + " " + mtdDecl.getTypeAccess().prettyPrint() + " " + mtdDecl.signature();
            if (signature.equals(mtdName)) {
                return cur;
            }
        }
        for (int i = 0; i < cur.getNumChildren(); i++) {
            ASTNodeArtifact astNode = getMethod(cur.getChild(i));
            if (astNode != null) {
                return astNode;
            }
        }
        return null;
    }

    private ASTNode getBaseMethod(ASTNode cur) {
        if (isMethod(cur)) {
            MethodDecl mtdDecl = (MethodDecl) cur;
            String signature = mtdDecl.getModifiers().prettyPrint() + " " + mtdDecl.getTypeAccess().prettyPrint() + " " + mtdDecl.signature();
            if (signature.equals(mtdName)) {
                return cur;
            }
        }
        for (int i = 0; i < cur.getNumChild(); i++) {
            ASTNode astNode = getBaseMethod(cur.getChild(i));
            if (astNode != null) {
                return astNode;
            }
        }
        return null;
    }


    private void parseInst(ASTNodeArtifact cur) {
        if (isStmt(cur)) {
            instList.add(cur);
        }
        for (int i = 0; i < cur.getNumChildren(); i++) {
            parseInst(cur.getChild(i));
        }
    }

    private void parseInst(ASTNode baseCur, ASTNodeArtifact cur) {
        if (isStmt(cur)) {
            instList.add(cur);
            baseMap.put(cur, baseCur);
        }
        for (int i = 0, j = 0; i < cur.getNumChildren(); i++) {
            if (!cur.getChild(i).isAdded()) {
                parseInst(baseCur.getChild(j), cur.getChild(i));
                j++;
            } else {
                parseInst(baseCur, cur.getChild(i));
            }
        }
    }

    public static boolean isMethod(ASTNodeArtifact astNode) {
        if (astNode.getASTNode() instanceof MethodDecl) {
            return true;
        }
        return false;
    }

    public static boolean isMethod(ASTNode astNode) {
        if (astNode instanceof MethodDecl) {
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