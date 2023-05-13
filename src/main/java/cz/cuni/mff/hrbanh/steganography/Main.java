package cz.cuni.mff.hrbanh.steganography;

import java.io.IOException;
import javax.imageio.ImageIO;

public class Main {
    private void Hide(String imgPath, String filePath) throws IOException {
//        MyImage img = new MyImage(imgPath);
        HiddenFile hf = new HiddenFile(filePath, 1);
        hf.hideInImage(imgPath);
    }
    private static void GetCapacity(String path) throws IOException{
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
        HiddenFile hf = new HiddenFile("shakespeare.txt", 1);
        MyImage img = hf.hideInImage("stromovka.jpeg");
        img.Write();
//
//
//        GetCapacity("stromovka.jpeg");
//        GetCapacity("small_red.png");

//        HiddenFile.ExtractFromImage("stromovka.jpeg.jpeg");
    }
}