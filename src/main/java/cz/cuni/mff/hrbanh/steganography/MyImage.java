package main.java.cz.cuni.mff.hrbanh.steganography;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public final class MyImage {
    // private BufferedImage img;
    public int[] pixels;
    private int width;
    private int height;
    private final String path;
    public int[] GetCapacity(){
        // in bits, the capacity of the image
        int[] capacitiesPerbPB = new int[8];
        int numPixels = pixels.length;
        for (int i = 1; i <= 8; i++){
            capacitiesPerbPB[i-1] = numPixels * 4 * i;
        }
        return capacitiesPerbPB;
    }
    private void Load(String path) throws IOException{
        File file = new File(path);
        BufferedImage img = ImageIO.read(file);
        width = img.getWidth();
        height = img.getHeight();
        pixels = img.getRGB(0, 0, width, height, null, 0, width);
    }
    public MyImage(String path) throws IOException{
        this.path = path;
        Load(path);
    }
    public void Write() throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0,0,width, height, pixels, 0, width);
        File output = new File(path+".png");
        ImageIO.write(img, "png", output);
    }
}
