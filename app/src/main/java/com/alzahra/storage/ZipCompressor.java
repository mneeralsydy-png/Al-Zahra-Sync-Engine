package com.alzahra.storage;

import android.util.Log;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;

import java.io.File;

public class ZipCompressor {
    
    private static final String TAG = "ZipCompressor";

    public static void compressData(File sourceDir) {
        if (sourceDir == null || !sourceDir.exists()) {
            Log.w(TAG, "Source directory does not exist");
            return;
        }
        
        File[] files = sourceDir.listFiles();
        if (files == null || files.length == 0) {
            Log.d(TAG, "No files to compress");
            return;
        }
        
        try {
            File zipFile = new File(sourceDir.getParent(), "data_backup.zip");
            
            if (zipFile.exists()) {
                zipFile.delete();
            }
            
            ZipFile zip = new ZipFile(zipFile);
            ZipParameters params = new ZipParameters();
            params.setCompressionMethod(CompressionMethod.DEFLATE);
            params.setCompressionLevel(CompressionLevel.MAXIMUM);
            
            for (File file : files) {
                if (file.getName().equals("data_backup.zip")) continue;
                
                if (file.isDirectory()) {
                    zip.addFolder(file, params);
                } else {
                    zip.addFile(file, params);
                }
            }
            
            Log.d(TAG, "Compressed: " + zipFile.getAbsolutePath() + 
                  " (" + zipFile.length() + " bytes)");
            
        } catch (Exception e) {
            Log.e(TAG, "Compression failed", e);
        }
    }
}
