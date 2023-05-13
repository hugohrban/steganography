package main.java.cz.cuni.mff.hrbanh.steganography;

import java.io.IOException;

public class Main {
    private static void Hide(String imgPath, String filePath, int bitsPerByte) throws IOException {
        HiddenFile hf = new HiddenFile(filePath, bitsPerByte);
        MyImage img = hf.HideInImage(imgPath);
        img.Write();
    }
    private static void PrintCapacities(String path) throws IOException{
        // print capacities of picture
        MyImage img = new MyImage(path);
        int[] capacitiesPerbPB = img.GetCapacity();
        System.out.println("Capacities for file " + path + ":");
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
            System.out.println("Capacity for " + (i+1) + " bits per byte: " + capacity);
        }
    }
    private void ExtractFile(String path){
        // TODO
    }
    public static void main(String[] args) throws IOException {
        Hide("small_white.png", "shakespeare.txt", 5);
//        PrintCapacities("stromovka.jpeg");
//        PrintCapacities("small_red.png");
        HiddenFile.ExtractFromImage("small_white.png.png");
    }
}