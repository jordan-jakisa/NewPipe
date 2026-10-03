package org.schabi.newpipe.util;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.schabi.newpipe.App;
import org.schabi.newpipe.settings.NewPipeSettings;

public final class PermissionHelper {
    public static final int POST_NOTIFICATIONS_REQUEST_CODE = 779;
    public static final int DOWNLOAD_DIALOG_REQUEST_CODE = 778;
    public static final int DOWNLOADS_REQUEST_CODE = 777;

    private PermissionHelper() { }

    public static boolean checkStoragePermissions(final Activity activity, final int requestCode) {
        if (NewPipeSettings.useStorageAccessFramework(activity)) {
            return true; // Storage permissions are not needed for SAF
        }

        if (!checkReadStoragePermissions(activity, requestCode)) {
            return false;
        }
        return checkWriteStoragePermissions(activity, requestCode);
    }

    public static boolean checkReadStoragePermissions(final Activity activity,
                                                      final int requestCode) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    requestCode);

            return false;
        }
        return true;
    }


    public static boolean checkWriteStoragePermissions(final Activity activity,
                                                       final int requestCode) {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            /*if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {*/

            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);

            // PERMISSION_WRITE_STORAGE is an
            // app-defined int constant. The callback method gets the
            // result of the request.
            /*}*/
            return false;
        }
        return true;
    }

    public static boolean checkPostNotificationsPermission(final Activity activity,
                                                           final int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(activity,
                Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            if (!App.getInstance().getNotificationsRequested()) {
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, requestCode);
                App.getInstance().setNotificationsRequested();
                return false;
            }
        }
        return true;
    }
}
