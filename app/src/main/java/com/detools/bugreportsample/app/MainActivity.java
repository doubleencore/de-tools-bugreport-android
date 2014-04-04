package com.detools.bugreportsample.app;

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

import com.detools.bugreport.lib.DataCollection;
import com.detools.bugreport.lib.DataCollectionListener;
import com.detools.bugreport.lib.ScreenshotListener;
import com.detools.bugreport.lib.ScreenshotObserver;

import java.io.File;

public class MainActivity extends ActionBarActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button bugReport = (Button) findViewById(R.id.bug_report);
        bugReport.setOnClickListener(new View.OnClickListener() {
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
                                    dataCollection(path);
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
        DataCollection dc = new DataCollection(Environment.getExternalStorageDirectory(), "bugreport.zip", true, this);
        dc.setListener(new DataCollectionListener() {
            @Override
            public void onCollectionCompleted(File file) {
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
