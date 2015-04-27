package edu.cmu.utility;

import AST.ASTNode;
import de.fosd.jdime.common.ASTNodeArtifact;
import de.fosd.jdime.common.ArtifactList;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This is a util class to generate the AST with Graphviz
 * @author: chupanw
 */
public class GraphvizGenerator {
    public static void main(String[] args) {

    }

    public static void toPDF(ASTNodeArtifact artifact, String output) throws IOException {
        File out = new File(output);
        FileWriter writer = new FileWriter(out);
        String tree = artifact.dumpGraphvizTree(true);
        writer.write("digraph AST {\n");
        writer.write(tree);
        writer.write("}\n");
        writer.close();
        Runtime rt = Runtime.getRuntime();
        String cmd = "dot -Tpdf " + output + " -o " + output + ".pdf";
        Process process = rt.exec(cmd);
        try {
            process.waitFor();
            if (process.exitValue() != 0) {
                System.out.println("Something went wrong");
            }
            else {
                System.out.println("Please find the output file here: " + output + ".pdf");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        out.delete();
    }

    public static void toPDF(ASTNode astNode, String output) throws IOException {
        ASTNodeArtifact rootArtifact = wrap(astNode);
        rootArtifact.forceRenumbering();
        toPDF(rootArtifact, output);
    }

    private static ASTNodeArtifact wrap(ASTNode astNode){
        ASTNodeArtifact astArtifact = new ASTNodeArtifact(astNode);
        ArtifactList<ASTNodeArtifact> childrenArtifact = new ArtifactList<>();
        for (int i = 0; i < astNode.getNumChild(); i++) {
            ASTNodeArtifact child = wrap(astNode.getChild(i));
            child.setParent(astArtifact);
            childrenArtifact.add(child);
        }
        astArtifact.setChildren(childrenArtifact);
        return astArtifact;
    }
}
