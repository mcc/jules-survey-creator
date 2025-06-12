package mcc.survey.creator.dto;

public class UpdateUserStatusRequest {
    private boolean isActive;

    public boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(boolean active) {
        isActive = active;
    }
}
