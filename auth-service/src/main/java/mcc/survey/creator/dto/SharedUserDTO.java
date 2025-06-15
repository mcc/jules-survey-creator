package com.google.authservice.dto;

public class SharedUserDTO {
    private String userId;
    private String username;
    // Assuming email is also useful to display
    private String email;

    public SharedUserDTO() {
    }

    public SharedUserDTO(String userId, String username, String email) {
        this.userId = userId;
        this.username = username;
        this.email = email;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
