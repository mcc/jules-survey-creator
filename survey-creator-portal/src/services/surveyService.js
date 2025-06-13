import { apiClient } from '../contexts/AuthContext'; // Corrected import path if necessary based on actual structure

// The userId parameter is kept for potential future use or if API requires it,
// even if the backend currently gets the user from the security context.
export const getSurveysByUser = async (userId) => {
  try {
    // Assuming the backend endpoint /api/surveys is secured and returns surveys for the authenticated user.
    // If the backend specifically needs the userId in the query params: apiClient.get(`/api/surveys?userId=${userId}`);
    const response = await apiClient.get('/api/surveys');
    return response.data;
  } catch (error) {
    console.error('Error fetching surveys by user:', error);
    throw error;
  }
};

export const getSurvey = async (surveyId) => {
  try {
    const response = await apiClient.get(`/api/surveys/${surveyId}`);
    return response.data;
  } catch (error) {
    console.error(`Error fetching survey with ID ${surveyId}:`, error);
    throw error;
  }
};

export const createSurvey = async (surveyData) => {
  try {
    const response = await apiClient.post('/api/surveys', surveyData);
    return response.data;
  } catch (error) {
    console.error('Error creating survey:', error);
    throw error;
  }
};

export const updateSurvey = async (surveyId, surveyData) => {
  try {
    const response = await apiClient.put(`/api/surveys/${surveyId}`, surveyData);
    return response.data;
  } catch (error) {
    console.error(`Error updating survey with ID ${surveyId}:`, error);
    throw error;
  }
};

export const deleteSurvey = async (surveyId) => {
  try {
    const response = await apiClient.delete(`/api/surveys/${surveyId}`);
    return response.data; // Or handle 204 No Content appropriately (data might be undefined)
  } catch (error) {
    console.error(`Error deleting survey with ID ${surveyId}:`, error);
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
    console.error(`Error publishing survey with ID ${surveyId}:`, error);
    throw error;
  }
};

export const unpublishSurvey = async (surveyId) => {
  try {
    const survey = await getSurvey(surveyId);
    const updatedSurveyData = { ...survey, status: 'draft' };
    return await updateSurvey(surveyId, updatedSurveyData);
  } catch (error) {
    console.error(`Error unpublishing survey with ID ${surveyId}:`, error);
    throw error;
  }
};
