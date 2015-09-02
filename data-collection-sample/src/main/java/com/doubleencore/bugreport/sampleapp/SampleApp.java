package com.doubleencore.bugreport.sampleapp;

import android.app.Application;

import com.doubleencore.bugreport.DataCollection;

/**
 * Created by chris on 9/1/15.
 */
public class SampleApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        DataCollection.setup(this);
    }
}
