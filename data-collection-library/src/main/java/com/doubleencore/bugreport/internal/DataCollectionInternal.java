package com.doubleencore.bugreport.internal;

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
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created on 4/2/14.
 */
public class DataCollectionInternal implements ScreenshotListener {

    private static final String TAG = DataCollectionInternal.class.getSimpleName();

    private static DataCollectionInternal mDataCollection;
    private Application mApp;


    private DataCollectionInternal(final Application application) {
        mApp = application;
    }

    public static void setup(Application application) {
        mDataCollection = new DataCollectionInternal(application);
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

    public static DataCollectionInternal getInstance() {
        return mDataCollection;
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
     * Execute the data collection task
     */
    public void execute() {
        execute(null);
    }

    public void execute(File file) {
        List<File> files = new ArrayList<>();
        if (file != null) {
            files.add(file);
        }

        try {
            File deviceInfo = collectDeviceInfo(mApp.getApplicationContext());
            files.add(deviceInfo);
        } catch (IOException e) {
            Log.e(TAG, "Unable to create device info file: ", e);
        }

        files.addAll(addApplicationFolders());

        File[] bugs = files.toArray(new File[files.size()]);

        new CollectorAsyncTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, bugs);
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
        execute(new File(path));
    }

    private List<File> addApplicationFolders() {
        return ZipUtils.getFiles(mApp.getFilesDir(), true);
    }

    private class CollectorAsyncTask extends AsyncTask<File , Void, File> {
        @Override
        protected File doInBackground(File... files) {
            try {
                return ZipUtils.generateZip(mApp.getApplicationContext().getExternalCacheDir(), "bugreport.zip", files);
            } catch (IOException e) {
                Log.e(TAG, "IOException: " + e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(File file) {
            showNotification(file);
        }
    }
}
