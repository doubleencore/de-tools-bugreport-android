package com.doubleencore.bugreport;

import android.app.Activity;
import android.app.Application;


/**
 * Created by chris on 9/2/15.
 */
public class BugReport {

    public static final int ENABLE_OBSERVER = 500;
    public static final int EXECUTE_COLLECTION = 501;

    public static void setup(Application application) { }

    public static void executeCollection(Activity activity) { }

    public static void enableObserver(Activity activity) { }

    public static void disableObserver() { }
}
