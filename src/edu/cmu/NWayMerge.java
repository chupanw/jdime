package edu.cmu;


import de.fosd.jdime.common.*;
import de.fosd.jdime.strategy.MergeStrategy;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @author: chupanw
 */
public class NWayMerge {
    private File baseFile;
    private File[] patchFiles;
    ArrayList<ASTNodeArtifact> diffArray;

    public NWayMerge(File baseFile, File[] patchesFile) {
        this.baseFile = baseFile;
        this.patchFiles = patchesFile;
        this.diffArray = new ArrayList<>();
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.WARN);
    }

    public void merge() throws IOException, InterruptedException {
        MergeContext[] contexts = new MergeContext[patchFiles.length];
        for (int i = 0; i < patchFiles.length; i++) {
            ArtifactList<FileArtifact> list = new ArtifactList<>();
            list.add(new FileArtifact(baseFile));
            list.add(new FileArtifact(patchFiles[i]));
            contexts[i] = new MergeContext();
            contexts[i].setQuiet(true);
            File output = File.createTempFile("output", ".txt", new File("outputs"));
            output.createNewFile();
            contexts[i].setOutputFile(new FileArtifact(output));
            contexts[i].setInputFiles(list);
            contexts[i].setMergeStrategy(MergeStrategy.parse("structured"));
            de.fosd.jdime.Main.merge(contexts[i]);
            System.out.println("Diff patch " + i + " finished");
            diffArray.add(contexts[i].getDiffResult());
        }
        ASTNodeArtifact baseAST = new ASTNodeArtifact(new FileArtifact(baseFile));
        NASTMerge multiMerger = new NASTMerge(diffArray, baseAST);
        multiMerger.merge();
    }
}
