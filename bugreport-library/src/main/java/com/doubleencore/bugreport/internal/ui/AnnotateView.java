package com.doubleencore.bugreport.internal.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.doubleencore.bugreport.internal.BugReportInternal;
import com.doubleencore.bugreport.internal.jira.CreateIssueActivity;
import com.doubleencore.bugreport.lib.R;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import java.io.File;
import java.util.Calendar;


/**
 * Created on 3/11/16.
 */
public class AnnotateView extends FrameLayout implements BugReportInternal.CollectorListener {

    private static final int TRIPLE_CLICK = 3;
    private static final int TAP_DELAY = 250;

    private CanvasView canvas;
    private FloatingActionsMenu fam;
    private ProgressBar progress;

    private long lastClick;
    private int clickCount;

    public AnnotateView(Context context) {
        super(context);
        init(context);
    }

    public AnnotateView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AnnotateView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public AnnotateView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.annotate_view, this);
        canvas = (CanvasView) findViewById(R.id.canvas);
        progress = (ProgressBar) findViewById(R.id.progress_bar);

        fam = (FloatingActionsMenu) findViewById(R.id.fam);
        findViewById(R.id.draw).setOnClickListener(fabClickListener);
        findViewById(R.id.clear).setOnClickListener(fabClickListener);
        findViewById(R.id.send_email).setOnClickListener(fabClickListener);
        FloatingActionButton jira = (FloatingActionButton) findViewById(R.id.send_jira);
        if (BugReportInternal.isJiraEnabled()) {
            jira.setOnClickListener(fabClickListener);
        } else {
            fam.removeButton(jira);
        }

        hideAnnotation();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                boolean tripleClick = trackClicks();
                if (tripleClick) {
                    showAnnotation();
                }
                return tripleClick;
        }
        return false;
    }

    private boolean trackClicks() {
        long now = Calendar.getInstance().getTimeInMillis();
        if (now < lastClick + TAP_DELAY) {
            clickCount++;
            lastClick = now;
            if (clickCount == TRIPLE_CLICK) {
                clickCount = 0;
                return true;
            }
        } else {
            lastClick = now;
            clickCount = 1;
        }
        return false;
    }

    private void hideAnnotation() {
        canvas.setVisibility(View.GONE);
        fam.setVisibility(View.GONE);
    }

    private void showAnnotation() {
        canvas.setVisibility(View.VISIBLE);
        fam.setVisibility(View.VISIBLE);
    }

    private Bitmap captureScreen() {
        View rootView = getRootView();
        rootView.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(rootView.getDrawingCache());
        rootView.setDrawingCacheEnabled(false);
        return bitmap;
    }

    private final OnClickListener fabClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {

            int i = v.getId();
            if (i == R.id.draw) {
                canvas.setDrawable(true);
                fam.collapse();
            } else if (i == R.id.send_email) {
                fam.collapseImmediately();

                Bitmap screen = captureScreen();
                BugReportInternal.getInstance().execute(screen, null);

                canvas.clear();
                hideAnnotation();
            } else if (i == R.id.clear) {
                canvas.clear();
                fam.collapse();
                hideAnnotation();
            } else if (i == R.id.send_jira) {
                fam.collapseImmediately();

                Bitmap screen = captureScreen();
                BugReportInternal.getInstance().execute(screen, AnnotateView.this);

                progress.setVisibility(View.VISIBLE);
                canvas.clear();
                hideAnnotation();
            } else {
                throw new IllegalStateException("Unknown ID: " + i);
            }
        }
    };

    @Override
    public void collectorCompleted(File file) {
        progress.setVisibility(View.GONE);

        Intent intent = new Intent(getContext(), CreateIssueActivity.class);
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        getContext().startActivity(intent);
    }
}
