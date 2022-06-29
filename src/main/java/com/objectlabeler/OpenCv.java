package com.objectlabeler;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.List;

import static org.opencv.core.Core.inRange;

public class OpenCv {

    public static List<Rect> BoundingBoxes(Mat img, Scalar lower, Scalar upper) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        //Creating the empty destination matrix
        Mat imgHSV = new Mat();
        //Converting the image to HSV
        Imgproc.cvtColor(img, imgHSV, Imgproc.COLOR_BGR2HSV);
        //Create mask
        Mat mask = new Mat();
        // Get items in mask range
        inRange(imgHSV, lower, upper, mask);

        List<MatOfPoint> contours = new ArrayList<>();
        // Found edges of items left after everything is masked
        Imgproc.findContours(mask, contours, new Mat(),
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        List<Rect> boxes = new ArrayList<>();
        // Make rectangle boxes for every contour found
        for (MatOfPoint contour : contours) {
            boxes.add(Imgproc.boundingRect(contour));
        }
        return boxes;
    }

    public static Mat img2Mat(BufferedImage image) {
        image = convertTo3ByteBGRType(image);
        byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        Mat mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC3);
        mat.put(0, 0, data);
        return mat;
    }

    private static BufferedImage convertTo3ByteBGRType(BufferedImage image) {
        BufferedImage convertedImage = new BufferedImage(image.getWidth(), image.getHeight(),
                BufferedImage.TYPE_3BYTE_BGR);
        convertedImage.getGraphics().drawImage(image, 0, 0, null);
        return convertedImage;
    }

}