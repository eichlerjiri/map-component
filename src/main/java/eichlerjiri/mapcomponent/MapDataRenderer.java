package eichlerjiri.mapcomponent;

public class MapDataRenderer {

    public final int width;
    public final int height;

    public final int centerButtonX;
    public final int centerButtonY;
    public final int centerButtonWidth;
    public final int centerButtonHeight;

    public MapDataRenderer(int width, int height, int centerButtonX, int centerButtonY, int centerButtonWidth, int centerButtonHeight) {
        this.width = width;
        this.height = height;
        this.centerButtonX = centerButtonX;
        this.centerButtonY = centerButtonY;
        this.centerButtonWidth = centerButtonWidth;
        this.centerButtonHeight = centerButtonHeight;
    }
}
