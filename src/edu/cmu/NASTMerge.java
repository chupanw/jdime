package edu.cmu;

import de.fosd.jdime.common.ASTNodeArtifact;
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
        }
    }
}
