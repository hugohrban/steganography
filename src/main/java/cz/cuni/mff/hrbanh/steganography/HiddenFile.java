package main.java.cz.cuni.mff.hrbanh.steganography;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class HiddenFile {
    private final byte[] data;
    private final int bitsPerByte;
    public static final byte[] magicNumber = new byte[] {0x73, 0x74, 0x65, 0x67, 0x61,
            0x6e, 0x6f, 0x67, 0x72, 0x61, 0x70, 0x68, 0x79};
    public MyImage HideInImage(String imgPath) throws IOException{
        MyImage img = new MyImage(imgPath);
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
            throw new IOException("File is too large to be hidden in image.");
        }
        else {
            return img;
        }
    }
    private static void WriteToFile(String path, byte[] data) throws IOException{
        File file = new File(path);
        file.createNewFile();
        java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
        fos.write(data);
        fos.close();
    }
    public static void ExtractFromImage(String path) throws IOException{
        MyImage img = new MyImage(path);
        int[] pixels = img.pixels;
        List<Byte> dataArr = new ArrayList<>();
        byte buffer = 0;
        int bufferMask = 1;
        boolean bit;
        int filenameLength = 0;     // in bytes
        int dataLength = 0;         // length of data (in bytes)
        int bitsPerByte = 1;
        String fileName = "";

        for (int i = 0; i < pixels.length; i++){
            for (int j = 0; j < 4; j++){
                for (int k = 0; k < bitsPerByte; k++){
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
                    if (dataArr.size() == 13 && bufferMask == 1){
                        //TODO check if magic number equals, otherwise throw exception
                        for (int l = 0; l < magicNumber.length; l++){
                            if (dataArr.get(l) != magicNumber[l]){
                                throw new IllegalArgumentException("Magic number does not match");
                            }
                        }
                    }
                    if (dataArr.size() == 14 && bufferMask == 1){
                        bitsPerByte = dataArr.get(13);
                        if (bitsPerByte < 1 || bitsPerByte > 8){
                            throw new IllegalArgumentException("bitsPerByte must be between 1 and 8");
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
                        fileName = "extracted_" + new String(fileNameBytes, StandardCharsets.UTF_8);
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
    private byte[] ReadFileBin(String path) throws IOException {
        File file = new File(path);
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();
        return data;
    }
    public HiddenFile(String path, int bitsPerByte) throws IOException{
        String filename = path.substring(path.lastIndexOf('/')+1);
        this.bitsPerByte = bitsPerByte;
        byte[] fileNameBytes = filename.getBytes();
        int filenameLength = fileNameBytes.length;
        byte[] data = ReadFileBin(path);
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
