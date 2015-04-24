package edu.cmu.utility;

import de.fosd.jdime.common.ASTNodeArtifact;

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
}
