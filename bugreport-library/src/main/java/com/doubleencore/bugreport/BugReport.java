package com.doubleencore.bugreport;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.doubleencore.bugreport.internal.BugReportInternal;
import com.doubleencore.bugreport.internal.ScreenshotObserver;

/**
 * Created by chris on 9/2/15.
 */
public class BugReport {

    public static final int ENABLE_OBSERVER = 250;
    public static final int EXECUTE_COLLECTION = 251;

    /** Utility to help collect files related to an apps current state and generate a zip file
     * @param application Reference to the application looking to collect data
     *
     */
    public static void setup(@NonNull Application application) {
        BugReportInternal.setup(application);
    }

    public static void executeCollection(@NonNull Activity activity) {
        if (checkPermissions(activity, BugReport.EXECUTE_COLLECTION)) {
            BugReportInternal.getInstance().execute();
        }
    }

    public static void enableObserver(@NonNull Activity activity) {
        if (checkPermissions(activity, BugReport.ENABLE_OBSERVER)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ScreenshotObserver.enableObserverMushroom(BugReportInternal.getInstance());
            } else {
                ScreenshotObserver.enableObserver(BugReportInternal.getInstance());
            }
        }
    }

    public static void disableObserver() {
        ScreenshotObserver.disableObserver();
    }

    private static boolean checkPermissions(Activity activity, int requestCode) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);
            return false;
        } else {
            return true;
        }
    }

}
