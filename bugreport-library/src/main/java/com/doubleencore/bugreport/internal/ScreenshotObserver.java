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
import java.util.concurrent.TimeUnit;

/**
 * Created on 4/3/14.
 */
public class ScreenshotObserver {

    private static final String TAG = ScreenshotObserver.class.getSimpleName();
    private static final long OBSERVE_FREQUENCY = TimeUnit.SECONDS.toMillis(1L);

    private static volatile FileObserver mFileObserver;
    private static ScreenshotListener mListener;

    private static FileAlterationObserver mFileAlterationObserver;
    private static FileAlterationMonitor mFileAlterationMonitor;

    /**
     * Enable observing the screenshots directory to be called back when a screenshot is taken on the device
     * @param listener receiving callbacks of files created in the screenshot directory
     * @return true if enabling was successful, false otherwise
     */
    private static boolean enableObserverDefault(final ScreenshotListener listener) {
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
    private static boolean enableObserverMarshmallow(final ScreenshotListener listener) {
        mListener = listener;
        if (mFileAlterationObserver == null) {
            try {
                mFileAlterationObserver = new FileAlterationObserver(getScreenshotDirectory());
                mFileAlterationObserver.addListener(mFileAlterationListener);
                mFileAlterationObserver.initialize();
                mFileAlterationMonitor = new FileAlterationMonitor(OBSERVE_FREQUENCY);
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

    public static boolean enableObserver(final ScreenshotListener listener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ScreenshotObserver.enableObserverMarshmallow(listener);
        } else {
            return ScreenshotObserver.enableObserverDefault(listener);
        }
    }

    /**
     * Disable the observer and stop receiving callbacks regarding screenshots
     */
    public static void disableObserver() {
        mListener = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            disableObserverMarshmallow();
        } else {
            disableObserverDefault();
        }

    }

    private static void disableObserverDefault() {
        if (mFileObserver != null) {
            mFileObserver.stopWatching();
        }
    }

    private static void disableObserverMarshmallow() {
        if (mFileAlterationMonitor != null) {
            try {
                mFileAlterationMonitor.stop();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping file monitor: " + e);
            }
        }
        if (mFileAlterationObserver != null) {
            try {
                mFileAlterationObserver.removeListener(mFileAlterationListener);
                mFileAlterationObserver.destroy();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping file observer: " + e);
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

    private static FileAlterationListener mFileAlterationListener = new FileAlterationListener() {
        @Override
        public void onStart(FileAlterationObserver observer) {

        }

        @Override
        public void onDirectoryCreate(File directory) {

        }

        @Override
        public void onDirectoryChange(File directory) {

        }

        @Override
        public void onDirectoryDelete(File directory) {

        }

        @Override
        public void onFileCreate(File file) {
            if (mListener != null) {
                mListener.onScreenshot(file);
            }
        }

        @Override
        public void onFileChange(File file) {

        }

        @Override
        public void onFileDelete(File file) {

        }

        @Override
        public void onStop(FileAlterationObserver observer) {

        }
    };
}
