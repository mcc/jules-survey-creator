import { apiClient } from '../contexts/AuthContext';

export const getUsers = () => {
  return apiClient.get('/admin/users').then(response => response.data);
};

export const getUser = (userId) => {
  return apiClient.get(`/admin/users/${userId}`).then(response => response.data);
};

export const createUser = (userData) => {
  return apiClient.post('/admin/users', userData).then(response => response.data);
};

export const editUser = (userId, userData) => {
  return apiClient.put(`/admin/users/${userId}`, userData).then(response => response.data);
};

export const resetPassword = (username) => {
  return apiClient.post(`/admin/users/${username}/reset-password`).then(response => response.data);
};

export const setUserStatus = (userId, isActive) => {
  return apiClient.put(`/admin/users/${userId}/status`, { isActive }).then(response => response.data);
};

export const activateUser = (userId) => {
  return setUserStatus(userId, true);
};

export const inactivateUser = (userId) => {
  return setUserStatus(userId, false);
};

export const getRoles = () => {
  return apiClient.get('/admin/roles').then(response => response.data);
}
// Add this function to userService.js

export const changeCurrentUserPassword = (passwordData) => {
  // passwordData is expected to be an object like:
  // { oldPassword: "currentPassword", newPassword: "newPasswordGoesHere" }
  return apiClient.post('/auth/users/change-password', passwordData)
    .then(response => response.data) // Or handle the full response if needed
    .catch(error => {
      // It's often better to let the calling component handle UI errors,
      // but re-throwing or transforming the error can be useful.
      console.error('Error changing password:', error.response?.data || error.message);
      throw error.response?.data || error; // Re-throw for the component to catch
    });
};
