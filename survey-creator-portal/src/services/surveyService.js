import { apiClient } from '../contexts/AuthContext'; // Corrected import path if necessary based on actual structure

// The userId parameter is kept for potential future use or if API requires it,
// even if the backend currently gets the user from the security context.
export const getSurveysByUser = async (userId) => {
  try {
    // Assuming the backend endpoint /api/surveys is secured and returns surveys for the authenticated user.
    // If the backend specifically needs the userId in the query params: apiClient.get(`/api/surveys?userId=${userId}`);
    const response = await apiClient.get('/surveys');
    return response.data;
  } catch (error) {
    // Optional: console.error('Error fetching surveys by user:', error);
    throw error;
  }
};

// Fetches survey details, which should include shared users
export const fetchSharedUsers = async (surveyId) => {
  try {
    const response = await apiClient.get(`/surveys/${surveyId}`);
    // Assuming response.data is the survey object and it contains a sharedWithUsers array
    return response.data;
  } catch (error) {
    // Optional: console.error(`Error fetching shared users for survey ID ${surveyId}:`, error);
    throw error;
  }
};

export const shareSurvey = async (surveyId, username) => {
  try {
    // Step 1: Get User ID by Username
    const userResponse = await apiClient.get(`/auth/users/by-username/${username.trim()}`);
    const userIdToShare = userResponse.data.id;

    if (!userIdToShare) {
      throw new Error(`User ID not found for username: ${username.trim()}`);
    }

    // Step 2: Share the Survey
    const shareResponse = await apiClient.post(`/surveys/${surveyId}/share/${userIdToShare}`, {});
    return shareResponse.data;
  } catch (error) {
    // Optional: console.error(`Error sharing survey ID ${surveyId} with username ${username}:`, error);
    throw error;
  }
};

export const unshareSurvey = async (surveyId, userIdToUnshare) => {
  try {
    const response = await apiClient.delete(`/surveys/${surveyId}/unshare/${userIdToUnshare}`);
    // For DELETE requests, a 204 No Content is common, in which case response.data might be empty.
    // The calling component should be prepared for this.
    return response.data;
  } catch (error) {
    // Optional: console.error(`Error unsharing survey ID ${surveyId} from user ID ${userIdToUnshare}:`, error);
    throw error;
  }
};

export const getSurvey = async (surveyId) => {
  try {
    const response = await apiClient.get(`/surveys/${surveyId}`);
    return response.data;
  } catch (error) {
    // Optional: console.error(`Error fetching survey with ID ${surveyId}:`, error);
    throw error;
  }
};

export const createSurvey = async (surveyData) => {
  try {
    const response = await apiClient.post('/surveys/createSurvey', surveyData);
    return response.data;
  } catch (error) {
    // Optional: console.error('Error creating survey:', error);
    throw error;
  }
};

export const updateSurvey = async (surveyId, surveyData) => {
  try {
    const response = await apiClient.put(`/surveys/${surveyId}`, surveyData);
    return response.data;
  } catch (error) {
    // Optional: console.error(`Error updating survey with ID ${surveyId}:`, error);
    throw error;
  }
};

export const deleteSurvey = async (surveyId) => {
  try {
    const response = await apiClient.delete(`/surveys/${surveyId}`);
    return response.data; // Or handle 204 No Content appropriately (data might be undefined)
  } catch (error) {
    // Optional: console.error(`Error deleting survey with ID ${surveyId}:`, error);
    throw error;
  }
};

export const publishSurvey = async (surveyId) => {
  try {
    const survey = await getSurvey(surveyId);
    const updatedSurveyData = { ...survey, status: 'published' };
    // The backend might re-validate or only take specific fields,
    // ensure survey object structure matches what updateSurvey endpoint expects.
    return await updateSurvey(surveyId, updatedSurveyData);
  } catch (error) {
    // Optional: console.error(`Error publishing survey with ID ${surveyId}:`, error);
    throw error;
  }
};

export const unpublishSurvey = async (surveyId) => {
  try {
    const survey = await getSurvey(surveyId);
    const updatedSurveyData = { ...survey, status: 'draft' };
    return await updateSurvey(surveyId, updatedSurveyData);
  } catch (error) {
    // Optional: console.error(`Error unpublishing survey with ID ${surveyId}:`, error);
    throw error;
  }
};
