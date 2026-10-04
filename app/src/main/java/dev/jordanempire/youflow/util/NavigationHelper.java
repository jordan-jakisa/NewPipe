package dev.jordanempire.youflow.util;

import android.app.Activity;

import com.jakewharton.processphoenix.ProcessPhoenix;

import dev.jordanempire.youflow.NewPipeDatabase;

/** App level navigation helpers that are still needed by non-Compose code. */
public final class NavigationHelper {
    private NavigationHelper() {
    }

    /**
     * Restarts the whole process, used after a backup restore so the database is reopened.
     *
     * @param activity the activity to finish
     */
    public static void restartApp(final Activity activity) {
        NewPipeDatabase.close();
        ProcessPhoenix.triggerRebirth(activity.getApplicationContext());
    }
}
