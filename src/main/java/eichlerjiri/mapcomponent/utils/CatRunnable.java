package eichlerjiri.mapcomponent.utils;

public abstract class CatRunnable implements Runnable {

    public final int category;

    public CatRunnable(int category) {
        this.category = category;
    }
}
