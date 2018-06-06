package eichlerjiri.mapcomponent.utils;

import android.content.Context;

public class AndroidUtils {

    public static float spSize(Context context) {
        return context.getResources().getDisplayMetrics().scaledDensity;
    }
}
