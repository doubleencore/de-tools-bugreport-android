package com.doubleencore.bugreport.lib.datacollection;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;

import com.doubleencore.bugreport.lib.R;

/**
 * Created on 4/4/14.
 */
public class DataDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    private DialogInterface.OnClickListener mListener;

    public static DataDialogFragment newInstance(DialogInterface.OnClickListener clickListener) {
        DataDialogFragment frag = new DataDialogFragment();
        frag.mListener = clickListener;
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        View view = LayoutInflater.from(getActivity())
                .inflate(R.layout.progress_dialog, null);

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        builder.setTitle(R.string.bug_report);
        builder.setCancelable(true);

        builder.setNegativeButton(R.string.cancel, mListener);

        return builder.create();
    }


    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_NEGATIVE:
                dismissAllowingStateLoss();
                break;
        }
    }
}
