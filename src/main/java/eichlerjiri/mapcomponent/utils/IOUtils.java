package eichlerjiri.mapcomponent.utils;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class IOUtils {

    public static byte[] download(String url) {
        try {
            InputStream is = new URL(url).openStream();
            try {
                return readAll(is);
            } finally {
                is.close();
            }
        } catch (IOException e) {
            Log.e("IOUtils", "Cannot download file: " + url, e);
            return null;
        }
    }

    public static byte[] readFile(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            try {
                return readAll(fis);
            } finally {
                fis.close();
            }
        } catch (IOException e) {
            Log.e("IOUtils", "Cannot read file: " + file, e);
            return null;
        }
    }

    public static boolean writeFile(File file, byte[] data) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            try {
                fos.write(data);
                return true;
            } finally {
                fos.close();
            }
        } catch (IOException e) {
            Log.e("IOUtils", "Cannot write file: " + file, e);
            return false;
        }
    }

    public static byte[] readAll(InputStream is) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            while (true) {
                int num = is.read(buffer);
                if (num == -1) {
                    break;
                }
                baos.write(buffer, 0, num);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            Log.e("IOUtils", "Cannot read stream", e);
            return null;
        }
    }
}
