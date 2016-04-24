package com.doubleencore.bugreport.internal.utils;

import android.content.Context;
import android.support.annotation.NonNull;

/**
 * Created on 4/23/16.
 */
public final class PackageUtils {
    private PackageUtils() {

    }

    @NonNull  public static String getAppName(@NonNull Context ctx) {
        return ctx.getApplicationContext().getString(ctx.getApplicationInfo().labelRes);
    }

}
