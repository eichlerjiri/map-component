package eichlerjiri.mapcomponent.tiles;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import eichlerjiri.mapcomponent.MapComponent;

public class TileDownloadPool extends ThreadPoolExecutor {

    public final MapComponent mc;
    private final ArrayList<String> mapUrls;
    private final ThreadLocal<String> serverUrl = new ThreadLocal<>();

    public TileDownloadPool(MapComponent mc, ArrayList<String> mapUrls) {
        super(10, 10, 10L, TimeUnit.SECONDS, new PriorityBlockingQueue<Runnable>(),
                new ThreadPoolExecutor.DiscardPolicy());
        allowCoreThreadTimeOut(true);

        this.mc = mc;
        this.mapUrls = mapUrls;
    }

    public String getServerUrl() {
        String serverUrlStr = serverUrl.get();
        if (serverUrlStr == null) {
            serverUrlStr = mapUrls.get(new Random().nextInt(mapUrls.size()));
            serverUrl.set(serverUrlStr);
        }
        return serverUrlStr;
    }
}
