package com.doubleencore.bugreportsample.app;

import android.content.DialogInterface;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.doubleencore.bugreportsample.app.R;
import com.doubleencore.bugreport.lib.datacollection.DataCollection;
import com.doubleencore.bugreport.lib.datacollection.DataCollectionListener;
import com.doubleencore.bugreport.lib.datacollection.DataDialogFragment;
import com.doubleencore.bugreport.lib.screenshot.ScreenshotDialogFragment;
import com.doubleencore.bugreport.lib.screenshot.ScreenshotListener;
import com.doubleencore.bugreport.lib.screenshot.ScreenshotObserver;

import java.io.File;

public class MainActivity extends ActionBarActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button = (Button) findViewById(R.id.bug_report);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dataCollection(null);
            }
        });

        ToggleButton monitorScreenShot = (ToggleButton) findViewById(R.id.screenshot_toggle);
        monitorScreenShot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    ScreenshotObserver.enableObserver(new ScreenshotListener() {
                        @Override
                        public void onScreenshot(final String path) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    final ScreenshotDialogFragment frag = ScreenshotDialogFragment.newInstance(new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            switch (which) {
                                                case DialogInterface.BUTTON_POSITIVE:
                                                    dataCollection(path);
                                                default:
                                                    dialog.dismiss();
                                            }

                                        }
                                    });
                                    frag.show(getSupportFragmentManager(), "SCREENSHOTDIALOG");
                                    Toast.makeText(MainActivity.this, "onScreenshot: " + path, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                } else {
                    ScreenshotObserver.disableObserver();
                }
            }
        });
    }

    public void dataCollection(String additionalFile) {
        final DataCollection dc = new DataCollection(Environment.getExternalStorageDirectory(), "bugreport.zip", true, this);

        final DataDialogFragment frag = DataDialogFragment.newInstance(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_NEGATIVE:
                        dc.cancel(true);
                        dialog.dismiss();
                        break;
                }

            }
        });
        frag.show(getSupportFragmentManager(), "DATADIALOGFRAGMENT");

        dc.setListener(new DataCollectionListener() {
            @Override
            public void onCollectionCompleted(File file) {
                frag.dismissAllowingStateLoss();
                Toast.makeText(MainActivity.this, "onCollectionCompleted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCollectionFailed(Exception e) {
                Log.e(TAG, "onCollectionFailed: " + e);
                Toast.makeText(MainActivity.this, "onCollectionFailed", Toast.LENGTH_SHORT).show();
            }
        });
        dc.addFolder(getFilesDir().getParentFile(), true);
        if (!TextUtils.isEmpty(additionalFile)) {
            dc.addFile(new File(additionalFile));
        }
        dc.execute();
    }
}
