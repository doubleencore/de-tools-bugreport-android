package com.doubleencore.bugreport;

import android.app.Activity;
import android.app.Application;

import com.doubleencore.bugreport.common.BaseBugReport;


/**
 * Created by chris on 9/2/15.
 */
public class BugReport extends BaseBugReport {

    public static void setup(Application application) { }

    public static void executeCollection(Activity activity) { }

    public static void enableObserver(Activity activity) { }

    public static void disableObserver() { }
}
