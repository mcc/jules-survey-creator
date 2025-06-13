package mcc.survey.creator.dto;

import jakarta.validation.constraints.NotBlank;

public class AdminResetPasswordRequest {

    @NotBlank(message = "Username cannot be blank")
    private String username;

    @NotBlank(message = "New password cannot be blank")
    private String newPassword;

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
