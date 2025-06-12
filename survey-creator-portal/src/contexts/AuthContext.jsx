import React, { createContext, useState, useContext, useEffect } from 'react';
import axios from 'axios';
import { jwtDecode } from 'jwt-decode'; // Corrected import

export const AuthContext = createContext(); // Export AuthContext

// Create an Axios instance
const apiClient = axios.create({
  baseURL: '/api/auth', // Assuming your Spring Boot app is served from the same domain
});

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true); // Add loading state

  useEffect(() => {
    // Check for existing token on initial load
    const token = localStorage.getItem('jwtToken');
    if (token) {
      try {
        const decodedToken = jwtDecode(token);
        // Check if token is expired
        if (decodedToken.exp * 1000 < Date.now()) { // Expired
          localStorage.removeItem('jwtToken');
          localStorage.removeItem('refreshToken');
          setUser(null); // Ensure user state is cleared
        } else {
          setUser({ email: decodedToken.sub, ...decodedToken }); // Assuming 'sub' is email
          apiClient.defaults.headers.common['Authorization'] = `Bearer ${token}`;
        }
      } catch (error) {
        console.error('Error decoding token on initial load:', error);
        localStorage.removeItem('jwtToken');
        localStorage.removeItem('refreshToken');
        setUser(null); // Ensure user state is cleared
      }
    }
    setLoading(false);
  }, []);

  const login = async (email, password) => {
    try {
      const response = await apiClient.post('/login', { email, password });
      const { token, refreshToken } = response.data;

      localStorage.setItem('jwtToken', token);
      localStorage.setItem('refreshToken', refreshToken);

      const decodedToken = jwtDecode(token);
      setUser({ email: decodedToken.sub, ...decodedToken }); // Assuming 'sub' is email
      apiClient.defaults.headers.common['Authorization'] = `Bearer ${token}`;
      console.log('User logged in');
    } catch (error) {
      console.error('Login failed:', error);
      // Handle login errors (e.g., show error message to user)
      throw error; // Re-throw error to be caught by the calling component
    }
  };

  const logout = async () => {
    try {
      // Optional: Call logout endpoint on the backend
      await apiClient.post('/logout', { refreshToken: localStorage.getItem('refreshToken') });
      console.log('Logout successful on backend');
    } catch (error) {
      console.error('Backend logout failed:', error);
      // Still proceed with client-side logout
    } finally {
      localStorage.removeItem('jwtToken');
      localStorage.removeItem('refreshToken');
      setUser(null);
      delete apiClient.defaults.headers.common['Authorization'];
      console.log('User logged out');
    }
  };

  const refreshToken = async () => {
    const currentRefreshToken = localStorage.getItem('refreshToken');
    if (!currentRefreshToken) {
      console.log('No refresh token available.');
      await logout(); // Or handle appropriately, e.g., redirect to login
      return Promise.reject(new Error('No refresh token'));
    }

    try {
      const response = await apiClient.post('/refresh', { refreshToken: currentRefreshToken });
      const { token: newToken, refreshToken: newRefreshToken } = response.data; // Assuming backend might also send a new refresh token

      localStorage.setItem('jwtToken', newToken);
      if (newRefreshToken) { // If backend provides a new refresh token, store it
        localStorage.setItem('refreshToken', newRefreshToken);
      }

      const decodedToken = jwtDecode(newToken);
      setUser({ email: decodedToken.sub, ...decodedToken });
      apiClient.defaults.headers.common['Authorization'] = `Bearer ${newToken}`;
      console.log('Token refreshed');
      return newToken;
    } catch (error) {
      console.error('Token refresh failed:', error);
      await logout(); // Logout user if refresh fails
      return Promise.reject(error);
    }
  };

  // Axios interceptor setup
  useEffect(() => {
    const interceptor = apiClient.interceptors.response.use(
      (response) => response,
      async (error) => {
        const originalRequest = error.config;
        // Prevent retry loops for login, token refresh endpoint itself, or if already retried
        if (originalRequest.url === '/login' || originalRequest.url === '/refresh' || originalRequest._retry) {
          return Promise.reject(error);
        }

        if (error.response?.status === 401) {
          originalRequest._retry = true;
          try {
            console.log('Attempting to refresh token due to 401 on other request...');
            const newToken = await refreshToken();
            // Update the header for the original request
            apiClient.defaults.headers.common['Authorization'] = `Bearer ${newToken}`; // Also update apiClient instance for future
            originalRequest.headers['Authorization'] = `Bearer ${newToken}`;
            return apiClient(originalRequest); // Retry the original request
          } catch (refreshError) {
            console.error('Failed to refresh token after 401 (interceptor):', refreshError);
            // Logout is handled by refreshToken if it fails, so just propagate the error
            return Promise.reject(refreshError);
          }
        }
        return Promise.reject(error);
      }
    );

    // Cleanup interceptor on component unmount
    return () => {
      apiClient.interceptors.response.eject(interceptor);
    };
  }, [refreshToken]); // Add refreshToken as a dependency


  if (loading) {
    return <div>Loading...</div>; // Or a proper loading spinner
  }

  return (
    <AuthContext.Provider value={{ user, login, logout, refreshToken }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  return useContext(AuthContext);
};
