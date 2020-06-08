package eichlerjiri.mapcomponent.utils;

public class FloatList {

    public float[] data = new float[8];
    public int size;

    public void ensureCapacity(int extra) {
        int newCapacity = data.length * 2;
        while (newCapacity < size + extra) {
            newCapacity *= 2;
        }

        float[] dataNew = new float[newCapacity];
        System.arraycopy(data, 0, dataNew, 0, size);
        data = dataNew;
    }

    public void add(float v0, float v1, float v2, float v3, float v4, float v5, float v6, float v7, float v8, float v9, float v10, float v11) {
        if (data.length < size + 12) {
            ensureCapacity(12);
        }

        data[size++] = v0;
        data[size++] = v1;
        data[size++] = v2;
        data[size++] = v3;
        data[size++] = v4;
        data[size++] = v5;
        data[size++] = v6;
        data[size++] = v7;
        data[size++] = v8;
        data[size++] = v9;
        data[size++] = v10;
        data[size++] = v11;
    }
}
