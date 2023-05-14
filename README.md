# Steganography

## Description
This is a simple steganography tool that can be used to hide files in images 
using least-significant bit encoding. 
It can also be used to extract files from images that have files hidden in them.

## Usage
First to compile the program, change to the target directory which contains the 
compiled classes  and finally run the program in interactive mode, 
run the following commands:
```bash
mvn clean install
cd target/classes
java cz.cuni.mff.hrbanh.steganography.Main
```

Running the command with no arguments will execute the program in interactive mode.
 ## Commands
    -h, --hide PATH_TO_IMAGE PATH_TO_FILE BITS_PER_BYTE
        
Hides the content of the the file into the image. The image is loaded 
as an array of pixels in ARGB format. The content of the file is then
stored in the least significant bits of the pixels. The number of bits
used per byte is specified by the `BITS_PER_BYTE` argument (must be between
`1` and `8`). The resulting image is saved with the same name, but prepended 
with `steg_` in the same directory as the original image.
    
    -e, --extract PATH_TO_IMAGE

Extracts the content of the image. The extracted content is saved in the same directory as the
original image with the same name, but prepended with `extracted_`. (Notice, that the `BITS_PER_BYTE` argument is not needed, because it is
inferred from the image itself.)

    -c, --capacities PATH_TO_IMAGE

Displays the capacities (maximum size of a file that can be stored in that image) of the 
image. The capacities are displayed in bits per byte for all possible values of bits per byte.
    
    -i, --interactive

Starts the program in interactive mode. The user can enter as many commands as they want.
The interactive mode can also be started by providing no arguments to the program.
The command syntax is the same as above. Use the `exit` command to exit the program.
    
    --help

Displays the help message.

## Examples
The `src/java/resources` directory contains a few sample images and files that can be used
to test the program. Here are a few examples of how to use the program (first compile the
program and change directory as described above):

1. Interactive mode

To execute the program in interactive mode, run the following command:
```bash
java cz.cuni.mff.hrbanh.steganography.Main
```
After the program starts, you can enter commands using the same syntax as below.
It is possible to enter as many commands as you want. To exit the program, enter `exit`.

2. Hiding a text file in an image
```bash 
java cz.cuni.mff.hrbanh.steganography.Main -h stromovka.jpeg shakespeare.txt 1
```
This stores the content of the `shakespeare.txt` file, which contains 40000 lines
of Shakespeare's plays into the `stromovka.jpeg` image using `1` bit per byte encoding.
The resulting image is saved as `steg_stromovka.jpeg`.

3. Hiding a python script in an image
```bash
java cz.cuni.mff.hrbanh.steganography.Main -h red.png minesweeper.py 7
```
This stores a sample python script into an image that contains only red pixels. 
We use this image for illustration purposes. The number of bits per byte is set to 7, 
which is high, so the resulting image steg_red.png contains artifacts.

4. Extracting a file from an image
```bash
java cz.cuni.mff.hrbanh.steganography.Main -e lake.png
```
The image `lake.png` contains a hidden message. This command extracts the message and 
saves it as a new file. The file will be called `extracted_snow.jpeg` and is an image
of some snowy trees. If the file does not contain a valid message, the program will
throw an exception.

5. Displaying the capacities of an image
```bash
java cz.cuni.mff.hrbanh.steganography.Main -c stromovka.jpeg
```
Prints the capacities of the image `stromovka.jpeg` for all possible values of 
bits per byte parameters.

6. Getting help
```bash
java cz.cuni.mff.hrbanh.steganography.Main --help
```
Prints the help message.
