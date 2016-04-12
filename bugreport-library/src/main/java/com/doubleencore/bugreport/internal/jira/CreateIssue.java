package com.doubleencore.bugreport.internal.jira;

import com.google.gson.annotations.SerializedName;

public class CreateIssue {

    public Fields fields;

    public CreateIssue(String projectKey, String summary, String description, String issueTypeName) {
        fields = new Fields(projectKey, summary, description, issueTypeName);
    }

    public static class Fields {
        public Project project;
        public String summary;
        public String description;
        @SerializedName("issuetype")
        public IssueType issueType;

        public Fields(String projectKey, String summary, String description, String issueTypeName) {
            project = new Project(projectKey);
            issueType = new IssueType(issueTypeName);
            this.summary = summary;
            this.description = description;
        }
    }

    public static class Project {
        public String key;
        public Project(String key) {
            this.key = key;
        }
    }

    public static class IssueType {
        public String name;
        public IssueType(String name) {
            this.name = name;
        }
    }
}
