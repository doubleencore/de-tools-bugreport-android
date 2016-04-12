package com.doubleencore.bugreport.internal.jira;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.doubleencore.bugreport.internal.BugReportInternal;
import com.doubleencore.bugreport.lib.BuildConfig;
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

    public interface CreateIssueListener {
        void onIssueCreatedSuccess(CreateIssueResponse response);
        void onIssueCreatedFailed();
    }
    private static final String JIRA_SERVER_URL = "https://doubleencore.atlassian.net";
    private static final String CREATE_ISSUE_PATH = "/rest/api/2/issue/";
    private static final String ATTACHMENT_PATH_FORMAT = "/rest/api/2/issue/%s/attachments";
    private static final String DEFAULT_ISSUE_TYPE = "Bug";
    private static final String HEADER_KEY_XTOKEN = "X-Atlassian-Token";
    private static final String HEADER_VALUE_XTOKEN = "nocheck";
    private static final MediaType TYPE_JSON = MediaType.parse("application/json; charset=utf-8");
    private static final int RESPONSE_CREATED = 201;

    private OkHttpClient mHttpClient = new OkHttpClient();
    private Gson mGson = new Gson();
    private Context mContext;
    private CreateIssueListener mCreateIssueListener;
    private String mAttachmentPath;
    private String mProjectKey;
    private String mUsername;
    private String mPassword;

    public JiraService(Context context, CreateIssueListener listener) {
        UploadService.NAMESPACE = BuildConfig.APPLICATION_ID;
        mContext = context;
        mCreateIssueListener = listener;
        mProjectKey = BugReportInternal.getJiraProjectKey();
        mUsername = BugReportInternal.getJiraUsername();
        mPassword = BugReportInternal.getJiraPassword();
    }

    public void createJiraIssue(Uri attachmentUri, String summary, String description) {
        if (attachmentUri != null) {
            mAttachmentPath = attachmentUri.getPath();
        }

        CreateIssue createIssue = new CreateIssue(mProjectKey, summary, description, DEFAULT_ISSUE_TYPE);

        String credential = Credentials.basic(mUsername, mPassword);
        RequestBody body = RequestBody.create(TYPE_JSON, mGson.toJson(createIssue));
        Request request = new Request.Builder()
                .url(JIRA_SERVER_URL + CREATE_ISSUE_PATH)
                .post(body)
                .addHeader("Authorization", credential)
                .build();

        mHttpClient.newCall(request).enqueue(this);
    }

    private void uploadAttachment(String issueId) {
        String url = JIRA_SERVER_URL + String.format(ATTACHMENT_PATH_FORMAT, issueId);

        try {
            MultipartUploadRequest uploadRequest = new MultipartUploadRequest(mContext, url)
                    .addFileToUpload(mAttachmentPath, "file")
                    .addHeader(HEADER_KEY_XTOKEN, HEADER_VALUE_XTOKEN)
                    .setNotificationConfig(new UploadNotificationConfig())
                    .setMaxRetries(2)
                    .setBasicAuth(mUsername, mPassword);
            uploadRequest.startUpload();
        } catch (FileNotFoundException e) {
        } catch (MalformedURLException e) {
        }
    }

    private void onIssueCreatedSuccess(CreateIssueResponse issueResponse) {
        if (mCreateIssueListener != null) {
            mCreateIssueListener.onIssueCreatedSuccess(issueResponse);
        }
    }

    private void onIssueCreatedFailed() {
        if (mCreateIssueListener != null) {
            mCreateIssueListener.onIssueCreatedFailed();
        }
    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {
        if (response.code() == RESPONSE_CREATED) {
            CreateIssueResponse issueResponse = mGson.fromJson(response.body().string(), CreateIssueResponse.class);
            if (!TextUtils.isEmpty(mAttachmentPath)) {
                uploadAttachment(issueResponse.id);
            }
            onIssueCreatedSuccess(issueResponse);
        } else {
            onIssueCreatedFailed();
        }
    }

    @Override
    public void onFailure(Call call, IOException e) {
        onIssueCreatedFailed();
    }
}
