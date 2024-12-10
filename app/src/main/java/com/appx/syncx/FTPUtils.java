package com.appx.syncx;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import android.content.Context;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.ArrayList;

public class FTPUtils
{
    private final FTPClient ftpClient;
    private final FileUtils fileUtils;
    private final String HOST = "42.232.42.756";
    private final int PORT = 21;
    private final String[] creds = new String[] {"anonymous", "anonymous@domain.com"};

    public FTPUtils(Context context)
    {
        ftpClient = new FTPClient();
        fileUtils = new FileUtils(context);
        ftpClient.setDefaultTimeout(30000);
    }

    public void connect() throws Exception
    {
        ftpClient.connect(HOST, PORT);
        ftpClient.login(creds[0], creds[1]);
    }

    public boolean checkConnection()
    {
        return ftpClient != null && ftpClient.isConnected();
    }

    public void disconnect() throws Exception
    {
        if (checkConnection())
        {
            ftpClient.logout();
            ftpClient.disconnect();
        }
    }

    public List<String> readIndexFile() throws Exception
    {
        List<String> files = new ArrayList<>();
        String remotePath = "index.txt";
        try
        {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(ftpClient.retrieveFileStream(remotePath), StandardCharsets.UTF_8)
            );
            String line;
            while ((line = reader.readLine()) != null)
                files.add(line.strip());
            reader.close();

            if (!ftpClient.completePendingCommand())
                throw new Exception("Failed to complete FTP transaction.");
        }
        catch (Exception e)
        {
            throw new Exception("Error reading remote file: " + e.getMessage(), e);
        }
        return files;
    }


    public boolean isExisting(String remoteFilePath)
    {
        try
        {
            FTPFile[] files = ftpClient.listFiles(remoteFilePath);
            return files.length == 1 && files[0].isFile();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }
    }

    public boolean download(String remoteFilePath, String localFilePath) throws IOException
    {
        fileUtils.createDirectories(localFilePath);
        FileOutputStream fos = new FileOutputStream(localFilePath);
        return ftpClient.retrieveFile(remoteFilePath, fos);
    }

    public String getSHA256OverFTP(String remoteFilePath) throws IOException, NoSuchAlgorithmException
    {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        InputStream inputStream = ftpClient.retrieveFileStream(remoteFilePath);
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream())
        {
            byte[] byteArray = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(byteArray)) != -1)
            {
                digest.update(byteArray, 0, bytesRead);
            }
        }
        finally
        {
            ftpClient.completePendingCommand();
        }

        byte[] hashBytes = digest.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes)
        {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }
}
