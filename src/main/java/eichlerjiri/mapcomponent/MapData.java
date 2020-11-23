package eichlerjiri.mapcomponent;

public class MapData {

    public double posX = 0.5;
    public double posY = 0.5;
    public float zoom;
    public float azimuth;

    public boolean centered = true;

    public double currentPositionX = Double.NEGATIVE_INFINITY;
    public double currentPositionY = Double.NEGATIVE_INFINITY;
    public float currentPositionAzimuth = Float.NEGATIVE_INFINITY;
    public double startPositionX = Double.NEGATIVE_INFINITY;
    public double startPositionY = Double.NEGATIVE_INFINITY;
    public double endPositionX = Double.NEGATIVE_INFINITY;
    public double endPositionY = Double.NEGATIVE_INFINITY;
    public double[] path;
    public int pathOffset;
    public int pathLength;

    public MapData copy() {
        MapData n = new MapData();

        n.posX = posX;
        n.posY = posY;
        n.zoom = zoom;
        n.azimuth = azimuth;

        n.centered = centered;

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
