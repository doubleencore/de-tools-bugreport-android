package com.doubleencore.bugreportsample.app;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.doubleencore.bugreport.DataCollection;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button = (Button) findViewById(R.id.bug_report);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DataCollection.executeCollection();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        DataCollection.enableObserver();
    }

    @Override
    protected void onPause() {
        DataCollection.disableObserver();
        super.onPause();
    }
}