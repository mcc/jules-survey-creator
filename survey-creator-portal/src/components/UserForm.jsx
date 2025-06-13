import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { createUser, editUser, getUser, getRoles, setUserStatus } from '../services/userService'; // Adjust path
import {
  TextField, Button, Box, Typography, CircularProgress, Alert, Paper,
  FormGroup, FormControlLabel, Checkbox, Select, MenuItem, InputLabel, FormControl, FormHelperText
} from '@mui/material';

const UserForm = () => {
  const { userId } = useParams(); // Get userId from URL if editing
  const navigate = useNavigate();
  const isEditMode = Boolean(userId);

  const [formData, setFormData] = useState({
    username: '',
    email: '',
    roles: [],
    teamIds: [], // For selected team IDs
    isActive: true,
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [rolesError, setRolesError] = useState(null);
  const [teamsError, setTeamsError] = useState(null); // Specific error for fetching teams
  const [successMessage, setSuccessMessage] = useState(null);
  const [availableRoles, setAvailableRoles] = useState([]);
  const [availableTeams, setAvailableTeams] = useState([]); // For storing all available teams
  const [initialIsActive, setInitialIsActive] = useState(null);

  useEffect(() => {
    let isMounted = true;
    setLoading(true);
    setRolesError(null);
    setTeamsError(null);

    const fetchInitialData = async () => {
      try {
        // Fetch Roles
        const rolesData = await getRoles();
        if (isMounted) {
          setAvailableRoles(rolesData.map(r => (typeof r === 'string' ? r : r.name || r.role)).filter(Boolean));
        }

        // Fetch Teams (imported from adminService)
        // Assuming adminService is available or imported, e.g., import adminService from './adminService';
        // For now, let's assume a getTeams function similar to getRoles exists or is part of userService/adminService
        // This might need adjustment if adminService is not directly usable here or if getTeams is elsewhere.
        // Let's assume it's in userService for now or you'll adapt this.
        // For the purpose of this diff, I'll mock a fetchTeams function call.
        // You would replace this with: const teamsData = await adminService.getAllTeams();
        // const teamsData = await getTeams(); // Placeholder, replace with actual call
        const adminService = (await import('../services/adminService')).default; // Dynamically import
        const teamsResponse = await adminService.getAllTeams();
        if (isMounted) {
          setAvailableTeams(teamsResponse.data || []);
        }

        // Fetch User if in edit mode
        if (isEditMode && userId) {
          const userData = await getUser(userId);
          if (isMounted) {
            setFormData({
              username: userData.username,
              email: userData.email,
              roles: userData.roles?.map(role => typeof role === 'string' ? role : (role.name || role.role)).filter(Boolean) || [],
              teamIds: userData.teams?.map(team => team.id) || [], // Extract team IDs
              isActive: userData.isActive,
            });
            setInitialIsActive(userData.isActive);
          }
        }
        if (isMounted) setError(null); // Clear general error if all successful
      } catch (err) {
        console.error("Failed to fetch initial data:", err);
        if (isMounted) {
          // Distinguish between role, team, or user fetch errors if necessary
          if (err.message.includes("roles")) setRolesError(err.message);
          else if (err.message.includes("teams")) setTeamsError(err.message); // Assuming error message indicates source
          else setError(err.message || 'Failed to load data.');
        }
      } finally {
        if (isMounted) setLoading(false);
      }
    };

    fetchInitialData();

    return () => {
      isMounted = false; // Cleanup function to set isMounted to false when component unmounts
    };
  }, [userId, isEditMode]);


  const handleChange = (event) => {
    const { name, value, type, checked } = event.target;
    setFormData(prev => ({
      ...prev,
      // For 'roles', the Select component with 'multiple' prop directly provides an array for 'value'
      [name]: type === 'checkbox' ? checked : value,
    }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError(null);
    setRolesError(null); // Clear roles error on submit attempt
    setSuccessMessage(null);

    const payload = {
      username: formData.username,
      email: formData.email,
      roles: formData.roles,
      teamIds: formData.teamIds, // Include selected team IDs
    };

    // In create mode, password should be handled by the backend or a separate field if needed
    // For edit mode, isActive is part of the main payload if changed, or handled by setUserStatus
    // The existing logic for isActive seems to handle it separately via setUserStatus if it changed.
    // For simplicity with backend expectations (EditUserRequest can take isActive), let's include it:
    if (isEditMode) {
        payload.isActive = formData.isActive;
    }
    // If CreateUserRequest expects isActive, add it here too. Backend defaults to true.

    try {
      let mainOperationSuccessful = false; // To track if the primary create/edit was successful
      if (isEditMode) {
        await editUser(userId, payload);
        mainOperationSuccessful = true; // Mark main edit as successful

        // Now, check if isActive status changed and update if necessary
        if (initialIsActive !== null && formData.isActive !== initialIsActive) {
          await setUserStatus(userId, formData.isActive);
          setInitialIsActive(formData.isActive); // Update initialIsActive to prevent re-submission
        }
        setSuccessMessage('User updated successfully!');
      } else {
        await createUser(payload);
        mainOperationSuccessful = true; // Mark create as successful
        setSuccessMessage('User created successfully!');
      }

      if (mainOperationSuccessful) {
        // Optionally navigate after a short delay or on button click
        setTimeout(() => {
          navigate('/admin/users'); // Navigate back to user list
        }, 2000);
      }
    } catch (err) {
      // Check if the error is from the primary operation or setUserStatus
      // This basic error message might need refinement if more granular feedback is required
      setError(err.message || (isEditMode ? 'Failed to update user details or status.' : 'Failed to create user.'));
    } finally {
      setLoading(false);
    }
  };

  // Show loading indicator only when fetching user data for edit mode initially
  if (loading && isEditMode && !formData.username && !error) {
    return <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}><CircularProgress /></Box>;
  }

  return (
    <Paper sx={{ margin: 2, padding: 3 }}>
      <Typography variant="h6" component="h1" gutterBottom>
        {isEditMode ? 'Edit User' : 'Create New User'}
      </Typography>
      <Box component="form" onSubmit={handleSubmit} sx={{ mt: 1 }}>
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        {successMessage && <Alert severity="success" sx={{ mb: 2 }}>{successMessage}</Alert>}

        <TextField
          margin="normal"
          required
          fullWidth
          id="username"
          label="Username"
          name="username"
          autoComplete="username"
          value={formData.username}
          onChange={handleChange}
          disabled={isEditMode || loading}
        />
        <TextField
          margin="normal"
          required
          fullWidth
          id="email"
          label="Email Address"
          name="email"
          autoComplete="email"
          type="email"
          value={formData.email}
          onChange={handleChange}
          disabled={loading}
        />
        {/* Roles Dropdown */}
        <FormControl fullWidth margin="normal" error={Boolean(rolesError)}>
          <InputLabel id="roles-label">Roles</InputLabel>
          <Select
            labelId="roles-label"
            id="roles-select"
            name="roles"
            multiple
            value={formData.roles}
            onChange={handleChange}
            label="Roles"
            renderValue={(selected) => selected.join(', ')}
            disabled={loading || availableRoles.length === 0 || rolesError}
            MenuProps={{ PaperProps: { style: { maxHeight: 224, width: 250 } } }}
          >
            {availableRoles.map((role) => (
              <MenuItem key={role} value={role}>
                {role}
              </MenuItem>
            ))}
          </Select>
          {rolesError && <FormHelperText error>{rolesError}</FormHelperText>}
          {!rolesError && availableRoles.length === 0 && !loading && !rolesError && (
            <FormHelperText>Loading roles or no roles available.</FormHelperText>
          )}
          {!rolesError && <FormHelperText>Select one or more roles. Default is ROLE_USER if empty on creation.</FormHelperText>}
        </FormControl>

        {/* Teams Multi-Select Dropdown */}
        <FormControl fullWidth margin="normal" error={Boolean(teamsError)}>
          <InputLabel id="teams-label">Teams</InputLabel>
          <Select
            labelId="teams-label"
            id="teams-select"
            name="teamIds" // Ensure this matches the state property for team IDs
            multiple
            value={formData.teamIds} // Use formData.teamIds
            onChange={handleChange}
            label="Teams"
            renderValue={(selectedTeamIds) =>
                selectedTeamIds.map(id => availableTeams.find(team => team.id === id)?.name).filter(Boolean).join(', ')
            }
            disabled={loading || availableTeams.length === 0 || teamsError}
            MenuProps={{ PaperProps: { style: { maxHeight: 224, width: 250 } } }}
          >
            {availableTeams.map((team) => (
              <MenuItem key={team.id} value={team.id}>
                {team.name} (Service: {team.serviceName || 'N/A'})
              </MenuItem>
            ))}
          </Select>
          {teamsError && <FormHelperText error>{teamsError}</FormHelperText>}
          {!teamsError && availableTeams.length === 0 && !loading && !teamsError && (
            <FormHelperText>Loading teams or no teams available.</FormHelperText>
          )}
          {!teamsError && <FormHelperText>Assign user to one or more teams.</FormHelperText>}
        </FormControl>

        {isEditMode && (
          <FormGroup sx={{ mt: 1 }}>
            <FormControlLabel
              control={<Checkbox checked={formData.isActive} onChange={handleChange} name="isActive" />}
              label="Active"
              disabled={loading}
            />
          </FormGroup>
        )}
        <Button
          type="submit"
          fullWidth
          variant="contained"
          sx={{ mt: 3, mb: 2 }}
          disabled={loading}
        >
          {loading ? <CircularProgress size={24} /> : (isEditMode ? 'Update User' : 'Create User')}
        </Button>
        <Button
            fullWidth
            variant="outlined"
            onClick={() => navigate('/admin/users')}
            disabled={loading}
        >
            Cancel
        </Button>
      </Box>
    </Paper>
  );
};

export default UserForm;
