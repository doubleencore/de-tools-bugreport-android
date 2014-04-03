package com.detools.bugreport.lib;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Created on 4/2/14.
 */
public class DataCollection {

    private static final String TAG = DataCollection.class.getSimpleName();

    private DataCollectionListener mListener;
    private boolean mFilesSet = false;
    private List<File> mFiles;
    private File mOutputDirectory;
    private String mOutputName;
    private Exception mException;
    private AsyncTask<Void, Void, File> mAsyncTask;

    /** Utility to help collect files related to an apps current state and generate a zip file
     * @param outputDirectory Directory to store output in
     * @param outputName File name for generated zip file
     * @param collectDeviceInfo generates a file containing some basic device info
     * @param context required only if collectDeviceInfo is true
     */
    public DataCollection(File outputDirectory, String outputName, final boolean collectDeviceInfo, final Context context) {
        mOutputDirectory = outputDirectory;
        mOutputName = outputName;

        mAsyncTask = new AsyncTask<Void, Void, File>() {
            @Override
            protected File doInBackground(Void... Void) {
                File deviceInfo = null;
                try {
                    if (collectDeviceInfo) {
                        deviceInfo = collectDeviceInfo(context);
                        mFiles.add(deviceInfo);
                    }
                    File[] bugs = mFiles.toArray(new File[mFiles.size()]);

                    return ZipUtils.generateZip(mOutputDirectory, mOutputName, bugs);
                } catch (IOException e) {
                    mException = e;
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
                if (mListener != null) {
                    if (file != null) {
                        mListener.onCollectionCompleted(file);
                    } else if (mException != null) {
                        mListener.onCollectionFailed(mException);
                    }
                }
            }
        };
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

        bw.write("Manufacture: " + Build.MANUFACTURER);
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
     * Sets the list of files to add to a directory
     * Overwrites any previously set files
     * @param baseDirectory Directory to look for files in
     * @param searchRecursively true if it should collect files in sub directories
     * @return this
     */
    public DataCollection collectionFolder(File baseDirectory, boolean searchRecursively) {
        mFiles = ZipUtils.getFiles(baseDirectory, searchRecursively);
        mFilesSet = true;
        return this;
    }

    /**
     * Sets the list of files to add to a directory
     * Overwrites any previously set files
     * @param files List of files to add to the zip file
     * @return this
     */
    public DataCollection setFiles(List<File> files) {
        mFiles = files;
        mFilesSet = true;
        return this;
    }

    /**
     * Execute the data collection task
     * Must call {@link #setFiles(java.util.List)} or {@link #collectionFolder(java.io.File, boolean)} prior to calling this method
     * @return this
     */
    public DataCollection execute() {
        if (!mFilesSet) {
            throw new IllegalStateException("Must call setFiles() or collectionFolder() before calling execute()");
        }
        mAsyncTask.execute();
        return this;
    }

    /**
     * Attempt to cancel the task.
     * See {@link android.os.AsyncTask#cancel(boolean)} for details.
     * @param mayInterruptIfRunning
     */
    public void cancel(boolean mayInterruptIfRunning) {
        mAsyncTask.cancel(mayInterruptIfRunning);
    }

    /**
     * Sets the listener for callbacks
     * @param listener Listener for sending callbacks
     */
    public void setListener(DataCollectionListener listener) {
        mListener = listener;
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
}
