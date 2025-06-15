package mcc.survey.creator.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ErrorResponseDto {

    private int status;
    private String message;
    private String timestamp;
    private String path;

    @JsonInclude(JsonInclude.Include.NON_NULL) // Only include if not null
    private String debugMessage;

    // Private constructor to force usage of builder or factory methods if needed,
    // but for DTOs, public constructors are often fine.
    // For simplicity, using direct setters or a constructor.

    public ErrorResponseDto(int status, String message, String path) {
        this.status = status;
        this.message = message;
        this.path = path;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
    }

    public ErrorResponseDto(int status, String message, String path, String debugMessage) {
        this(status, message, path);
        this.debugMessage = debugMessage;
    }

    // Getters
    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getPath() {
        return path;
    }

    public String getDebugMessage() {
        return debugMessage;
    }

    // Setters (optional, DTOs can be immutable)
    public void setStatus(int status) {
        this.status = status;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setDebugMessage(String debugMessage) {
        this.debugMessage = debugMessage;
    }
}
