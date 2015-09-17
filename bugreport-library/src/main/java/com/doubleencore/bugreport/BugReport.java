package com.doubleencore.bugreport;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;

import com.doubleencore.bugreport.common.BaseBugReport;
import com.doubleencore.bugreport.internal.BugReportInternal;
import com.doubleencore.bugreport.internal.ScreenshotObserver;

/**
 * Created by chris on 9/2/15.
 */
public class BugReport extends BaseBugReport {

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
            ScreenshotObserver.enableObserver(BugReportInternal.getInstance());
        }
    }

    public static void disableObserver() {
        ScreenshotObserver.disableObserver();
    }

    private static boolean checkPermissions(Activity activity, int requestCode) {
        if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);
            return false;
        } else {
            return true;
        }
    }

}
