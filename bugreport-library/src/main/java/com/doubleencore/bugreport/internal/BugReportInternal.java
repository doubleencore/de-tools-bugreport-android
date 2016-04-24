package com.doubleencore.bugreport.internal;

import android.app.Activity;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.doubleencore.bugreport.internal.jira.CreateIssueActivity;
import com.doubleencore.bugreport.internal.screenshot.ScreenshotListener;
import com.doubleencore.bugreport.internal.utils.ZipUtils;
import com.doubleencore.bugreport.lib.R;
import com.doubleencore.buildutils.BuildUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.doubleencore.bugreport.internal.utils.PackageUtils.getAppName;

/**
 * Created on 4/2/14.
 */
public class BugReportInternal implements ScreenshotListener {

    private static final String TAG = BugReportInternal.class.getSimpleName();

    private static BugReportInternal mDataCollection;
    private static String mJiraProjectKey;
    private static String mJiraUsername;
    private static String mJiraPassword;
    private Application mApp;
    private WeakReference<Activity> mActivity;

    private BugReportInternal(final Application application) {
        mApp = application;
    }

    public static void setup(@NonNull Application application) {
        mDataCollection = new BugReportInternal(application);
    }

    public static void enableJira(@NonNull String projectKey, @NonNull String username, @NonNull String password) {
        mJiraProjectKey = projectKey;
        mJiraUsername = username;
        mJiraPassword = password;
    }

    public static String getJiraProjectKey() {
        return mJiraProjectKey;
    }

    public static String getJiraUsername() {
        return mJiraUsername;
    }

    public static String getJiraPassword() {
        return mJiraPassword;
    }

    public static boolean isJiraEnabled() {
        return !TextUtils.isEmpty(mJiraPassword) &&
                    !TextUtils.isEmpty(mJiraUsername) &&
                    !TextUtils.isEmpty(mJiraPassword);
    }

