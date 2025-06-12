import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { createUser, editUser, getUser } from '../services/userService'; // Adjust path
import {
  TextField, Button, Box, Typography, CircularProgress, Alert, Paper,
  FormGroup, FormControlLabel, Checkbox
} from '@mui/material';

const UserForm = () => {
  const { userId } = useParams(); // Get userId from URL if editing
  const navigate = useNavigate();
  const isEditMode = Boolean(userId);

  const [formData, setFormData] = useState({
    username: '',
    email: '',
    roles: '', // Comma-separated roles e.g., "ROLE_USER,ROLE_ADMIN"
    isActive: true,
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);


  useEffect(() => {
    if (isEditMode) {
      const fetchUser = async () => {
        setLoading(true);
        try {
          const userData = await getUser(userId);
          setFormData({
            username: userData.username,
            email: userData.email,
            roles: userData.roles.map(role => typeof role === 'string' ? role : role.name).join(','), // Handle if roles are objects or strings
            isActive: userData.isActive,
          });
          setError(null);
        } catch (err) {
          setError(err.message || 'Failed to fetch user data.');
        } finally {
          setLoading(false);
        }
      };
      fetchUser();
    }
  }, [userId, isEditMode]);

  const handleChange = (event) => {
    const { name, value, type, checked } = event.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError(null);
    setSuccessMessage(null);

    const rolesArray = formData.roles.split(',').map(role => role.trim()).filter(role => role);
    const payload = {
      username: formData.username,
      email: formData.email,
      roles: rolesArray,
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
      if (isEditMode) {
        await editUser(userId, payload);
        setSuccessMessage('User updated successfully!');
      } else {
        await createUser(payload);
        setSuccessMessage('User created successfully!');
      }
      // Optionally navigate after a short delay or on button click
      setTimeout(() => {
        navigate('/admin/users'); // Navigate back to user list
      }, 2000);
    } catch (err) {
      setError(err.message || (isEditMode ? 'Failed to update user.' : 'Failed to create user.'));
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
          disabled={loading}
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
        <TextField
          margin="normal"
          fullWidth
          id="roles"
          label="Roles (comma-separated, e.g., ROLE_USER,ROLE_ADMIN)"
          name="roles"
          value={formData.roles}
          onChange={handleChange}
          disabled={loading}
          helperText="Default role is ROLE_USER if left empty during creation."
        />
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
