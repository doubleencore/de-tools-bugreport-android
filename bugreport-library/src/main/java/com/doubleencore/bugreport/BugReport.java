package com.doubleencore.bugreport;

import android.Manifest;
import android.app.Application;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;

import com.doubleencore.bugreport.internal.BugReportInternal;
import com.doubleencore.bugreport.internal.ScreenshotObserver;

/**
 * Created by chris on 9/2/15.
 */
public class BugReport {

    /** Utility to help collect files related to an apps current state and generate a zip file
     * @param application Reference to the application looking to collect data
     *
     */
    @RequiresPermission(allOf = {Manifest.permission_group.STORAGE})
    public static void setup(@NonNull Application application) {
        BugReportInternal.setup(application);
    }

    public static void executeCollection() {
        BugReportInternal.getInstance().execute();
    }

    public static void enableObserver() {
        ScreenshotObserver.enableObserver(BugReportInternal.getInstance());
    }

    public static void disableObserver() {
        ScreenshotObserver.disableObserver();
    }
}
