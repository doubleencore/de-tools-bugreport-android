package com.doubleencore.bugreport.lib.screenshot;

import android.os.Environment;
import android.os.FileObserver;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Created on 4/3/14.
 */
public class ScreenshotObserver {

    private static final String TAG = ScreenshotObserver.class.getSimpleName();

    private static volatile FileObserver mFileObserver;
    private static ScreenshotListener mListener;
    private static boolean mIsObserving;


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
                            mListener.onScreenshot(screenshotsFolder.getPath() + "/" + path);
                        }
                    }
                };
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Screenshot directory not found: " + e);
                return false;
            }

        }

        mFileObserver.startWatching();
        mIsObserving = true;
        return true;
    }

    public static boolean isObserving() {
        return mIsObserving;
    }

    /**
     * Disable the observer and stop receiving callbacks regarding screenshots
     */
    public static void disableObserver() {
        mListener = null;
        if (mFileObserver != null) {
            mFileObserver.stopWatching();
        }
        mIsObserving = false;
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
