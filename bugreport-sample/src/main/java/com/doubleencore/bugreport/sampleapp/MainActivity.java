package com.doubleencore.bugreport.sampleapp;

import android.os.Bundle;
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
                BugReport.executeCollection();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        BugReport.enableObserver();
    }

    @Override
    protected void onPause() {
        BugReport.disableObserver();
        super.onPause();
    }
}
