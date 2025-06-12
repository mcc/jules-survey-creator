package mcc.survey.creator.dto;

public class ResetPasswordRequest {
    private String username; // Or userId, as per plan discussion

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
