package com.doubleencore.bugreport.internal.jira;

import com.google.gson.annotations.SerializedName;

public class CreateIssueResponse {

    public String id;
    public String key;
    @SerializedName("self")
    public String issueUrl;
}
