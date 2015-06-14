package edu.cmu;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @author: chupanw
 */
public class GenProg {

    public static void main(String[] args) {
        String srcDir = "/Users/chupanw/Projects/VarexJPatch/output-for-chupan/default";
        String patchDir = "/Users/chupanw/Projects/VarexJPatch/output-for-chupan/variants/";
        ArrayList<String> files = new ArrayList<>();

        files.add("/org/apache/commons/math/analysis/solvers/UnivariateRealSolverUtils.java");
        files.add("/org/apache/commons/math/distribution/AbstractContinuousDistribution.java");
        files.add("/org/apache/commons/math/distribution/NormalDistributionImpl.java");
        files.add("/org/apache/commons/math/special/Erf.java");
        files.add("/org/apache/commons/math/special/Gamma.java");

        File baseFile = new File(srcDir + files.get(0));

        int count = 0;
        ArrayList<File> patchArray = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            File patchFile = new File(patchDir + "variant-" + i + files.get(0));
            if (patchFile.exists()) {
                patchArray.add(patchFile);
                count++;
            }
        }

        File[] patchFiles = new File[count];
        patchArray.toArray(patchFiles);

        NWayMerge merger = new NWayMerge(baseFile, patchFiles);
        try {
            merger.merge();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
