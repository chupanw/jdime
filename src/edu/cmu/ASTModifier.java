package edu.cmu;

import AST.*;
import de.fosd.jdime.common.ASTNodeArtifact;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Utility class to modify existing AST
 * @author: chupanw
 */
public class ASTModifier {
    ASTNodeArtifact node;
    public static int numPatches = 1;
    // store the nodes to be inserted
    public static HashMap<ASTNode<?>, HashSet<Pair>> insertionMap = new HashMap<>();

    public ASTModifier(ASTNodeArtifact node) {
        this.node = node;
    }

    private ASTNode<?> getClassDecl(){
        ASTNodeArtifact cur = node;
        while (!(cur.getASTNode() instanceof ClassDecl)){
            cur = cur.getParent();
        }
        return cur.getASTNode();
    }

    private ASTNode<?> getBodyDeclList(ASTNode<?> classDecl){
        // According to the JastAddJ API, the last child of ClassDecl is BodyDecl
        int index = classDecl.getNumChild() -1;
        return classDecl.getChild(index);
    }

    public void prepareCondBoolean() {
        ASTNode<?> classDecl = getClassDecl();
        ASTNode<?> bodyDeclList = getBodyDeclList(classDecl);

        if (containsKey(bodyDeclList)){
            return;
        }

        /* Create a new AST.FieldDeclaration */

        Modifiers modifiers = new Modifiers();
        Annotation annotation = new Annotation("annotation", new TypeAccess("Conditional"), new List<ElementValuePair>());
        Modifier staticModifier = new Modifier("static");
        modifiers.addModifier(annotation);
        modifiers.addModifier(staticModifier);

        PrimitiveTypeAccess typeAccess = new PrimitiveTypeAccess("boolean");

        String name = "patch" + numPatches;

        BooleanLiteral booleanLiteral = new BooleanLiteral(true);

        FieldDeclaration fieldDeclaration = new FieldDeclaration(modifiers, typeAccess, name, booleanLiteral);

//        bodyDeclList.insertChild(fieldDeclaration, 1);
        HashSet<Pair> hashSet = insertionMap.get(bodyDeclList);
        if (hashSet == null) {
            hashSet = new HashSet<>();
            insertionMap.put(bodyDeclList, hashSet);
        }
        hashSet.add(new Pair(fieldDeclaration, 0));

        //debug
        System.out.println("@Conditional is created");
    }

    public static boolean containsKey(ASTNode<?> n){
        Set<ASTNode<?>> set = insertionMap.keySet();
        Iterator<ASTNode<?>> itr = set.iterator();
        while (itr.hasNext()){
            ASTNode<?> node = itr.next();
            if (node.getId() == n.getId()){
                return true;
            }
        }
        return false;
    }

    public static HashSet<Pair> get(ASTNode<?> n){
        Set<ASTNode<?>> set = insertionMap.keySet();
        Iterator<ASTNode<?>> itr = set.iterator();
        while (itr.hasNext()){
            ASTNode<?> node = itr.next();
            if (node.getId() == n.getId()){
                return insertionMap.get(node);
            }
        }
        return null;
    }

    public static void insertCondBoolean(ASTNode<?> n){
        if (ASTModifier.containsKey(n)){
            HashSet<Pair> hashSet = ASTModifier.get(n);
            assert hashSet != null;
            Iterator<Pair> iter = hashSet.iterator();
            while (iter.hasNext()){
                Pair pair = iter.next();
                n.insertChild(pair.getNode(), pair.getIndex());
            }
        }
    }

    /**
     * Called only when there is a conflict. Surround the conflicting code with
     * an if block.
     */
    private void createCondBlock() {
    }
}
