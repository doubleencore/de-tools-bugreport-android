package com.doubleencore.bugreport;

import android.Manifest;
import android.app.Application;
import android.support.annotation.RequiresPermission;

import com.doubleencore.bugreport.internal.DataCollectionInternal;
import com.doubleencore.bugreport.internal.ScreenshotObserver;

/**
 * Created by chris on 9/2/15.
 */
public class DataCollection {

    /** Utility to help collect files related to an apps current state and generate a zip file
     * @param application Reference to the application looking to collect data
     *
     */
    @RequiresPermission(allOf = {Manifest.permission_group.STORAGE})
    public static void setup(Application application) {
        DataCollectionInternal.setup(application);
    }

    public static void executeCollection() {
        DataCollectionInternal.getInstance().execute();
    }

    public static void enableObserver() {
        ScreenshotObserver.enableObserver(DataCollectionInternal.getInstance());
    }

    public static void disableObserver() {
        ScreenshotObserver.disableObserver();
    }
}
