package eichlerjiri.mapcomponent.utils;

public class DoubleArrayList {

    public double[] data = new double[8];
    public int size;

    public void clear() {
        size = 0;
    }

    private void ensureCapacity(int extra) {
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

        data[size] = v0;
        data[size + 1] = v1;
        size += 2;
    }

    public void add(double v0, double v1, double v2, double v3, double v4, double v5,
                    double v6, double v7, double v8, double v9, double v10, double v11) {
        if (data.length < size + 12) {
            ensureCapacity(12);
        }

        data[size] = v0;
        data[size + 1] = v1;
        data[size + 2] = v2;
        data[size + 3] = v3;
        data[size + 4] = v4;
        data[size + 5] = v5;
        data[size + 6] = v6;
        data[size + 7] = v7;
        data[size + 8] = v8;
        data[size + 9] = v9;
        data[size + 10] = v10;
        data[size + 11] = v11;
        size += 12;
    }
}
