package com.doubleencore.bugreport.sampleapp;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.doubleencore.bugreport.BugReport;
import com.doubleencore.bugreportsample.app.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button = (Button) findViewById(R.id.bug_report);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BugReport.executeCollection(MainActivity.this);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean granted = false;
        if (grantResults.length > 0) {
            granted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
        switch (requestCode) {
            case BugReport.ENABLE_OBSERVER:
                if (granted) BugReport.enableObserver(this);
                break;
            case BugReport.EXECUTE_COLLECTION:
                if (granted) BugReport.executeCollection(this);
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        BugReport.enableObserver(this);
    }

    @Override
    protected void onPause() {
        BugReport.enableObserver(this);
        super.onPause();
    }
}
