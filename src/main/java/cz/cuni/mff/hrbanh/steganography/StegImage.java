package cz.cuni.mff.hrbanh.steganography;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Class for storing data of an image into which we will encode a hidde file and subsequently save it.
 */
public final class StegImage {
    /**
     * Pixels of the underlying BufferedImage. Values are in the ARGB color space.
     */
    public int[] pixels;

    /**
     * Width of image in pixels
     */
    private int width;
    /**
     * Height of image in pixels
     */
    private int height;
    private final String path;

    /**
     * Get the maximal size of a file which can be stored in this image
     * for every possible bitsPerByte encoding
     * @return int[8] - max file sizes.
     */
    public int[] GetCapacity(){
        int[] capacitiesPerbPB = new int[8];
        int numPixels = pixels.length - 28;                 // not including magic number
        for (int i = 1; i <= 8; i++){
            capacitiesPerbPB[i-1] = numPixels * 4 * i;      // in bits
            capacitiesPerbPB[i-1] /= 8;                     // in bytes
            capacitiesPerbPB[i-1] -= 259;                   // not rest of header
        }
        return capacitiesPerbPB;                            // in bytes
    }

    /**
     * Get the pixels of the image
     * @param path path to the image
     * @throws IOException if the image cannot be loaded
     */
    private void Load(String path) throws IOException{
        File file = new File(path);
        BufferedImage img = ImageIO.read(file);
        width = img.getWidth();
        height = img.getHeight();
        pixels = img.getRGB(0, 0, width, height, null, 0, width);
    }

    /**
     * Create a StegImage from an image file
     * @param path path to the image
     * @throws IOException if the image cannot be loaded
     */
    public StegImage(String path) throws IOException{
        this.path = path;
        Load(path);
    }

    /**
     * Write the image to disk. The image is written as a png file.
     * @throws IOException if the image cannot be written
     */
    public void Write() throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0,0,width, height, pixels, 0, width);
        // prepend steg_ to the img name and save in png format
        String newImgPath = path.substring(0, path.lastIndexOf(File.separator)+1) +
                "steg_" + path.substring(path.lastIndexOf(File.separator)+1);
        newImgPath = newImgPath.substring(0, newImgPath.lastIndexOf(".")) + ".png";
        File output = new File(newImgPath);
        ImageIO.write(img, "png", output);
    }
}
