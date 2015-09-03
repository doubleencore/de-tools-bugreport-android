package com.doubleencore.bugreport.internal;

import android.support.annotation.NonNull;

import java.io.File;

/**
 * Created on 4/3/14.
 */
public interface ScreenshotListener {
    void onScreenshot(@NonNull File screenshot);
}
