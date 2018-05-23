package eichlerjiri.mapcomponent.utils;

public class MapTileKey {

    public int zoom;
    public int x;
    public int y;

    public MapTileKey() {
    }

    public MapTileKey(MapTileKey other) {
        zoom = other.zoom;
        x = other.x;
        y = other.y;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MapTileKey)) {
            return false;
        }

        MapTileKey o = (MapTileKey) obj;
        return zoom == o.zoom && x == o.x && y == o.y;
    }

    @Override
    public int hashCode() {
        int result = zoom;
        result = 31 * result + x;
        result = 31 * result + y;
        return result;
    }
}
