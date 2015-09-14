package com.doubleencore.bugreport.internal;

import android.support.annotation.WorkerThread;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created on 3/24/14.
 */
public class ZipUtils {

    /**
     * List of all files in a given directory
     * @param baseDirectory directory to look for files in
     * @param recursiveSearch true to recursively look through sub directories of baseDirectory and add them
     * @return list of files
     */
    public static List<File> getFiles(File baseDirectory, boolean recursiveSearch) {
        ArrayList<File> returnFiles = new ArrayList<>();
        File[] files = baseDirectory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.canRead()) {
                    if (recursiveSearch && file.isDirectory()) {
                        returnFiles.addAll(getFiles(file, true));
                    } else {
                        returnFiles.add(file);
                    }
                }
            }
        }
        return returnFiles;
    }

    /**
     * Generate a zip file in location provided with the files given
     * @param path Directory to place the file
     * @param zipName Name to give to the zip file
     * @param files Files to compress into the zip file
     * @return File to the zip file generate
     * @throws java.io.IOException For any read/write/etc issues
     */
    @WorkerThread
    public static File generateZip(File path, String zipName, List<File> files) throws IOException {
        File file = new File(path, zipName);
        FileOutputStream destination = new FileOutputStream(file);
        ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(destination));
        for (File toZip : files) {
            writeToZip(toZip, zos);
        }
        zos.close();
        destination.close();

        return file;
    }

    private static void writeToZip(File file, ZipOutputStream zos) throws IOException {
        byte[] buffer = new byte[1024];
        FileInputStream fis = new FileInputStream(file);
        zos.putNextEntry(new ZipEntry(file.getAbsolutePath()));
        int length;
        while ((length = fis.read(buffer)) > 0) {
            zos.write(buffer, 0, length);
        }
        fis.close();
        zos.closeEntry();
    }
}
