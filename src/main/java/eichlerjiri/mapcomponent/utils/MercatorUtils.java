package eichlerjiri.mapcomponent.utils;

public class MercatorUtils {

    public static double lonToMercatorX(double lon) {
        return (lon + 180) / 360;
    }

    public static double latToMercatorY(double lat) {
        double y = Math.log(Math.tan((lat + 90) * (Math.PI / 360)));
        return 0.5 - y / (2 * Math.PI);
    }
}
