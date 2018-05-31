package eichlerjiri.mapcomponent.utils;

public class GeoBoundary {

    public double minLat = Double.MAX_VALUE;
    public double minLon = Double.MAX_VALUE;
    public double maxLat = Double.MIN_VALUE;
    public double maxLon = Double.MIN_VALUE;

    public GeoBoundary() {
    }

    public GeoBoundary(GeoBoundary geoBoundary) {
        minLat = geoBoundary.minLat;
        minLon = geoBoundary.minLon;
        maxLat = geoBoundary.maxLat;
        maxLon = geoBoundary.maxLon;
    }

    public void addPoint(double lat, double lon) {
        minLat = Math.min(minLat, lat);
        minLon = Math.min(minLon, lon);
        maxLat = Math.max(maxLat, lat);
        maxLon = Math.max(maxLon, lon);
    }
}
