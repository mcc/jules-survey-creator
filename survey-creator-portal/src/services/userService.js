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
