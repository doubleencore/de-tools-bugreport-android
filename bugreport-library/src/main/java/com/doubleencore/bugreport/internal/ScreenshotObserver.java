package com.doubleencore.bugreport.internal;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Environment;
import android.os.FileObserver;
import android.util.Log;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Created on 4/3/14.
 */
public class ScreenshotObserver {

    private static final String TAG = ScreenshotObserver.class.getSimpleName();

    private static volatile FileObserver mFileObserver;
    private static ScreenshotListener mListener;

    private static FileAlterationObserver mFileAlterationObserver;
    private static FileAlterationListener mFileAlterationListener;
    private static FileAlterationMonitor mFileAlterationMonitor;

    /**
     * Enable observing the screenshots directory to be called back when a screenshot is taken on the device
     * @param listener receiving callbacks of files created in the screenshot directory
     * @return true if enabling was successful, false otherwise
     */
    public static boolean enableObserver(final ScreenshotListener listener) {
        mListener = listener;
        if (mFileObserver == null) {
            try {
                final File screenshotsFolder = getScreenshotDirectory();
                mFileObserver = new FileObserver(screenshotsFolder.getPath(), FileObserver.CLOSE_WRITE) {
                    @Override
                    public void onEvent(int event, String path) {
                        if (mListener != null && path != null) {
                            mListener.onScreenshot(new File(screenshotsFolder.getPath() + File.separator + path));
                        }
                    }
                };
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Screenshot directory not found: " + e);
                return false;
            }

        }

        mFileObserver.startWatching();
        return true;
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static boolean enableObserverMushroom(final FileAlterationListener listener) {
        mFileAlterationListener = listener;
        if (mFileAlterationObserver == null) {
            try {
                mFileAlterationObserver = new FileAlterationObserver(getScreenshotDirectory());
                mFileAlterationObserver.addListener(mFileAlterationListener);
                mFileAlterationObserver.initialize();
                mFileAlterationMonitor = new FileAlterationMonitor(1000L);
                mFileAlterationMonitor.addObserver(mFileAlterationObserver);
                mFileAlterationMonitor.start();
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Screenshot directory not found: " + e);
                return false;
            } catch (Exception e) {
                Log.e(TAG, "Error starting file monitor: " + e);
                return false;
            }
        }
        return true;
    }

    /**
     * Disable the observer and stop receiving callbacks regarding screenshots
     */
    public static void disableObserver() {
        mListener = null;
        mFileAlterationListener = null;
        if (mFileObserver != null) {
            mFileObserver.stopWatching();
        }
        if (mFileAlterationMonitor != null) {
            try {
                mFileAlterationMonitor.stop();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping file monitor: " + e);
            }
        }
    }

    /**
     * Attempt to locate the folder the screenshots are stored in.
     * @return screenshot directory
     * @throws FileNotFoundException if the directory is not found
     */
    private static File getScreenshotDirectory() throws FileNotFoundException {
        File screenshotFolder = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Screenshots");
        if (!screenshotFolder.exists() || !screenshotFolder.isDirectory()) {
            throw new FileNotFoundException(screenshotFolder.getAbsolutePath());
        }
        return screenshotFolder;
    }
}
