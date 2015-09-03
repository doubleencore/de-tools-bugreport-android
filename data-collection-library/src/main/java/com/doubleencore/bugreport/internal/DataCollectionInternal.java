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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.doubleencore.bugreport.lib.R;

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

    public static void setup(@NonNull Application application) {
        mDataCollection = new DataCollectionInternal(application);
    }

    /**
     * Builds a notification which when tapped shares the file
     * @param file The file to attempt to share
     */
    private void showNotification(@NonNull File file) {

        //send email
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, mApp.getString(R.string.bug_report));
        shareIntent.setType("application/zip");

        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        Intent intent = Intent.createChooser(shareIntent, mApp.getString(R.string.share));

        PendingIntent pendingIntent = PendingIntent.getActivity(mApp, 0, intent, 0);

        Notification notification = new NotificationCompat.Builder(mApp)
                .setContentTitle(mApp.getString(R.string.notification_title, getAppName()))
                .setContentText(mApp.getString(R.string.notification_text))
                .setContentIntent(pendingIntent)
                .setLargeIcon(BitmapFactory.decodeResource(mApp.getResources(), android.R.drawable.ic_menu_share))
                .setSmallIcon(android.R.drawable.ic_menu_share)
                .setLocalOnly(true)
                .build();

        notification.flags = Notification.FLAG_AUTO_CANCEL;

        NotificationManager notificationManager = (NotificationManager) mApp.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(Integer.MAX_VALUE, notification);
    }

    @NonNull
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
    private File collectDeviceInfo(@NonNull Context context) throws IOException {
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

    /**
     * Execute the data collection task
     */
    public void execute(@Nullable File file) {
        List<File> files = new ArrayList<>();
        if (file != null) {
            files.add(file);
        }

        try {
            collectDeviceInfo(mApp.getApplicationContext());
        } catch (IOException e) {
            Log.e(TAG, "Unable to create device info file: ", e);
        }

        files.addAll(addApplicationFolders());

        new CollectorAsyncTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,
                files.toArray(new File[files.size()]));
    }

    /**
     * Gets the build number assigned by Jenkins
     * @param context context to the app
     * @param packageName Package name to look for
     * @return Value assigned by Jenkins, -1 if not found (ie, dev build)
     */
    private int getBuildNumber(@NonNull Context context, String packageName) {
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
    public void onScreenshot(@NonNull File screenshot) {
        execute(screenshot);
    }

    private List<File> addApplicationFolders() {
        return ZipUtils.getFiles(new File(mApp.getApplicationInfo().dataDir), true);
    }

    private class CollectorAsyncTask extends AsyncTask<File , Void, File> {
        @Override
        protected File doInBackground(@NonNull File... files) {
            try {
                return ZipUtils.generateZip(mApp.getApplicationContext().getExternalCacheDir(), "bugreport.zip", files);
            } catch (IOException e) {
                Log.e(TAG, "IOException: " + e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(File file) {
            if (file != null) {
                showNotification(file);
            } else {
                Toast.makeText(mApp, "Unable to create bug report", Toast.LENGTH_SHORT).show();
            }

        }
    }
}
