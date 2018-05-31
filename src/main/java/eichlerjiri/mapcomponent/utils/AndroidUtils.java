package eichlerjiri.mapcomponent.utils;

import android.content.Context;

public class AndroidUtils {

    public static float spToPix(Context context, float sp) {
        return sp * context.getResources().getDisplayMetrics().scaledDensity;
    }
}
