package eichlerjiri.mapcomponent.utils;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class IOUtils {

    public static byte[] download(String url) {
        URLConnection conn = null;
        try {
            conn = new URL(url).openConnection();
            InputStream is = conn.getInputStream();
            try {
                return readAll(is);
            } finally {
                closeStream(is);
            }
        } catch (IOException e) {
            Log.e("IOUtils", "Cannot download file: " + url, e);
            if (conn instanceof HttpURLConnection) {
                InputStream es = ((HttpURLConnection) conn).getErrorStream();
                if (es != null) {
                    readAll(es);
                    closeStream(es);
                }
            }
            return null;
        }
    }

    public static byte[] readFile(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            try {
                return readAll(fis);
            } finally {
                closeStream(fis);
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
                closeStream(fos);
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

    private static void closeStream(InputStream is) {
        try {
            is.close();
        } catch (IOException e) {
            Log.e("IOUtils", "Cannot close stream", e);
        }
    }

    private static void closeStream(OutputStream os) {
        try {
            os.close();
        } catch (IOException e) {
            Log.e("IOUtils", "Cannot close stream", e);
        }
    }
}
