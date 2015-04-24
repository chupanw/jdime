package edu.cmu;

import de.fosd.jdime.common.ASTNodeArtifact;
import de.fosd.jdime.common.ArtifactList;
import de.fosd.jdime.common.FileArtifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.strategy.MergeStrategy;
import edu.cmu.utility.GraphvizGenerator;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MultiMain {

    public static void main(String[] args) throws IOException {
        File baseFile = new File("testfiles/patch/Base.java");
        File patchFile1 = new File("testfiles/patch/Patch1.java");
        File patchFile2 = new File("testfiles/patch/Patch2.java");
        ASTNodeArtifact baseAST = new ASTNodeArtifact(new FileArtifact(baseFile));
        ASTNodeArtifact patchAST1 = new ASTNodeArtifact(new FileArtifact(patchFile1));
        ASTNodeArtifact patchAST2 = new ASTNodeArtifact(new FileArtifact(patchFile2));
        ArtifactList<FileArtifact> list1 = new ArtifactList<>();
        ArtifactList<FileArtifact> list2 = new ArtifactList<>();

        list1.add(new FileArtifact(baseFile));
        list1.add(new FileArtifact(patchFile1));
        list2.add(new FileArtifact(baseFile));
        list2.add(new FileArtifact(patchFile2));

        MergeContext context1 = new MergeContext();
        MergeContext context2 = new MergeContext();
        context1.setQuiet(true);
        context2.setQuiet(true);
        File output1 = File.createTempFile("output1", ".txt", new File("outputs"));
        File output2 = File.createTempFile("output2", ".txt", new File("outputs"));
        output1.createNewFile();
        output2.createNewFile();
        context1.setOutputFile(new FileArtifact(output1));
        context1.setInputFiles(list1);
        context1.setMergeStrategy(MergeStrategy.parse("structured"));

        context2.setOutputFile(new FileArtifact(output2));
        context2.setInputFiles(list2);
        context2.setMergeStrategy(MergeStrategy.parse("structured"));

        // initialize logger
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);

        try {
            de.fosd.jdime.Main.merge(context1);
            de.fosd.jdime.Main.merge(context2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Finished");

        ArrayList<ASTNodeArtifact> diffArray = new ArrayList<>();
        diffArray.add(context1.getDiffResult());
        diffArray.add(context2.getDiffResult());
        NASTMerge multiMerger = new NASTMerge(diffArray,baseAST);
        multiMerger.merge();
    }
}
