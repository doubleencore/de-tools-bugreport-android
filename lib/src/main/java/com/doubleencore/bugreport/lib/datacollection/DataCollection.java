package com.doubleencore.bugreport.lib.datacollection;

import android.Manifest;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresPermission;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.doubleencore.bugreport.lib.screenshot.ScreenshotListener;
import com.doubleencore.bugreport.lib.screenshot.ScreenshotObserver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created on 4/2/14.
 */
public class DataCollection implements ScreenshotListener {

    private static final String TAG = DataCollection.class.getSimpleName();

    private boolean mFilesSet = false;
    private List<File> mFiles = new ArrayList<>();
    private AsyncTask<Void, Void, File> mAsyncTask;
    private static DataCollection mDataCollection;
    private Application mApp;


    @RequiresPermission (allOf = {Manifest.permission_group.STORAGE})
    public static void setup(Application application) {
        mDataCollection = new DataCollection(application);
    }

    /** Utility to help collect files related to an apps current state and generate a zip file
     * @param application
     *
     */
    private DataCollection(final Application application) {

        mApp = application;

        mAsyncTask = new AsyncTask<Void, Void, File>() {

            @Override
            protected File doInBackground(Void... Void) {
                File deviceInfo = null;
                try {
                    deviceInfo = collectDeviceInfo(mApp.getApplicationContext());
                    mFiles.add(deviceInfo);

                    File[] bugs = mFiles.toArray(new File[mFiles.size()]);

                    return ZipUtils.generateZip(mApp.getApplicationContext().getExternalCacheDir(), "bugreport.zip", bugs);
                } catch (IOException e) {
                    Log.e(TAG, "IOException: " + e);
                    return null;
                } finally {
                    if (deviceInfo != null && deviceInfo.exists()) {
                        // Ensure we clean up our files
                        deviceInfo.delete();
                    }
                }
            }

            @Override
            protected void onPostExecute(File file) {
                showNotification(file);
                mFiles.clear();
                mFilesSet = false;
            }
        };
    }

    private void showNotification(File file) {

        //send email
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Bug Report");
        shareIntent.setType("application/zip");

        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        Intent intent = Intent.createChooser(shareIntent, "Share");

        Context context = mApp.getApplicationContext();

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        Notification notification = new NotificationCompat.Builder(context)
                .setContentTitle(getAppName() + " bug report")
                .setContentText("Tap to share the bug report with a developer")
                .setContentIntent(pendingIntent)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), android.R.drawable.ic_menu_share))
                .setSmallIcon(android.R.drawable.ic_menu_share)
                .setLocalOnly(true)
                .build();

        notification.flags = Notification.FLAG_AUTO_CANCEL;

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(Integer.MAX_VALUE, notification);
    }

    private String getAppName() {
        return mApp.getApplicationContext().getString(mApp.getApplicationInfo().labelRes);
    }

    private static DataCollection getInstance() {
        return mDataCollection;
    }

    public static void enableObserver() {
        ScreenshotObserver.enableObserver(DataCollection.getInstance());
    }

    public static void disableObserver() {
        ScreenshotObserver.disableObserver();
    }

    /**
     * Collects basic info about the device and current build
     * @param context context for the app
     * @return File which was generated that contains the information
     * @throws java.io.IOException
     */
    private File collectDeviceInfo(Context context) throws IOException {
        File deviceInfo = new File(context.getFilesDir().getParent(), "device_info.txt");
        if (deviceInfo.exists()) {
            deviceInfo.delete();
            deviceInfo.createNewFile();
        }

        FileWriter fw = new FileWriter(deviceInfo.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);

        bw.write("Manufacturer: " + Build.MANUFACTURER);
        bw.newLine();
        bw.write("Device: " + Build.MODEL);
        bw.newLine();
        bw.write("OS API: " + Build.VERSION.SDK_INT);
        bw.newLine();
        bw.write("Build #: " + getBuildNumber(context, context.getPackageName()));

        bw.close();
        return deviceInfo;
    }

    /**
     * Add the list of files to add to a directory
     * @param baseDirectory Directory to look for files in
     * @param searchRecursively true if it should collect files in sub directories
     * @return this
     */
    public DataCollection addFolder(File baseDirectory, boolean searchRecursively) {
        mFiles.addAll(ZipUtils.getFiles(baseDirectory, searchRecursively));
        mFilesSet = true;
        return this;
    }

    public DataCollection addFile(File file) {
        mFiles.add(file);
        mFilesSet = true;
        return this;
    }

    /**
     * Execute the data collection task
     * Must call {@link #addFile(java.io.File)} or {@link #addFolder(java.io.File, boolean)} prior to calling this method
     * @return this
     */
    private DataCollection execute() {
        if (!mFilesSet) {
            throw new IllegalStateException("Must call addFile() or addFolder() before calling execute()");
        }
        mAsyncTask.execute();
        return this;
    }

    /**
     * Gets the build number assigned by Jenkins
     * @param context context to the app
     * @param packageName Package name to look for
     * @return Value assigned by Jenkins, -1 if not found (ie, dev build)
     */
    private int getBuildNumber(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            ApplicationInfo ai = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            if (null != ai.metaData && ai.metaData.containsKey("de_build_version")) {
                return ai.metaData.getInt("de_build_version");
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Name not found: ", e);
        }
        return -1;
    }

    @Override
    public void onScreenshot(String path) {
        addFile(new File(path));
        addApplicationFolders();
        execute();
    }

    public static void executeCollection() {
        addApplicationFolders();
        DataCollection.getInstance().execute();
    }

    private static void addApplicationFolders() {
        DataCollection dc = DataCollection.getInstance();
        dc.addFolder(dc.mApp.getFilesDir(), true);
    }
}
