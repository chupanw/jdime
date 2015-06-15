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

        for (int i = 1; i < files.size(); i++) {
            File baseFile = new File(srcDir + files.get(i));

            int count = 0;
            ArrayList<File> patchArray = new ArrayList<>();
            ArrayList<Integer> patchNumArray = new ArrayList<>();
            for (int j = 0; j < 2000; j++) {
                File patchFile = new File(patchDir + "variant-" + j + files.get(i));
                if (patchFile.exists()) {
                    patchArray.add(patchFile);
                    patchNumArray.add(j);
                    count++;
                }
            }

            File[] patchFiles = new File[count];
            patchArray.toArray(patchFiles);

            NWayMerge merger = new NWayMerge(baseFile, patchFiles, patchNumArray);
            try {
                merger.merge();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
