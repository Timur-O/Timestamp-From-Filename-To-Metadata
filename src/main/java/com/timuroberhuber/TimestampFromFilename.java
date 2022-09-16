package com.timuroberhuber;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TimestampFromFilename {

    public static void main(String[] args) throws IOException, ImageReadException, ImageWriteException {
        String srcDir = "SRC DIR HERE";
        String outDir = "OUt DIR HERE";
        File srcDirFile = new File(srcDir);

        List<File> listOfFiles = getImages(srcDirFile);

        for (File currFile : listOfFiles) {
            LocalDateTime newDate = extractDate(currFile);
            String outputName = outDir + "\\" + currFile.getName();
            changeTimestamp(currFile, newDate, outputName);
        }
    }

    public static List<File> getImages(File srcDir) {
        List<File> listOfFiles = List.of(Objects.requireNonNull(srcDir.listFiles()));
        listOfFiles = listOfFiles
                        .stream()
                        .filter(File::isFile)
                        .filter(x -> x.getName().contains("jpg"))
                        .collect(Collectors.toList());
        return listOfFiles;
    }

    public static LocalDateTime extractDate(File file) {
        String filename = file.getName();
        String newDate = filename.substring(4, 13);
        newDate = newDate.substring(0,4) + "-" + newDate.substring(4,6) + "-" + newDate.substring(6,8);
        newDate += "T00:00:00";
        return LocalDateTime.parse(newDate);
    }

    public static void changeTimestamp(File file, LocalDateTime newDate, String outputFileName) throws IOException, ImageReadException, ImageWriteException {
            // Source: https://stackoverflow.com/questions/36868013/editing-jpeg-exif-data-with-java
            OutputStream os = null;

            try {
                TiffOutputSet outputSet = null; // The data we're going to write

                final ImageMetadata metadata = Imaging.getMetadata(file); // Could be null
                final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;

                if (null != jpegMetadata) {
                    final TiffImageMetadata exif = jpegMetadata.getExif(); // Could be null, immutable

                    if (null != exif) {
                        // Copy existing data to output data, since existing is immutable
                        outputSet = exif.getOutputSet();
                    }
                }

                // If not data, create empty set of data
                if (null == outputSet) {
                    outputSet = new TiffOutputSet();
                }

                final TiffOutputDirectory exifDirectory = outputSet
                        .getOrCreateExifDirectory();

                // Remove previous value, in case present
                exifDirectory.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);

                // Add new value
                exifDirectory.add(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL,
                                  newDate.format(DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")));

                os = new FileOutputStream(outputFileName);
                os = new BufferedOutputStream(os);

                new ExifRewriter().updateExifMetadataLossless(file, os, outputSet);
            } finally {
                if (os != null) {
                    os.close();
                }
            }
    }
}
