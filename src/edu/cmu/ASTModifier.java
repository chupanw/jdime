package edu.cmu;

import AST.*;
import de.fosd.jdime.common.ASTNodeArtifact;
import de.fosd.jdime.common.ArtifactList;

import java.io.IOException;
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
    // cpwTODO: increment when dealing with new patches
    public static int numPatches = 1;
    // cpwTODO: reset when dealing with new patches
    public static boolean condCreated = false;
    // cpwTODO: reset when dealing with new patches
    public static boolean condNeeded = false;
    // store the nodes to be inserted
    public static HashMap<ASTNode<?>, HashSet<Pair>> insertionMap = new HashMap<>();

    public ASTModifier(ASTNodeArtifact node) {
        this.node = node;
    }

    private ASTNodeArtifact getClassDecl(){
        ASTNodeArtifact cur = node;
        while (!(cur.getASTNode() instanceof ClassDecl)){
            cur = cur.getParent();
            if (cur == null){
                return null;
            }
        }
        return cur;
    }

    private ASTNodeArtifact getBodyDeclList(ASTNodeArtifact classDecl){
        // According to the JastAddJ API, the last child of ClassDecl is BodyDecl
        int index = classDecl.getNumChildren() -1;
        return classDecl.getChild(index);
    }

    public void insertCondBoolean() {
        if (!condNeeded){
            return;
        }
        if (condCreated){
            return;
        }
        ASTNodeArtifact classDecl = getClassDecl();
        if (classDecl == null){
            return;
        }
        ASTNodeArtifact bodyDeclList = getBodyDeclList(classDecl);

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

        fieldDeclaration.setParent(bodyDeclList.getASTNode());
        bodyDeclList.getASTNode().insertChild(fieldDeclaration, 0);

        ASTNodeArtifact fieldDeclarationWrapper = createWrapperTree(fieldDeclaration);
        ArtifactList children = bodyDeclList.getChildren();
        children.add(0, fieldDeclarationWrapper);
        bodyDeclList.setChildren(children);
        fieldDeclarationWrapper.setParent(bodyDeclList);

        condCreated = true;
        //debug
        System.out.println("@Conditional is created");
    }

    private ASTNodeArtifact createWrapperTree(ASTNode<?> node){
        ASTNodeArtifact wrapperNode = new ASTNodeArtifact(node);
        ArtifactList<ASTNodeArtifact> childrenList = new ArtifactList<>();
        for (int i = 0; i < node.getNumChild(); i++) {
            ASTNodeArtifact childWrapper = createWrapperTree(node.getChild(i));
            childWrapper.setParent(wrapperNode);
            childrenList.add(childWrapper);
        }
        if (!childrenList.isEmpty()) {
            wrapperNode.setChildren(childrenList);
        }
        return wrapperNode;
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
}
