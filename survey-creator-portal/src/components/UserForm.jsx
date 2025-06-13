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
    roles: [], // Initialized as an empty array for multi-select
    isActive: true,
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null); // Generic error for the form
  const [rolesError, setRolesError] = useState(null); // Specific error for fetching roles
  const [successMessage, setSuccessMessage] = useState(null);
  const [availableRoles, setAvailableRoles] = useState([]);
  const [initialIsActive, setInitialIsActive] = useState(null);

  useEffect(() => {
    let isMounted = true; // To prevent state updates on unmounted component
    setLoading(true);
    setRolesError(null); // Reset roles error on each attempt

    const fetchRoles = async () => {
      try {
        const rolesData = await getRoles();
        if (isMounted) {
          // Assuming rolesData is an array of strings or objects with a 'name' or 'role' property
          setAvailableRoles(rolesData.map(r => (typeof r === 'string' ? r : r.name || r.role)).filter(Boolean));
        }
      } catch (err) {
        console.error("Failed to fetch roles:", err);
        if (isMounted) {
          setRolesError(err.message || 'Failed to load roles.');
        }
      }
    };

    const fetchUser = async () => {
      if (!userId) { // Not in edit mode
        if(isMounted) setLoading(false); // Stop loading if only roles were to be fetched (create mode)
        return;
      }
      try {
        const userData = await getUser(userId);
        if (isMounted) {
          setFormData({
            username: userData.username,
            email: userData.email,
            // Ensure roles is an array of strings
            roles: userData.roles.map(role => typeof role === 'string' ? role : (role.name || role.role)).filter(Boolean),
            isActive: userData.isActive,
          });
          if (isMounted) { // Ensure component is still mounted before setting state
            setInitialIsActive(userData.isActive);
          }
        }
      } catch (err) {
        console.error("Failed to fetch user:", err);
        if (isMounted) {
          setError(err.message || 'Failed to fetch user data.'); // Set general error
        }
      }
    };

    const loadData = async () => {
      await fetchRoles(); // Fetch roles first
      if (isEditMode) {
        await fetchUser(); // Then fetch user if in edit mode
      }
      if (isMounted) {
        setLoading(false); // Overall loading finished
        setError(null); // Clear general error if all successful
      }
    };

    loadData();

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
      roles: formData.roles, // formData.roles is already an array of strings
    };

    if (isEditMode) {
      payload.isActive = formData.isActive;
    } else {
      // For create mode, if roles are empty, backend service defaults to ROLE_USER
      // If specific default behavior is needed from frontend (e.g. always send ROLE_USER if empty),
      // it can be handled here. For now, rely on backend logic.
      // payload.isActive is not sent for create, backend defaults it to true.
    }

    try {
      let mainOperationSuccessful = false;
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
            id="roles-select" // ID for the Select component itself
            name="roles" // Make sure this matches the state property
            multiple
            value={formData.roles}
            onChange={handleChange}
            label="Roles" // Required for the InputLabel to float correctly on selection
            renderValue={(selected) => selected.join(', ')}
            disabled={loading || availableRoles.length === 0}
            MenuProps={{
              PaperProps: {
                style: {
                  maxHeight: 224,
                  width: 250,
                },
              },
            }}
          >
            {availableRoles.map((role) => (
              <MenuItem key={role} value={role}>
                {role}
              </MenuItem>
            ))}
          </Select>
          {rolesError && <FormHelperText error>{rolesError}</FormHelperText>}
          {!rolesError && availableRoles.length === 0 && !loading && (
            <FormHelperText>Loading roles...</FormHelperText>
          )}
          <FormHelperText>
            Default role is ROLE_USER if left empty during creation. Select one or more roles.
          </FormHelperText>
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
