package cz.cuni.mff.hrbanh.steganography;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for preprocessing a file to be hidden in an image. Content of file is read as an array of bytes and prepended with
 * specific metadata to store the file name, encoding parameters, etc.
 * HiddenFile data structure:
 *      bytes 0-12 - magic number
 *      byte 13 - bitsPerByte - number of least-significant bits changed (1-8)
 *      byte 14 - length of filename in bytes (max 255 ASCII characters)
 *      bytes 15-x - filename
 *      bytes (x+1)-(x+5) - length of file data in bytes (files up to 2^32 B = 2^22 kB = 2^12 MB = 4 GB)
 *      bytes (x+6)-... - file data
 * Bytes 0-13 are be encoded using one bit per byte encoding (only change one least significant bit of a byte),
 * the rest will be hidden using bitsPerByte encoding, as specified in the metadata, so that we can decode it after
 * finding out bitsPerByte. bitsPerByte parameter can be specified by the user.
 */
public final class HiddenFile {
    private final byte[] data;
    private final int bitsPerByte;

    /**
     * Magic number to determine whether an image contains a hidden file.
     * Spells out "steganography" in hexadecimal.
     */
    public static final byte[] magicNumber = new byte[] {0x73, 0x74, 0x65, 0x67, 0x61,
            0x6e, 0x6f, 0x67, 0x72, 0x61, 0x70, 0x68, 0x79};

    /**
     * Takes as argument a path to and image and hides the HiddenImage in the created instance of StegImage.
     * The magic number and the bitsPerByte of the hidden image are encoded using 1 bit per byte,
     * and the rest of `data` is encoded using `bitsPerByte`. This is useful when decoding an image to easily
     * determine whether it contains a hidden file and get its `bitsPerByte` parameter.
     * The data is encoded as masking a certain number of least-significant bits of each byte of a pixel.
     * The pixels are in ARGB color space, therefore each pixel contains 4 bytes (A,R,G,B).
     * Returns an instance of `StegImage` with an encoded hidden file.
     * @param imgPath path to the image
     * @return StegImage
     * @throws IOException if the `imgPath` is unreadable.
     */
    public StegImage HideInImage(String imgPath) throws IOException{
        StegImage img = new StegImage(imgPath);
        int imgCapacity = img.GetCapacity()[bitsPerByte - 1];
        if (imgCapacity < data.length - 259){
            System.out.println("Error: Image capacity is smaller than the hidden file size. " +
                    "Use bigger image or smaller file.");
            throw new IOException("Image capacity is smaller than the hidden file size.");
        }
        int[] pixels = img.pixels;
        int data_ix = 0;
        byte buffer = data[data_ix];
        int bufferMask = 0b1;
        boolean bit;
        int bitMask;

        for (int i = 0; i < pixels.length; i++) {               // for each pixel
            for (int j = 0; j < 4; j++){                        // for each byte in pixel - B, G, R, A
                for (int k = 0; k < bitsPerByte; k++){          // for `bitsPerByte` least significant bits in byte
                    bit = (buffer & bufferMask) != 0;           // get current bit from buffer
                    bitMask = 1 << (k + 8*j);                   // shift bitMask to correct position
                    if (bit){
                        pixels[i] |= bitMask;
                    } else {
                        pixels[i] &= ~bitMask;
                    }
                    bufferMask <<= 1;
                    if (bufferMask >= 0x100){
                        bufferMask = 0b1;
                        data_ix++;
                        if (data_ix >= data.length){
                            return img;
                        }
                        buffer = data[data_ix];
                    }
                    if (data_ix <= 13 || (data_ix == 14 && bufferMask == 1) ){      // if we are encoding magic number and bitsPerByte
                        break;
                    }
                }
            }
        }
        if (data_ix < data.length-1){
            throw new IOException("Image capacity is smaller than the hidden file size.");
        }
        else {
            return img;
        }
    }

    /**
     * Write an array of bytes to file with name `path`.
     * Used when decoding an image.
     * @param path name of the file to be written.
     * @param data
     * @throws IOException
     */
    private static void WriteToFile(String path, byte[] data) throws IOException{
        File file = new File(path);
        file.createNewFile();
        java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
        fos.write(data);
        fos.close();
    }

