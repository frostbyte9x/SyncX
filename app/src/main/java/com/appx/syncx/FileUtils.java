package com.appx.syncx;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.Settings;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class FileUtils
{
    private final Context context;
    private static final String APP_TAG = "SYNCX";
    private File musicDir = null;

    public FileUtils(Context context)
    {
        this.context = context;
        musicDir = determineStorageDirectory();
    }

    public boolean hasManageExternalStoragePermission()
    {
        return Environment.isExternalStorageManager();
    }

    public File determineStorageDirectory()
    {
        StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        StorageVolume[] storageVolumes = storageManager.getStorageVolumes().toArray(new StorageVolume[1]);
        for (StorageVolume storageVolume : storageVolumes)
        {
            if (storageVolume.isRemovable())
            {
                String sdCardPath = Objects.requireNonNull(storageVolume.getDirectory()).getPath();
                return new File(sdCardPath);
            }
        }
        return null;
    }

    public List<String> getFiles()
    {

        List<String> mp3Files = new ArrayList<>();

        if (musicDir.exists())
        {
            Log.d(APP_TAG, "Directory OK.");

            Path musicDirPath = Paths.get(musicDir.getAbsolutePath());
            try
            {
                Files.walk(musicDirPath)
                        .filter(Files::isRegularFile)
                        .map(path -> musicDirPath.relativize(path).toString())
                        .forEach(mp3Files :: add);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            Log.d(APP_TAG, "Cannot find directory.");
        }

        Collections.sort(mp3Files);
        return mp3Files;
    }

    public boolean createDirectories(String filepath)
    {
        File localFile = new File(filepath);
        File parentDir = localFile.getParentFile();
        if (parentDir != null && !parentDir.exists())
            return parentDir.mkdirs();
        else
            return false;
    }

    public boolean delete(String localFilePath)
    {
        File deletFile = new File(localFilePath);

        if (deletFile.exists())
            return deletFile.delete();
        else
        {
            Log.d(APP_TAG, "File does not exists.");
            return false;
        }
    }

    public void requestPermission()
    {
        Uri uri = Uri.parse("package:" + context.getPackageName());
        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri);
        context.startActivity(intent);
    }

    public String getSHA256(String localFilePath) throws IOException, NoSuchAlgorithmException
    {
        File file = new File(localFilePath);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (FileInputStream fis = new FileInputStream(file))
        {
            byte[] byteArray = new byte[1024];
            int bytesRead;

            while ((bytesRead = fis.read(byteArray)) != -1)
            {
                digest.update(byteArray, 0, bytesRead);
            }
        }

        byte[] hashBytes = digest.digest();

        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes)
        {
            hexString.append(String.format("%02x", b));
        }

        return hexString.toString();
    }

    public boolean isExisting(String localFilePath)
    {
        File file = new File(localFilePath);
        return file.isFile() && file.exists();
    }

    public void deleteEmptyDirectories(File folder)
    {
        if (folder.isDirectory())
        {
            File[] files = folder.listFiles();
            if (files == null)
                return;

            for (File file : files)
                deleteEmptyDirectories(file);

            if (folder.listFiles().length == 0) {
                if (!folder.delete())
                    Log.d("SYNCX", "Failed to delete directory: " + folder.getAbsolutePath());
                else
                    Log.d("SYNCX", "Deleted empty directory: " + folder.getAbsolutePath());

            }
        }
    }
}