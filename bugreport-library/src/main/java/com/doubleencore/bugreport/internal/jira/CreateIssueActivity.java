package com.doubleencore.bugreport.internal.jira;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.doubleencore.bugreport.internal.BugReportInternal;
import com.doubleencore.bugreport.lib.R;

public class CreateIssueActivity extends AppCompatActivity {

    private static final String TAG = CreateIssueActivity.class.getName();

    private EditText mSummaryEdit;
    private EditText mDescriptionEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_jira_issue);

        mSummaryEdit = (EditText) findViewById(R.id.summaryEdit);
        mDescriptionEdit = (EditText) findViewById(R.id.descriptionEdit);

        setTitle(getString(R.string.jira_project_title, BugReportInternal.getJiraProjectKey()));
    }

    public void onCreateIssueClick(View view) {
        String summary = mSummaryEdit.getText().toString();
        String description = mDescriptionEdit.getText().toString();
        Uri attachmentUri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);

        if (TextUtils.isEmpty(summary)) {
            Toast.makeText(this, R.string.please_enter_summary, Toast.LENGTH_SHORT).show();
            return;
        }

        view.setEnabled(false);

        JiraService.start(this, attachmentUri, summary, description);
        finish();
    }
}