    /**
     * Try to recover a hidden file from an image.
     * If the image contained a hidden file, it is processed and saved as "extracted_" + original file name.
     * This is the inverse operation to HideInImage.
     * We process the first several pixels that are encoded using 1 bpB and verify magic number and get bit per byte
     * parameter of the hidden file. Then we process the rest of the image using that parameter and store the recovered
     * data in a list. After processing all metadata and reading all data we call WriteToFile to save the hidden file.
     * @param imgPath path to image we try to recover hidden information from.
     * @throws IOException if the image is unable to read.
     * @throws IllegalArgumentException if the file is corrupted or the image does not contain a hidden file.
     */
    public static void ExtractFromImage(String imgPath) throws IOException, IllegalArgumentException{
        StegImage img = new StegImage(imgPath);
        int[] pixels = img.pixels;
        List<Byte> dataArr = new ArrayList<>();
        byte buffer = 0;
        int bufferMask = 1;
        boolean bit;

        int filenameLength = 0;     // in bytes
        int dataLength = 0;         // in bytes
        int bitsPerByte = 1;        // start with value set to 1, update it after reading the metadata
        String fileName = "";

        for (int i = 0; i < pixels.length; i++){                // cycle through pixels of image
            for (int j = 0; j < 4; j++){                        // bytes in a pixel (A, R, G, B)
                for (int k = 0; k < bitsPerByte; k++){          // least-significant bits of a given byte
                    bit = (pixels[i] & (1 << (j*8 + k)) )!= 0;
                    if (bit){
                        buffer |= bufferMask;
                    }
                    bufferMask <<= 1;
                    if (bufferMask >= 0x100){
                        bufferMask = 1;
                        dataArr.add(buffer);
                        buffer = 0;
                    }
                    if (dataArr.size() == 13 && bufferMask == 1){     // after reading first 13 bytes, verify magic number
                        for (int l = 0; l < magicNumber.length; l++){
                            if (dataArr.get(l) != magicNumber[l]){
                                throw new IllegalArgumentException("Magic number does not match");
                            }
                        }
                    }
                    if (dataArr.size() == 14 && bufferMask == 1){
                        bitsPerByte = dataArr.get(13);              // now we can process pixels using `bitsPerByte`
                        if (bitsPerByte < 1 || bitsPerByte > 8){
                            throw new IllegalArgumentException("bitsPerByte parameter must be between 1 and 8");
                        }
                        break;
                    }
                    if (dataArr.size() == 15 && bufferMask == 1){
                        filenameLength = dataArr.get(14);
                    }
                    if (dataArr.size() == 15 + filenameLength && bufferMask == 1){
                        byte[] fileNameBytes = new byte[filenameLength];
                        for (int l = 0; l < filenameLength; l++){
                            fileNameBytes[l] = dataArr.get(15 + l);
                        }
                        fileName = "extracted_" + new String(fileNameBytes);
                    }
                    if (dataArr.size() == 15 + filenameLength + 4 && bufferMask == 1){
                        dataLength = (int)(dataArr.get(15 + filenameLength) & 0xFF) |
                                ((int)(dataArr.get(15 + filenameLength + 1) & 0xFF) << 8) |
                                ((int)(dataArr.get(15 + filenameLength + 2) & 0xFF) << 16) |
                                ((int)(dataArr.get(15 + filenameLength + 3) & 0xFF) << 24);
                    }
                    if (dataArr.size() == 15 + filenameLength + 4 + dataLength && bufferMask == 1){
                        byte[] dataBytes = new byte[dataLength];
                        for (int l = 0; l < dataLength; l++){
                            dataBytes[l] = dataArr.get(15 + filenameLength + 4 + l);
                        }
                        WriteToFile(fileName, dataBytes);
                        return;
                    }
                }
            }
        }
    }

    /**
     * Get contents of a file as a byte array.
     * @param filePath
     * @return byte[] data
     * @throws IOException
     */
    private byte[] ReadFileBin(String filePath) throws IOException {
        File file = new File(filePath);
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();
        return data;
    }

    /**
     * Create instance of a HiddenFile. The structure is defined in the HiddenFile class docstring.
     * The data is saved as a byte[] data including all metadata and specifies what will be encoded to an image later on.
     * The class also stores its bitsPerByte parameter as an integer, which is also specified in metadata.
     * @param filePath
     * @param bitsPerByte
     * @throws IOException
     */
    public HiddenFile(String filePath, int bitsPerByte) throws IOException{
        String filename = filePath.substring(filePath.lastIndexOf('/')+1);
        this.bitsPerByte = bitsPerByte;
        byte[] fileNameBytes = filename.getBytes();
        int filenameLength = fileNameBytes.length;
        byte[] data = ReadFileBin(filePath);
        this.data = new byte[data.length + 19 + filenameLength];

        //writing magic number - bytes 0, .. , 12
        System.arraycopy(magicNumber, 0, this.data, 0, magicNumber.length);

        // bitsPerByte - byte 13
        if (bitsPerByte < 1 || bitsPerByte > 8){
            throw new IllegalArgumentException("bitsPerByte must be between 1 and 8");
        }
        this.data[13] = (byte)bitsPerByte;

        //length of filename - byte 14

        if (filenameLength > 255){
            throw new IllegalArgumentException("Filename too long. Must be less than 255 characters.");
        }
        this.data[14] = (byte)filenameLength;

        //filename - byte 15, .. , 16 + filename.length - one byte per character in filename
        System.arraycopy(fileNameBytes, 0, this.data, 15, filenameLength);

        // num_bytes_in_file - 4 bytes
        int fileSize = data.length;         // in bytes
        for (int i = 0; i < 4; i++){
            byte part = (byte)(fileSize >> 8*i);
            this.data[15+filenameLength+i] = part;
        }

        // file data
        System.arraycopy(data, 0, this.data, 19+filenameLength, data.length);
    }
}
