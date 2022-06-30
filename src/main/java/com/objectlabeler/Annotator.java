package com.objectlabeler;

import net.runelite.client.ui.DrawManager;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

public class Annotator {
    ScheduledExecutorService executor;
    DrawManager drawManager;

    public Annotator(ScheduledExecutorService executor, DrawManager drawManager) {
        this.executor = executor;
        this.drawManager = drawManager;
    }

    public void annotate(String[] objectsToAnnotate) {
        // TODO add an outline to the target classes (using NPC Indicator manually to add)
        // Fetch the screenshot we will annotate
        Consumer<Image> imageCallback = (img) ->
        {
            this.executor.submit(() -> {
                try {
                    labelFromImage(objectsToAnnotate, img);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        };

        this.drawManager.requestNextFrameListener(imageCallback);
    }

    private void labelFromImage(String[] objects, Image image) throws IOException {
        String objectName = objects[0]; // TODO support labeling more than 1 object
        Dataset dataset = new Dataset(objectName);
        dataset.mkdir(); // TODO cache the dataset already initialized to not always make them..

        // Get screenshot from game client image
        BufferedImage screenshot = Screenshot.capture(image);
        // Save screenshot
        File savedScreenshot = dataset.writeImage(screenshot);

        // Get bounding boxes from screenshot
        List<Rect> boxes = OpenCv.BoundingBoxes(screenshot, new Scalar(89, 130, 189), new Scalar(108, 255, 255));

        StringBuilder detectedCoordinates = new StringBuilder();
        double imageWidth = screenshot.getWidth();
        double imageHeight = screenshot.getHeight();

        // LABEL FORMAT: https://github.com/AlexeyAB/Yolo_mark/issues/60
        // Calculate annotation format for each box
        for (Rect rect : boxes) {
            double width = rect.br().x - rect.tl().x;
            double height = rect.br().y - rect.tl().y;
            if (width > 25 && height > 25) {
                double x_center = width / 2;
                double y_center = height / 2;

                double x_center_norm = x_center / imageWidth;
                double y_center_norm = y_center / imageHeight;
                double width_norm = width / imageWidth;
                double height_norm = height / imageHeight;

                String objectId = "0"; // TODO support more than just 1 object labelling at a time
                String annotation = String.format("%s %f %f %f %f\n", objectId, x_center_norm, y_center_norm, width_norm, height_norm);
                detectedCoordinates.append(annotation);
            }
        }

        // Write lines to file if detections were found
        if (detectedCoordinates.length() > 0) {
            dataset.writeImageLabels(savedScreenshot, detectedCoordinates);
            System.out.println("Image Labeled");
        }
    }
}