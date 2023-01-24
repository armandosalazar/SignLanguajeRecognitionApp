package com.example.signlanguajerecognition;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ObjectDetector {
    private Interpreter interpreter;
    private List<String> labelList;
    private int INPUT_SIZE;
    private int PIXEL_SIZE = 3;
    private int IMAGE_MEAN = 0;
    private GpuDelegate gpuDelegate;
    private int height = 0;
    private int width = 0;

    public ObjectDetector(AssetManager assetManager, String modelPath, String labelPath, int inputSize) throws IOException {
        INPUT_SIZE = inputSize;
        Interpreter.Options options = new Interpreter.Options();
        gpuDelegate = new GpuDelegate();
        options.addDelegate(gpuDelegate);
        options.setNumThreads(4);
    }

    private List<String> loadLabelList(AssetManager assetManager, String labelPath) throws IOException {
        List<String> labelList = new ArrayList<>();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(assetManager.open(labelPath)));
        String line;

        while ((line = bufferedReader.readLine()) != null) {
            labelList.add(line);
        }
        bufferedReader.close();
        return labelList;
    }

    private ByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public Mat recognizeImage(Mat matImage) {
        Mat rotatedMatImage = new Mat();
        Mat a = matImage.t();
        Core.flip(a, rotatedMatImage, 1);
        a.release();

        Bitmap bitmap;
        bitmap = Bitmap.createBitmap(rotatedMatImage.cols(), rotatedMatImage.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rotatedMatImage, bitmap);
        height = bitmap.getHeight();
        width = bitmap.getWidth();

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);


        ByteBuffer byteBuffer = convertBitmapToByteBuffer(scaledBitmap);

        Object[] input = new Object[1];
        input[0] = byteBuffer;

        Map<Integer, Object> outputMap = new TreeMap<>();
        // we are not going to use this method of output
        // instead we create treemap of three array (boxes,score,classes)

        float[][][] boxes = new float[1][10][4];
        // 10: top 10 object detected
        // 4: there coordinate in image
        float[][] scores = new float[1][10];
        // stores scores of 10 object
        float[][] classes = new float[1][10];
        // stores class of object

        // add it to object_map;
        outputMap.put(0, boxes);
        outputMap.put(1, classes);
        outputMap.put(2, scores);

        // now predict
        interpreter.runForMultipleInputsOutputs(input, outputMap);
        // Before watching this video please watch my previous 2 video of
        //      1. Loading tensorflow lite model
        //      2. Predicting object
        // In this video we will draw boxes and label it with it's name

        Object value = outputMap.get(0);
        Object Object_class = outputMap.get(1);
        Object score = outputMap.get(2);

        // loop through each object
        // as output has only 10 boxes
        for (int i = 0; i < 10; i++) {
            float class_value = (float) Array.get(Array.get(Object_class, 0), i);
            float score_value = (float) Array.get(Array.get(score, 0), i);
            // define threshold for score

            // Here you can change threshold according to your model
            // Now we will do some change to improve app
            if (score_value > 0.5) {
                Object box1 = Array.get(Array.get(value, 0), i);
                // we are multiplying it with Original height and width of frame

                float top = (float) Array.get(box1, 0) * height;
                float left = (float) Array.get(box1, 1) * width;
                float bottom = (float) Array.get(box1, 2) * height;
                float right = (float) Array.get(box1, 3) * width;
                // draw rectangle in Original frame //  starting point    // ending point of box  // color of box       thickness
                Imgproc.rectangle(rotatedMatImage, new Point(left, top), new Point(right, bottom), new Scalar(0, 255, 0, 255), 2);
                // write text on frame
                // string of class name of object  // starting point                         // color of text           // size of text
                Imgproc.putText(rotatedMatImage, labelList.get((int) class_value), new Point(left, top), 3, 1, new Scalar(255, 0, 0, 255), 2);
            }

        }
        // select device and run

        // before returning rotate back by -90 degree

        // Do same here
        Mat b = rotatedMatImage.t();
        Core.flip(b, matImage, 0);
        b.release();
        // Now for second change go to CameraBridgeViewBase
        return matImage;

    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer;
        int quantity = 1;
        int sizeImages = INPUT_SIZE;

        if (quantity == 0) {
            byteBuffer = ByteBuffer.allocateDirect(sizeImages * sizeImages * 3);
        } else {
            byteBuffer = ByteBuffer.allocateDirect(4 * sizeImages * sizeImages * 3);
        }

        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[sizeImages * sizeImages];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;

        for (int i = 0; i < sizeImages; ++i) {
            for (int j = 0; j < sizeImages; ++j) {
                final int val = intValues[pixel++];
                if (quantity == 0) {
                    byteBuffer.put((byte) ((val >> 16) & 0xFF));
                    byteBuffer.put((byte) ((val >> 8) & 0xFF));
                    byteBuffer.put((byte) (val & 0xFF));
                } else {
                    // paste this
                    byteBuffer.putFloat((((val >> 16) & 0xFF)) / 255.0f);
                    byteBuffer.putFloat((((val >> 8) & 0xFF)) / 255.0f);
                    byteBuffer.putFloat((((val) & 0xFF)) / 255.0f);
                }
            }
        }
        return byteBuffer;

    }
}
