package cz.cuni.mff.hrbanh.steganography;

import java.io.IOException;

public class Main {
    /**
     * Hide a file in an image. Creates an instance of HiddenFile and calls its HideInImage method.
     * Then save the image.
     * @param imgPath path to image where data will be hidden
     * @param filePath path to file which will be hidden
     * @param bitsPerByte number of bits per byte used for encoding
     * @throws IOException if the image cannot be loaded or written
     */
    private static void Hide(String imgPath, String filePath, int bitsPerByte) throws IOException{
        HiddenFile hf = new HiddenFile(filePath, bitsPerByte);
        StegImage img = hf.HideInImage(imgPath);
        img.Write();
    }

    /**
     * Extract a file from an image. Calls the static ExtractFromImage method of HiddenFile. The extracted
     * file is saved in the current directory.
     * @param imgPath path to image from which the file will be extracted
     * @throws IOException if the image cannot be read or the file cannot be written
     */
    private static void ExtractFile(String imgPath) throws IOException{
        HiddenFile.ExtractFromImage(imgPath);
    }

    /**
     * Print the maximum size of files which can be hidden in a given image for every
     * possible bitsPerByte encoding (1-8).
     * @param imgPath path to image
     */
    private static void PrintCapacities(String imgPath) throws IOException{
        // print capacities of picture
        StegImage img = new StegImage(imgPath);
        int[] capacitiesPerbPB = img.GetCapacity();
        System.out.println("Capacities for image " + imgPath + ":");
        for (int i = 0; i < 8; i++){
            int cap = capacitiesPerbPB[i] / 8;
            String capacity = cap + " B";
            if (cap > 1024*10){
                cap /= 1024;
                capacity = cap+" kB";
                if (cap >= 1024*10){
                    cap /= 1024;
                    capacity = cap+" MB";
                }
            }
            System.out.println("  Capacity for " + (i+1) + " bits per byte: " + capacity);
        }
    }
    public static void main(String[] args){
        try {
            //Hide("small_white.png", "shakespeare.txt", 5);
            //PrintCapacities("stromovka.jpeg");
            //PrintCapacities("small_red.png");
            ExtractFile("small_red.png");
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}