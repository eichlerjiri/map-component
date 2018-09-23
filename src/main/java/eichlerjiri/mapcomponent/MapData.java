package eichlerjiri.mapcomponent;

public class MapData {

    public double posX = 0.5;
    public double posY = 0.5;
    public float zoom;
    public float azimuth;

    public double currentPositionX = Double.MIN_VALUE;
    public double currentPositionY = Double.MIN_VALUE;
    public float currentPositionAzimuth = Float.MIN_VALUE;
    public double startPositionX = Double.MIN_VALUE;
    public double startPositionY = Double.MIN_VALUE;
    public double endPositionX = Double.MIN_VALUE;
    public double endPositionY = Double.MIN_VALUE;
    public double[] path;
    public int pathOffset;
    public int pathLength;

    public MapData copy() {
        MapData n = new MapData();

        n.posX = posX;
        n.posY = posY;
        n.zoom = zoom;
        n.azimuth = azimuth;

        n.currentPositionX = currentPositionX;
        n.currentPositionY = currentPositionY;
        n.currentPositionAzimuth = currentPositionAzimuth;
        n.startPositionX = startPositionX;
        n.startPositionY = startPositionY;
        n.endPositionX = endPositionX;
        n.endPositionY = endPositionY;
        n.path = path;
        n.pathOffset = pathOffset;
        n.pathLength = pathLength;

        return n;
    }
}
