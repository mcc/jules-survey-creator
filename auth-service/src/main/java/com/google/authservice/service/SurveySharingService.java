package com.google.authservice.service;

import com.google.authservice.dto.SharedUserDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SurveySharingService {

    // --- Mock Data Store ---
    // This is a mock in-memory store for survey sharing information.
    // Key: surveyId, Value: List of UserIds shared with.
    private final Map<String, List<String>> surveyShares = new HashMap<>();

    // This is a mock in-memory store for user details.
    // Key: userId, Value: User details (SharedUserDTO for simplicity here)
    private final Map<String, SharedUserDTO> mockUsers = new HashMap<>();

    public SurveySharingService() {
        // Initialize with some mock users
        mockUsers.put("user1", new SharedUserDTO("user1", "Alice Wonderland", "alice@example.com"));
        mockUsers.put("user2", new SharedUserDTO("user2", "Bob The Builder", "bob@example.com"));
        mockUsers.put("user3", new SharedUserDTO("user3", "Charlie Brown", "charlie@example.com"));
        mockUsers.put("user4", new SharedUserDTO("user4", "Diana Prince", "diana@example.com"));

        // Initialize some mock shares
        List<String> survey1Shares = new ArrayList<>();
        survey1Shares.add("user2");
        survey1Shares.add("user3");
        surveyShares.put("survey123", survey1Shares);

        List<String> survey2Shares = new ArrayList<>();
        survey2Shares.add("user1");
        surveyShares.put("survey456", survey2Shares);
    }
    // --- End of Mock Data Store ---

    public List<SharedUserDTO> getSharedUsers(String surveyId) {
        // In a real application, this would involve:
        // 1. Fetching the survey entity by surveyId.
        // 2. Accessing its list of shared user IDs or user entities.
        // 3. Mapping those user entities/IDs to SharedUserDTOs.

        System.out.println("Service: Getting shared users for surveyId: " + surveyId);
        List<String> sharedUserIds = surveyShares.getOrDefault(surveyId, new ArrayList<>());

        return sharedUserIds.stream()
                .map(mockUsers::get) // Look up user details from mockUsers
                .filter(user -> user != null) // Filter out if a userId doesn't have details (consistency)
                .collect(Collectors.toList());
    }

    public void shareSurveyWithUser(String surveyId, String userId) {
        // In a real application, this would involve:
        // 1. Validating that both surveyId and userId exist.
        // 2. Fetching the survey entity.
        // 3. Fetching the user entity.
        // 4. Adding the user to the survey's list of shared users (e.g., in a join table or a list in the survey document).
        // 5. Persisting the changes.

        System.out.println("Service: Sharing surveyId: " + surveyId + " with userId: " + userId);
        if (!mockUsers.containsKey(userId)) {
            // Or throw a specific UserNotFoundException
            System.out.println("Warning: User with ID " + userId + " not found in mock users. Sharing might be incomplete.");
            // In a real app, you'd likely throw an exception here or handle it based on requirements.
        }

        surveyShares.computeIfAbsent(surveyId, k -> new ArrayList<>()).add(userId);
        // To prevent duplicates if this method can be called multiple times:
        // List<String> users = surveyShares.computeIfAbsent(surveyId, k -> new ArrayList<>());
        // if (!users.contains(userId)) {
        //     users.add(userId);
        // }
        System.out.println("Current shares for " + surveyId + ": " + surveyShares.get(surveyId));
    }

    public void unshareSurveyWithUser(String surveyId, String userId) {
        // In a real application, this would involve:
        // 1. Validating surveyId and userId.
        // 2. Fetching the survey entity.
        // 3. Removing the user from the survey's list of shared users.
        // 4. Persisting the changes.

        System.out.println("Service: Unsharing surveyId: " + surveyId + " from userId: " + userId);
        List<String> sharedUserIds = surveyShares.get(surveyId);
        if (sharedUserIds != null) {
            sharedUserIds.remove(userId);
        }
        System.out.println("Current shares for " + surveyId + ": " + surveyShares.get(surveyId));
    }

    // Helper method to find a user by username (if needed, and if User service is separate)
    // For now, this is internal to the mock if we decide to support username in ShareSurveyRequest
    public SharedUserDTO findUserByUsername(String username) {
        return mockUsers.values().stream()
            .filter(user -> user.getUsername().equalsIgnoreCase(username))
            .findFirst()
            .orElse(null);
    }
}
