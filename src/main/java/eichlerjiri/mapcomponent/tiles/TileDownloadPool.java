package eichlerjiri.mapcomponent.tiles;

import java.util.Random;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import eichlerjiri.mapcomponent.utils.ObjectList;

public class TileDownloadPool extends ThreadPoolExecutor {

    public final ObjectList<String> mapUrls;
    public final ThreadLocal<String> serverUrl = new ThreadLocal<>();

    public TileDownloadPool(ObjectList<String> mapUrls) {
        super(10, 10, 10L, TimeUnit.SECONDS, new PriorityBlockingQueue<Runnable>(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread t = new Thread(runnable);
                t.setPriority(4);
                return t;
            }
        }, new ThreadPoolExecutor.DiscardPolicy());
        allowCoreThreadTimeOut(true);

        this.mapUrls = mapUrls;
    }

    public String getServerUrl() {
        String serverUrlStr = serverUrl.get();
        if (serverUrlStr == null) {
            serverUrlStr = mapUrls.data[new Random().nextInt(mapUrls.size)];
            serverUrl.set(serverUrlStr);
        }
        return serverUrlStr;
    }
}
