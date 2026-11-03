package com.bike.trailgo.ui.riding;

import android.content.Context;

/** Local, user-specific ride data that must not survive a completed logout. */
public final class RideLocalData {
    private RideLocalData() {
    }

    public static boolean hasUnfinishedRide(Context context) {
        return RideSessionStore.load(context.getApplicationContext()) != null;
    }

    public static void clear(Context context) {
        Context appContext = context.getApplicationContext();
        RideSessionStore.clear(appContext);
        RideRouteCache.clear(appContext);
    }
}
