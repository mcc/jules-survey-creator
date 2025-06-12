// Helper function to get the auth token
const getToken = () => {
  const auth = localStorage.getItem('auth');
  if (auth) {
    try {
      const parsedAuth = JSON.parse(auth);
      return parsedAuth.token;
    } catch (e) {
      console.error('Error parsing auth token from localStorage', e);
      return null;
    }
  }
  return null;
};

const API_BASE_URL = '/api'; // Vite proxy will handle this

// Generic request function
const request = async (endpoint, options = {}) => {
  const token = getToken();
  const headers = {
    'Content-Type': 'application/json',
    ...options.headers,
  };
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    const errorData = await response.text();
    console.error('API Error:', response.status, errorData);
    // Try to parse errorData if it's JSON, otherwise use the raw text
    let parsedError;
    try {
        parsedError = JSON.parse(errorData);
    } catch (e) {
        parsedError = errorData;
    }
    throw { status: response.status, message: parsedError?.message || parsedError || `HTTP error! status: ${response.status}` };
  }

  if (response.status === 204) { // No Content
    return null;
  }
  return response.json();
};

export const getUsers = () => {
  return request('/admin/users');
};

export const getUser = (userId) => {
  return request(`/admin/users/${userId}`);
};

export const createUser = (userData) => {
  return request('/admin/users', {
    method: 'POST',
    body: JSON.stringify(userData),
  });
};

export const editUser = (userId, userData) => {
  return request(`/admin/users/${userId}`, {
    method: 'PUT',
    body: JSON.stringify(userData),
  });
};

export const resetPassword = (username) => {
  return request(`/admin/users/${username}/reset-password`, {
    method: 'POST',
  });
};

export const setUserStatus = (userId, isActive) => {
  return request(`/admin/users/${userId}/status`, {
    method: 'PUT',
    body: JSON.stringify({ isActive }),
  });
};

export const activateUser = (userId) => {
  return setUserStatus(userId, true);
};

export const inactivateUser = (userId) => {
  return setUserStatus(userId, false);
};
