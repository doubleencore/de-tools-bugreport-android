package com.doubleencore.bugreport.internal.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.doubleencore.bugreport.internal.BugReportInternal;
import com.doubleencore.bugreport.lib.R;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import java.util.Calendar;


/**
 * Created by chris on 3/11/16.
 */
public class AnnotateView extends FrameLayout {

    private static final int TRIPLE_CLICK = 3;
    private static final int TAP_DELAY = 250;

    private CanvasView canvas;
    private FloatingActionsMenu fam;

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
        fam = (FloatingActionsMenu) findViewById(R.id.fam);
        findViewById(R.id.draw).setOnClickListener(fabClickListener);
        findViewById(R.id.clear).setOnClickListener(fabClickListener);
        findViewById(R.id.send).setOnClickListener(fabClickListener);

        hideAnnotation();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return trackClicks();

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
                showAnnotation();
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

    private final OnClickListener fabClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {

            int i = v.getId();
            if (i == R.id.draw) {
                canvas.setDrawable(true);
                fam.collapse();
            } else if (i == R.id.send) {
                BugReportInternal.getInstance().execute();
                fam.collapse();
                hideAnnotation();
            } else if (i == R.id.clear) {
                canvas.clear();
                fam.collapse();
                hideAnnotation();
            } else {
                throw new IllegalStateException("Unknown ID: " + i);
            }

        }
    };

}
