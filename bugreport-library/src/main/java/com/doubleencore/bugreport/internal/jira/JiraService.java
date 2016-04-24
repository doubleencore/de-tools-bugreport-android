package com.doubleencore.bugreport.internal.jira;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.doubleencore.bugreport.internal.BugReportInternal;
import com.doubleencore.bugreport.lib.BuildConfig;
import com.doubleencore.bugreport.lib.R;
import com.google.gson.Gson;

import net.gotev.uploadservice.MultipartUploadRequest;
import net.gotev.uploadservice.UploadNotificationConfig;
import net.gotev.uploadservice.UploadService;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import static com.doubleencore.bugreport.internal.PackageUtils.getAppName;

public class JiraService extends IntentService {

    private static final String TAG = JiraService.class.getSimpleName();

    private static final String JIRA_SERVER_URL = "https://doubleencore.atlassian.net";
    private static final String CREATE_ISSUE_PATH = "/rest/api/2/issue/";
    private static final String ATTACHMENT_PATH_FORMAT = "/rest/api/2/issue/%s/attachments";
    private static final String DEFAULT_ISSUE_TYPE = "Bug";
    private static final String HEADER_KEY_XTOKEN = "X-Atlassian-Token";
    private static final String HEADER_VALUE_XTOKEN = "nocheck";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String TYPE_JSON = "application/json; charset=utf-8";

    private static final String KEY_SUMMARY = "key_summary";
    private static final String KEY_DESCRIPTION = "key_description";

    private Gson mGson = new Gson();
    private Context mContext;
    private String mAttachmentPath;

    private String mProjectKey = BugReportInternal.getJiraProjectKey();
    private String mUsername = BugReportInternal.getJiraUsername();
    private String mPassword = BugReportInternal.getJiraPassword();


    public JiraService() {
        super(TAG);
        UploadService.NAMESPACE = BuildConfig.APPLICATION_ID;
    }

    public static void start(@NonNull Context context, @NonNull Uri attachmentUri,
                             @NonNull String summary, @NonNull String description) {
        Intent intent = new Intent(context, JiraService.class);
        intent.putExtra(KEY_SUMMARY, summary);
        intent.putExtra(KEY_DESCRIPTION, description);
        intent.setData(attachmentUri);

        context.startService(intent);
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
            errorNotification(mContext.getString(R.string.notification_error, key));
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mAttachmentPath = intent.getData().getPath();
        mContext = getApplicationContext();

        CreateIssue createIssue = new CreateIssue(mProjectKey, intent.getStringExtra(KEY_SUMMARY),
                intent.getStringExtra(KEY_DESCRIPTION), DEFAULT_ISSUE_TYPE);
        CreateIssueResponse response = createIssue(createIssue);
        if (response != null) {
            uploadAttachment(response.id, response.key);
        } else {
            errorNotification(mContext.getString(R.string.jira_error, mProjectKey));
        }
    }

    private void errorNotification(String error) {

        Notification notification = new NotificationCompat.Builder(mContext)
                .setContentTitle(mContext.getString(R.string.notification_title, getAppName(mContext)))
                .setContentText(error)
                .setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), android.R.drawable.ic_menu_share))
                .setSmallIcon(android.R.drawable.ic_menu_share)
                .setLocalOnly(true)
                .build();

        notification.flags = Notification.FLAG_AUTO_CANCEL;

        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(Integer.MAX_VALUE, notification);
    }

    @Nullable
    private CreateIssueResponse createIssue(@NonNull CreateIssue issue) {
        URL url;
        StringBuilder response = new StringBuilder();
        try {
            url = new URL(JIRA_SERVER_URL + CREATE_ISSUE_PATH);

            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            conn.setRequestProperty(HEADER_AUTHORIZATION, Credentials.basic(mUsername, mPassword));
            conn.setRequestProperty(HEADER_CONTENT_TYPE, TYPE_JSON);

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(mGson.toJson(issue));
            writer.flush();
            writer.close();
            os.close();

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpsURLConnection.HTTP_CREATED) {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }

            return mGson.fromJson(response.toString(), CreateIssueResponse.class);

        } catch (Exception e) {
            Log.e(TAG, "Failed to create ticket: " + e);
            return null;
        }
    }
}
