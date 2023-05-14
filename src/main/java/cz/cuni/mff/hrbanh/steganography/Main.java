package cz.cuni.mff.hrbanh.steganography;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Scanner;

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
        System.out.println("++++ Capacities for image " + imgPath + ":");
        for (int i = 0; i < 8; i++){
            int cap = capacitiesPerbPB[i];
            String capacity = cap + " B";
            if (cap > 1024*10){
                cap /= 1024;
                capacity = cap+" kB";
                if (cap >= 1024*10){
                    cap /= 1024;
                    capacity = cap+" MB";
                }
            }
            System.out.println("    - Capacity for " + (i+1) + " bits per byte: " + capacity);
        }
    }

    /**
     * Function to process command line arguments
     * @param args command line arguments
     */
    private static void ProcessArgs(String[] args) throws IOException{
        String helpMessage = "Usage: java cz.cuni.mff.hrbanh.steganography.Main [OPTION]... [FILE]...\n" +
                "Hide a file in an image or extract a file from an image.\n" +
                "Options:\n" +
                "  -h, --hide PATH_TO_IMAGE PATH_TO_FILE BITS_PER_BYTE\n" +
                "                        Hide a file in an image.\n" +
                "  -e, --extract PATH_TO_IMAGE\n" +
                "                        Extract a file from an image.\n" +
                "  -c, --capacities PATH_TO_IMAGE\n" +
                "                        Print the maximum size of files which can be hidden in a given image for every\n" +
                "                        possible bitsPerByte encoding (1-8).\n" +
                "  -i, --interactive     Run in interactive mode.\n" +
                "  --help                Print this message and exit.\n" +
                "\n" +
                "Examples:\n" +
                "  java cz.cuni.mff.hrbanh.steganography.Main -h image.png file.txt 1\n" +
                "  java cz.cuni.mff.hrbanh.steganography.Main -e image.png\n" +
                "  java cz.cuni.mff.hrbanh.steganography.Main -c image.png\n" +
                "  java cz.cuni.mff.hrbanh.steganography.Main -i\n" +
                "  java cz.cuni.mff.hrbanh.steganography.Main --help\n";
        switch (args[0]){
            case "--hide":
            case "-h":
            case "hide":
            case "h":
                System.out.println("++++ Hiding file " + args[2] + " in image " +
                        args[1] + " with " + args[3] + " bits per byte...");
                Hide(args[1], args[2], Integer.parseInt(args[3]));
                String newImgPath = args[1].substring(0, args[1].lastIndexOf(File.separator)+1) +
                        "steg_" + args[1].substring(args[1].lastIndexOf(File.separator)+1);
                newImgPath = newImgPath.substring(0, newImgPath.lastIndexOf(".")) + ".png";
                System.out.println("++++ File hidden. Image saved as " + newImgPath);
                break;
            case "--extract":
            case "-e":
            case "extract":
            case "e":
                System.out.println("++++ Extracting file from image " + args[1]);
                ExtractFile(args[1]);
                System.out.println("++++ File extracted to current directory.");
                break;
            case "--capacities":
            case "-c":
            case "capacities":
            case "c":
                PrintCapacities(args[1]);
                break;
            case "--help":
            case "help":
                System.out.println(helpMessage);
                break;
            default:
                System.out.println("Unknown command. Use --help for help.");
        }
    }
    public static void main(String[] args){
        try {
            if (args.length == 0 || args[0].equals("--interactive") || args[0].equals("-i") ||
                    args[0].equals("interactive") || args[0].equals("i")) {
                Scanner sc = new Scanner(System.in);
                while (true){
                    System.out.println("Enter command:");
                    String[] input = sc.nextLine().split(" ");
                    if (input[0].equals("exit")){
                        break;
                    }
                    ProcessArgs(input);
                }
            } else{
                ProcessArgs(args);
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}