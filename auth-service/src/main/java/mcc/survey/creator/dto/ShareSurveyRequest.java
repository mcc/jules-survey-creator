package com.google.authservice.dto;

public class ShareSurveyRequest {
    // Using userId directly for sharing for now.
    // Username based sharing can be a feature to add if User service can resolve username to userId.
    private String userId;

    public ShareSurveyRequest() {
    }

    public ShareSurveyRequest(String userId) {
        this.userId = userId;
    }

    // Getter and Setter
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
