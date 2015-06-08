package edu.cmu.nway;

import edu.cmu.NWayMerge;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * @author: chupanw
 */
public class SimpleTests {

    @Before
    public void setUp() throws IOException {
        File tmpOutput = new File("testOutput");
        tmpOutput.createNewFile();
    }

    @After
    public void tearDown() {
        File tmpOutput = new File("testOutput");
        if (tmpOutput.exists()) {
            tmpOutput.delete();
        }
    }

    @Test
    public void test1() {
        File baseFile = new File("testfiles/patch/test1/Base.java");
        File[] patchFiles = new File[3];
        patchFiles[0] = new File("testfiles/patch/test1/Patch1.java");
        patchFiles[1] = new File("testfiles/patch/test1/Patch2.java");
        patchFiles[2] = new File("testfiles/patch/test1/Patch3.java");
        NWayMerge merger = new NWayMerge(baseFile, patchFiles);
        try {
            merger.merge();
            String solContent = Utility.readContentFromFile(new File("testfiles/patch/test1/solution"));
            String testContent = Utility.readContentFromFile(new File("testOutput"));
            Assert.assertEquals(solContent, testContent);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void test2() {
        File baseFile = new File("testfiles/patch/test2/Base.java");
        File[] patchFiles = new File[3];
        patchFiles[0] = new File("testfiles/patch/test2/Patch1.java");
        patchFiles[1] = new File("testfiles/patch/test2/Patch2.java");
        patchFiles[2] = new File("testfiles/patch/test2/Patch3.java");
        NWayMerge merger = new NWayMerge(baseFile, patchFiles);
        try {
            merger.merge();
            String solContent = Utility.readContentFromFile(new File("testfiles/patch/test2/solution"));
            String testContent = Utility.readContentFromFile(new File("testOutput"));
            Assert.assertEquals(solContent, testContent);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test3() {
        File baseFile = new File("testfiles/patch/test3/Base.java");
        File[] patchFiles = new File[3];
        patchFiles[0] = new File("testfiles/patch/test3/Patch1.java");
        patchFiles[1] = new File("testfiles/patch/test3/Patch2.java");
        patchFiles[2] = new File("testfiles/patch/test3/Patch3.java");
        NWayMerge merger = new NWayMerge(baseFile, patchFiles);
        try {
            merger.merge();
            String solContent = Utility.readContentFromFile(new File("testfiles/patch/test3/solution"));
            String testContent = Utility.readContentFromFile(new File("testOutput"));
            Assert.assertEquals(solContent, testContent);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}