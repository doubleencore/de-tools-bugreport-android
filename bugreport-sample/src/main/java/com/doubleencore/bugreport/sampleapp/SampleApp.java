package com.doubleencore.bugreport.sampleapp;

import android.app.Application;

import com.doubleencore.bugreport.BugReport;

/**
 * Created on 9/1/15.
 */
public class SampleApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        BugReport.setup(this);
        BugReport.setupJira("<JIRA_Project_Key>", "<JIRA_Username>", "<JIRA_Password>");
    }
}
