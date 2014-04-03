package com.detools.bugreport.lib;

import java.io.File;

/**
 * Created on 4/2/14.
 */
public interface DataCollectionListener {

    /**
     * Callback indicating completion
     * @param file reference to the file generated
     */
    public void onCollectionCompleted(File file);

    /**
     * Callback indicating a failure occurred
     * @param e exception which occurred
     */
    public void onCollectionFailed(Exception e);
}
