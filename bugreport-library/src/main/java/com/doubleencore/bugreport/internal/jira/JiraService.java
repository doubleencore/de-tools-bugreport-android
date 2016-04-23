package com.doubleencore.bugreport.internal.jira;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.doubleencore.bugreport.internal.BugReportInternal;
import com.doubleencore.bugreport.lib.BuildConfig;
import com.doubleencore.bugreport.lib.R;
import com.google.gson.Gson;

import net.gotev.uploadservice.MultipartUploadRequest;
import net.gotev.uploadservice.UploadNotificationConfig;
import net.gotev.uploadservice.UploadService;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class JiraService implements Callback {

    private static final String TAG = JiraService.class.getSimpleName();

    private static final String JIRA_SERVER_URL = "https://doubleencore.atlassian.net";
    private static final String CREATE_ISSUE_PATH = "/rest/api/2/issue/";
    private static final String ATTACHMENT_PATH_FORMAT = "/rest/api/2/issue/%s/attachments";
    private static final String DEFAULT_ISSUE_TYPE = "Bug";
    private static final String HEADER_KEY_XTOKEN = "X-Atlassian-Token";
    private static final String HEADER_VALUE_XTOKEN = "nocheck";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final MediaType TYPE_JSON = MediaType.parse("application/json; charset=utf-8");
    private static final int RESPONSE_CREATED = 201;

    private OkHttpClient mHttpClient = new OkHttpClient();
    private Gson mGson = new Gson();
    private Context mContext;
    private String mAttachmentPath;
    private String mProjectKey;
    private String mUsername;
    private String mPassword;

    public JiraService(Context context) {
        UploadService.NAMESPACE = BuildConfig.APPLICATION_ID;
        mContext = context;
        mProjectKey = BugReportInternal.getJiraProjectKey();
        mUsername = BugReportInternal.getJiraUsername();
        mPassword = BugReportInternal.getJiraPassword();
    }

    public void createJiraIssue(@NonNull Uri attachmentUri, String summary, String description) {
        mAttachmentPath = attachmentUri.getPath();

        CreateIssue createIssue = new CreateIssue(mProjectKey, summary, description, DEFAULT_ISSUE_TYPE);

        String credential = Credentials.basic(mUsername, mPassword);
        RequestBody body = RequestBody.create(TYPE_JSON, mGson.toJson(createIssue));
        Request request = new Request.Builder()
                .url(JIRA_SERVER_URL + CREATE_ISSUE_PATH)
                .post(body)
                .addHeader(HEADER_AUTHORIZATION, credential)
                .build();

        mHttpClient.newCall(request).enqueue(this);
    }

    private void uploadAttachment(String issueId, String key) {
        String url = JIRA_SERVER_URL + String.format(ATTACHMENT_PATH_FORMAT, issueId);

        try {
            MultipartUploadRequest uploadRequest = new MultipartUploadRequest(mContext, url)
                    .addFileToUpload(mAttachmentPath, "file")
                    .addHeader(HEADER_KEY_XTOKEN, HEADER_VALUE_XTOKEN)
                    .setMaxRetries(2)
                    .setBasicAuth(mUsername, mPassword)
                    .setNotificationConfig(new UploadNotificationConfig()
                            .setCompletedMessage(mContext.getString(R.string.notification_created, key))
                            .setErrorMessage(mContext.getString(R.string.notification_error, key))
                            .setInProgressMessage(mContext.getString(R.string.notification_attaching, key)))
                    .setAutoDeleteFilesAfterSuccessfulUpload(true);
            uploadRequest.startUpload();
        } catch (FileNotFoundException | MalformedURLException e) {
            Log.e(TAG, "Failed to attached: " + e);
            Toast.makeText(mContext, mContext.getString(R.string.notification_error, key), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {
        if (response.code() == RESPONSE_CREATED) {
            CreateIssueResponse issueResponse = mGson.fromJson(response.body().string(), CreateIssueResponse.class);
            uploadAttachment(issueResponse.id, issueResponse.key);
        } else {
            Log.e(TAG, "Failed to create ticket: " + response.toString());
            Toast.makeText(mContext, mContext.getString(R.string.jira_error, mProjectKey), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onFailure(Call call, IOException e) {
        Log.e(TAG, "Failed to create ticket: " + e.toString());
        Toast.makeText(mContext, mContext.getString(R.string.jira_error, mProjectKey), Toast.LENGTH_SHORT).show();
    }
}
