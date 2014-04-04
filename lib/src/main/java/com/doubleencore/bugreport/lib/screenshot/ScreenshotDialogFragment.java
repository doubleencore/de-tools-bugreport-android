package com.doubleencore.bugreport.lib.screenshot;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.doubleencore.bugreport.lib.R;

/**
 * Created on 4/4/14.
 */
public class ScreenshotDialogFragment extends DialogFragment {

    private DialogInterface.OnClickListener mListener;

    public static ScreenshotDialogFragment newInstance(DialogInterface.OnClickListener clickListener) {
        ScreenshotDialogFragment frag = new ScreenshotDialogFragment();
        frag.mListener = clickListener;
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.screenshot_detected);
        builder.setMessage(R.string.screenshot_prompt);
        builder.setNegativeButton(R.string.screenshot_no, mListener);
        builder.setPositiveButton(R.string.screenshot_yes, mListener);

        return builder.create();
    }
}
