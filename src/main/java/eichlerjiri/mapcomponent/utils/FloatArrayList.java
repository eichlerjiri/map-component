package eichlerjiri.mapcomponent.utils;

public class FloatArrayList {

    public float[] data = new float[8];
    public int size;

    public void clear() {
        size = 0;
    }

    public void ensureCapacity(int extra) {
        int newCapacity = data.length * 2;
        while (newCapacity < size + extra) {
            newCapacity *= 2;
        }

        float[] dataNew = new float[newCapacity];
        System.arraycopy(data, 0, dataNew, 0, size);
        data = dataNew;
    }

    public void add(float v0, float v1, float v2, float v3, float v4, float v5,
                    float v6, float v7, float v8, float v9, float v10, float v11) {
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
