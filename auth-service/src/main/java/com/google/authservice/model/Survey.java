package com.google.authservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Table(name = "surveys")
public class Survey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title cannot be blank")
    private String title;

    @Lob
    private String description;

    @Lob
    private String questionsJson; // Stores questions as a JSON string

    private String status; // e.g., "draft", "published"

    @NotBlank(message = "User ID cannot be blank")
    private String userId; // Links to the user who owns the survey

    @CreationTimestamp
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

    // Constructors
    public Survey() {
    }

    public Survey(String title, String description, String questionsJson, String status, String userId) {
        this.title = title;
        this.description = description;
        this.questionsJson = questionsJson;
        this.status = status;
        this.userId = userId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getQuestionsJson() {
        return questionsJson;
    }

    public void setQuestionsJson(String questionsJson) {
        this.questionsJson = questionsJson;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Survey survey = (Survey) o;
        return Objects.equals(id, survey.id) &&
               Objects.equals(title, survey.title) &&
               Objects.equals(userId, survey.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, userId);
    }

    @Override
    public String toString() {
        return "Survey{" +
               "id=" + id +
               ", title='" + title + '\'' +
               ", description='" + (description != null ? description.substring(0, Math.min(description.length(), 50)) : "null") + "..." + '\'' +
               ", questionsJson='" + (questionsJson != null ? questionsJson.substring(0, Math.min(questionsJson.length(), 50)) : "null") + "..." + '\'' +
               ", status='" + status + '\'' +
               ", userId='" + userId + '\'' +
               ", createdAt=" + createdAt +
               ", updatedAt=" + updatedAt +
               '}';
    }
}
