package com.objectlabeler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Dataset {
    File outputDirectory;
    String objectName;

    public Dataset(String object) {
        String directory = "/home/mig/.runelite/screenshots/kiiller689x/" + object + "_dataset"; // TODO make the directory actually dynamic
        this.objectName = object;
        this.outputDirectory = new File(directory);
    }

    public void mkdir() {
        this.outputDirectory.mkdirs();
    }

    public File writeImage(BufferedImage screenshot) throws IOException {
        File outputFile = new File(this.outputDirectory + "/" + this.objectName + "_" + System.currentTimeMillis() + ".jpg");
        ImageIO.write(Screenshot.ensureOpaque(screenshot), "JPG", outputFile);
        return outputFile;
    }

    public void writeImageLabels(File imageLabeled, StringBuilder labels) {
        try {
            File labelFile = new File(imageLabeled.getAbsolutePath().replace(".jpg", ".txt"));
            if (!labelFile.createNewFile()) {
                throw new IOException("File already exists, not overwriting!");
            }
            FileWriter myWriter = new FileWriter(labelFile);
            myWriter.write(String.valueOf(labels));
            myWriter.close();
        } catch (IOException e) {
            System.err.println("Failed to save labels: " + e);
            e.printStackTrace();
        }
    }

}
