package com.appx.syncx;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.List;

public class Home extends AppCompatActivity {

    private FileUtils fileUtils;
    private FTPUtils ftpUtils;
    private TextView textView;
    private Button button;
    private String localMediaDirectory;
    private static final String APP_TAG = "SYNCX";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        button = findViewById(R.id.sync_files);
        textView = findViewById(R.id.text_view);

        fileUtils = new FileUtils(this);
        ftpUtils = new FTPUtils(this);
        localMediaDirectory = fileUtils.determineStorageDirectory() + "/";

        Log.d(APP_TAG, String.valueOf(localMediaDirectory));
        textView.setText(null);

        if (!fileUtils.hasManageExternalStoragePermission())
            fileUtils.requestPermission();

        button.setOnClickListener(view -> new Thread(() -> {
            try {
                Log.d(APP_TAG, "Sync Started.");
                printTextView("Sync Started.");
                ftpUtils.connect();
                List<String> remoteStorageList = ftpUtils.readIndexFile();
                List<String> localStorageList = fileUtils.getFiles();
                Log.d(APP_TAG, String.valueOf("File count (Remote)" + remoteStorageList.size()));
                Log.d(APP_TAG, String.valueOf("File count (Local)" + localStorageList.size()));

                for (int i = 0; i < remoteStorageList.size(); i++) {
                    String remoteFilePath = remoteStorageList.get(i);
                    String localFilePath = localMediaDirectory + remoteFilePath;

                    if (fileUtils.isExisting(localFilePath)) {
                        String remoteFileChecksum = ftpUtils.getSHA256OverFTP(remoteFilePath);
                        String localFileChecksum = fileUtils.getSHA256(localFilePath);

                        if (!remoteFileChecksum.equals(localFileChecksum))
                        {
                            Log.d(APP_TAG, "Updated: " + remoteFilePath + ", " + localFilePath);
                            ftpUtils.download(remoteFilePath, localFilePath);
                            printTextView("Updated: "+ localFilePath);
                        }
                        else
                        {
                            Log.d(APP_TAG, "Skipped: " + remoteFilePath + ", " + localFilePath);
                            printTextView("Skipped: " + localFilePath);
                        }
                    }
                    else
                    {
                        Log.d(APP_TAG, "Created: " + remoteFilePath + ", " + localFilePath);
                        ftpUtils.download(remoteFilePath, localFilePath);
                        printTextView("Created: " + localFilePath);
                    }

                    localStorageList.remove(remoteFilePath);
                }

                for (int i = 0; i < localStorageList.size(); i++) {
                    String remoteFilePath = localStorageList.get(i);
                    String localFilePath = localMediaDirectory + remoteFilePath;

                    if (ftpUtils.isExisting(remoteFilePath)) {
                        Log.d(APP_TAG, "Deleted: " + localFilePath + ", " + remoteFilePath);
                        fileUtils.delete(localFilePath);
                        printTextView("Deleted: " + localFilePath);
                    }
                }

                fileUtils.deleteEmptyDirectories(new File(localMediaDirectory));

                ftpUtils.disconnect();
                Log.d(APP_TAG, "Sync Completed.");
                printTextView("Sync Completed.");
            } catch (InterruptedException e) {
                Log.d(APP_TAG, "Thread interrupted, but continuing execution.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start());
    }

    private void printTextView(String text) {
        runOnUiThread(() -> {
            textView.append(text + "\n");
        });
    }
}