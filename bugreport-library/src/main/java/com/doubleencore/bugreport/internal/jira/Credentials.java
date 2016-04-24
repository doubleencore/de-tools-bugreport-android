package com.doubleencore.bugreport.internal.jira;

import android.util.Base64;

/**
 * Created on 4/23/16.
 */
public final class Credentials {
    private Credentials() {
    }

    /**
     * Returns an auth credential for the Basic scheme.
     */
    public static String basic(String userName, String password) {
        return "Basic " + Base64.encodeToString((userName + ":" + password).getBytes(), Base64.NO_WRAP);
    }
}