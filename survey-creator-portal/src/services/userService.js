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

// Updated to match backend AdminController: POST /api/admin/users/reset-password
// which expects { username: "user", newPassword: "newPassword" } in the body.
// However, this frontend function was likely intended for an admin to trigger a reset,
// not necessarily to set a new password directly.
// The backend AdminController's resetPassword(AdminResetPasswordRequest)
// implies the admin provides the new password.
// If the intent was just to "trigger a reset" without setting a new password,
// that would be different from what backend AdminController /users/reset-password does.
// For now, assuming the admin *does* provide the new password as per backend.
export const resetPassword = (username, newPassword) => {
  return apiClient.post('/admin/users/reset-password', { username, newPassword })
    .then(response => response.data);
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
  return apiClient.get('/roles').then(response => response.data);
}
// Add this function to userService.js

export const changeCurrentUserPassword = (passwordData) => {
  // passwordData is expected to be an object like:
  // { oldPassword: "currentPassword", newPassword: "newPasswordGoesHere" }
  return apiClient.post('/auth/users/change-password', passwordData)
    .then(response => response.data) // Or handle the full response if needed
    .catch(error => {
      // The error object is already transformed by the apiClient interceptor.
      // Optional: console.error('Error changing password in userService:', error);
      throw error;
    });
};

export const searchUsers = (searchQuery) => {
  return apiClient.get(`/users/search?q=${encodeURIComponent(searchQuery)}`)
    .then(response => response.data)
    .catch(error => {
      console.error('Error searching users:', error);
      throw error;
    });
};

export const requestPasswordReset = (email) => {
  return apiClient.post('/auth/forgot-password', { email })
    .then(response => response.data)
    .catch(error => {
      // The error object is already transformed by the apiClient interceptor.
      // Optional: console.error('Error requesting password reset in userService:', error);
      throw error;
    });
};

export const confirmPasswordReset = (token, newPassword) => {
  return apiClient.post('/auth/reset-password', { token, newPassword })
    .then(response => response.data)
    .catch(error => {
      // The error object is already transformed by the apiClient interceptor.
      // Optional: console.error('Error confirming password reset in userService:', error);
      throw error;
    });
};