    /**
     * Builds a notification which when tapped shares the file
     * @param file The file to attempt to share
     */
    private void showNotification(@NonNull File file) {
        Uri fileUri = Uri.fromFile(file);

        // Send email
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, mApp.getString(R.string.bug_report));
        shareIntent.setType("application/zip");
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);

        Intent intent = Intent.createChooser(shareIntent, mApp.getString(R.string.share));
        PendingIntent sharePendingIntent = PendingIntent.getActivity(mApp, 0, intent, 0);

        // Create JIRA Issue
        Intent createJiraIssueIntent = new Intent(mApp, CreateIssueActivity.class);
        createJiraIssueIntent.putExtra(Intent.EXTRA_STREAM, fileUri);

        PendingIntent jiraPendingIntent = PendingIntent.getActivity(mApp, 0, createJiraIssueIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mApp)
                .setContentTitle(mApp.getString(R.string.notification_title, getAppName(mApp)))
                .setLargeIcon(BitmapFactory.decodeResource(mApp.getResources(), android.R.drawable.ic_menu_share))
                .setSmallIcon(android.R.drawable.ic_menu_share)
                .setLocalOnly(true)
                .addAction(android.R.drawable.ic_menu_share, mApp.getString(R.string.share_bug_report), sharePendingIntent);

        if (BugReportInternal.isJiraEnabled()) {
            builder.addAction(R.drawable.ic_jira, mApp.getString(R.string.create_jira_ticket), jiraPendingIntent);
        }

        Notification notification = builder.build();

        notification.flags = Notification.FLAG_AUTO_CANCEL;

        NotificationManager notificationManager = (NotificationManager) mApp.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(Integer.MAX_VALUE, notification);
    }

    public static BugReportInternal getInstance() {
        return mDataCollection;
    }

    public void annotateView(Activity activity) {
        ViewGroup root = (ViewGroup) activity.findViewById(android.R.id.content);

        if (root.findViewById(R.id.annotate_view) == null) {
            FrameLayout fl = (FrameLayout) LayoutInflater.from(activity).inflate(R.layout.annotated_container, root, false);
            root.addView(fl, root.getChildCount());
        }
        mActivity = new WeakReference<>(activity);
    }

    /**
     * Collects basic info about the device and current build
     * @param context context for the app
     * @return File which was generated that contains the information
     * @throws java.io.IOException
     */
    @NonNull
    @WorkerThread
    private File collectDeviceInfo(@NonNull Context context) throws IOException {
        File deviceInfo = new File(context.getExternalCacheDir(), "device_info.txt");
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
        bw.write("Build #: " + BuildUtils.getBuildNumber(mApp.getApplicationContext()));

        bw.close();
        return deviceInfo;
    }

    @Nullable
    @WorkerThread
    private File collectLogcat() throws IOException {

        Process process = Runtime.getRuntime().exec("logcat -v long -d");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        FileOutputStream outputStream;

        File file = getExternalFile(mApp, "logcat.txt");
        outputStream = new FileOutputStream(file);
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            outputStream.write(line.getBytes());
            outputStream.write("\n".getBytes());
        }
        outputStream.close();

        return file;
    }

    @Nullable
    @WorkerThread
    private File saveBitmap(@NonNull Bitmap bitmap) {
        Activity activity = mActivity.get();
        if (activity != null) {
            try {
                // Output file
                File imageFile = getExternalFile(mApp, "ScreenCapture_" + Calendar.getInstance().getTimeInMillis() + ".jpg");

                FileOutputStream outputStream = new FileOutputStream(imageFile);
                int quality = 87;
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
                outputStream.flush();
                outputStream.close();

                return imageFile;
            } catch (IOException e) {
                Log.e(TAG, "File exception: ", e);
                return null;
            }
        } else {
            return null;
        }
    }

    private File getExternalFile(@NonNull  Context context, @NonNull String filename) {
        return new File(context.getExternalFilesDir(null), filename);
    }

    /**
     * Execute the data collection task
     */
    public void execute() {
        new CollectorAsyncTask().execute();
    }

    /**
     * Execute the data collection task
     */
    public void execute(Bitmap bitmap, CollectorListener listener) {
        new CollectorAsyncTask()
                .setCollectorListener(listener)
                .setBitmap(bitmap)
                .execute();
    }

    /**
     * Execute the data collection task
     */
    public void execute(@Nullable File screenshot) {
        new CollectorAsyncTask()
                .setFile(screenshot)
                .execute();
    }

    @Override
    public void onScreenshot(@NonNull File screenshot) {
        execute(screenshot);
    }

    public interface CollectorListener {
        void collectorCompleted(File file);
    }

    private List<File> addApplicationFolders() {
        return ZipUtils.getFiles(new File(mApp.getApplicationInfo().dataDir), true);
    }

    private class CollectorAsyncTask extends AsyncTask<File, Void, File> {

        private CollectorListener mListener;
        private Bitmap mBitmap;
        private File mFile;

        public CollectorAsyncTask setCollectorListener(CollectorListener listener) {
            mListener = listener;
            return this;
        }

        public CollectorAsyncTask setBitmap(Bitmap bitmap) {
            mBitmap = bitmap;
            return this;
        }

        public CollectorAsyncTask setFile(File file) {
            mFile = file;
            return this;
        }

        @Override
        protected File doInBackground(File... screenshot) {
            try {

                List<File> files = new ArrayList<>();
                if (mFile != null) {
                    files.add(mFile);
                }

                if (mBitmap != null) {
                    files.add(saveBitmap(mBitmap));
                }

                File deviceInfo = collectDeviceInfo(mApp.getApplicationContext());
                files.add(deviceInfo);

                File logcat = collectLogcat();
                files.add(logcat);

                files.addAll(addApplicationFolders());

                return ZipUtils.generateZip(mApp.getApplicationContext().getExternalCacheDir(), "bugreport.zip", files);
            } catch (IOException e) {
                Log.e(TAG, "IOException: " + e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(File file) {
            if (mListener != null) {
                mListener.collectorCompleted(file);
            } else if (file != null) {
                showNotification(file);
            } else {
                Toast.makeText(mApp, "Unable to create bug report", Toast.LENGTH_SHORT).show();
            }

        }
    }
}
