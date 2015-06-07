package edu.cmu.nway;

import java.io.*;

/**
 * @author: chupanw
 */
public class Utility {
    public static String readContentFromFile(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        StringBuilder strBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            strBuilder.append(line);
        }
        return strBuilder.toString();
    }
}
