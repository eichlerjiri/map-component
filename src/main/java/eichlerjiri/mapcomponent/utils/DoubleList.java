package eichlerjiri.mapcomponent.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class DoubleList {

    public double[] data = new double[8];
    public int size;

    public void ensureCapacity(int extra) {
        int newCapacity = data.length * 2;
        while (newCapacity < size + extra) {
            newCapacity *= 2;
        }

        double[] dataNew = new double[newCapacity];
        System.arraycopy(data, 0, dataNew, 0, size);
        data = dataNew;
    }

    public void add(double v0, double v1) {
        if (data.length < size + 2) {
            ensureCapacity(2);
        }

        data[size++] = v0;
        data[size++] = v1;
    }
}
