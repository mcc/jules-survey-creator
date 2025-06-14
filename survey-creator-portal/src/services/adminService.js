import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || '/api'; // Fallback to /api if env var not set

// Utility to get the auth token - replace with your actual auth token retrieval logic
const getAuthToken = () => {
    // Example: localStorage.getItem('authToken');
    // For now, returning a placeholder if you have a global way to get it e.g. from a context
    // Or ensure your axios instance/interceptor handles this
    return localStorage.getItem('token');
};

// Axios instance with default settings
const apiClient = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Interceptor to add the auth token to requests
apiClient.interceptors.request.use(config => {
    const token = getAuthToken();
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
}, error => {
    return Promise.reject(error);
});

// --- Service Management ---
export const createService = (serviceData) => {
    return apiClient.post('/admin/services', serviceData);
};

export const getAllServices = () => {
    return apiClient.get('/admin/services');
};

export const getServiceById = (serviceId) => {
    return apiClient.get(`/admin/services/${serviceId}`);
};

export const updateService = (serviceId, serviceData) => {
    return apiClient.put(`/admin/services/${serviceId}`, serviceData);
};

export const deleteService = (serviceId) => {
    return apiClient.delete(`/admin/services/${serviceId}`);
};

// --- Team Management ---
export const createTeam = (teamData) => {
    return apiClient.post('/admin/teams', teamData);
};

export const getAllTeams = () => {
    return apiClient.get('/admin/teams');
};

export const getTeamsByService = (serviceId) => {
    return apiClient.get(`/admin/services/${serviceId}/teams`);
};

export const getTeamById = (teamId) => {
    return apiClient.get(`/admin/teams/${teamId}`);
};

export const updateTeam = (teamId, teamData) => {
    return apiClient.put(`/admin/teams/${teamId}`, teamData);
};

export const deleteTeam = (teamId) => {
    return apiClient.delete(`/admin/teams/${teamId}`);
};

export const assignUsersToTeam = (teamId, userIds) => {
    // The backend expects { userIds: [id1, id2] }
    return apiClient.post(`/admin/teams/${teamId}/users`, { userIds });
};

export const removeUserFromTeam = (teamId, userId) => {
    return apiClient.delete(`/admin/teams/${teamId}/users/${userId}`);
};

export const getUsersInTeam = (teamId) => {
    return apiClient.get(`/admin/teams/${teamId}/users`);
};

const adminService = {
    createService,
    getAllServices,
    getServiceById,
    updateService,
    deleteService,
    createTeam,
    getAllTeams,
    getTeamsByService,
    getTeamById,
    updateTeam,
    deleteTeam,
    assignUsersToTeam,
    removeUserFromTeam,
    getUsersInTeam,
};

export default adminService;
