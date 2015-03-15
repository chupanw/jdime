package edu.cmu;

import de.fosd.jdime.common.ASTNodeArtifact;
import de.fosd.jdime.common.ArtifactList;
import de.fosd.jdime.common.FileArtifact;
import de.fosd.jdime.common.MergeContext;
import de.fosd.jdime.strategy.MergeStrategy;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        File baseFile = new File("testfiles/patch/Base.java");
        File leftFile = new File("testfiles/patch/Left.java");
        File rightFile = new File("testfiles/patch/Right.java");
        File patchFile = new File("testfiles/patch/Patch.java");
        ASTNodeArtifact baseAST = new ASTNodeArtifact(new FileArtifact(baseFile));
        ASTNodeArtifact leftAST = new ASTNodeArtifact(new FileArtifact(leftFile));
        ASTNodeArtifact rightAST = new ASTNodeArtifact(new FileArtifact(rightFile));
        ASTNodeArtifact patchAST = new ASTNodeArtifact(new FileArtifact(patchFile));
        ArtifactList<FileArtifact> list = new ArtifactList<>();


        list.add(new FileArtifact(baseFile));
        list.add(new FileArtifact(patchFile));

        MergeContext context = new MergeContext();
        context.setQuiet(true);
        File output = File.createTempFile("output", ".txt", new File("outputs"));
        output.createNewFile();
        context.setOutputFile(new FileArtifact(output));
        context.setInputFiles(list);
        context.setMergeStrategy(MergeStrategy.parse("structured"));

        // initialize logger
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.WARN);

        try {
            de.fosd.jdime.Main.merge(context);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        System.out.println("Finished");

        System.out.println("Resulting Output:");
        System.out.println("------------------\n");
        System.out.println(context.getOutputFile().getContent());

    }
}
