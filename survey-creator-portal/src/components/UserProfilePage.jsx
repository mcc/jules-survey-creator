import React, { useState, useEffect, useContext } from 'react';
import { Container, Typography, Paper, Box, TextField, Button, CircularProgress, Alert } from '@mui/material';
import ChangePasswordForm from './ChangePasswordForm'; // Adjust path if needed
import { useAuth } from '../contexts/AuthContext'; // Assuming AuthContext provides user info
import { getUser, editUser } from '../services/userService'; // Assuming userService has these functions

const UserProfilePage = () => {
  const { user: authUser } = useAuth(); // Get authenticated user details
  const [profileData, setProfileData] = useState(null);
  const [editableData, setEditableData] = useState({
    rank: '',
    post: '',
    englishName: '',
    chineseName: '',
    email: '', // Also making email editable for completeness, can be restricted if needed
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    if (authUser && authUser.id) {
      setLoading(true);
      getUser(authUser.id)
        .then(response => {
          setProfileData(response);
          setEditableData({
            rank: response.rank || '',
            post: response.post || '',
            englishName: response.englishName || '',
            chineseName: response.chineseName || '',
            email: response.email || '',
          });
          setLoading(false);
        })
        .catch(err => {
          console.error("Failed to fetch user profile:", err);
          setError('Failed to load user profile. Please try again later.');
          setLoading(false);
        });
    }
  }, [authUser]);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setEditableData(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    setError('');
    setSuccess('');
    if (!authUser || !authUser.id) {
      setError("User not authenticated.");
      return;
    }

    // Construct payload for editUser, ensure it matches EditUserRequest DTO
    // Only include fields that are meant to be editable on this page.
    // Username is not typically changed on a profile page this way, so it's omitted.
    // Roles and isActive are typically managed by admins.
    const updatePayload = {
      // username: profileData.username, // if username can be updated, get it from a field
      email: editableData.email,
      rank: editableData.rank,
      post: editableData.post,
      englishName: editableData.englishName,
      chineseName: editableData.chineseName,
      // isActive: profileData.isActive, // if isActive can be updated by user
      // roles: profileData.roles ? profileData.roles.map(r => r.name || r) : [], // if roles can be updated
    };


    editUser(authUser.id, updatePayload)
      .then(updatedUser => {
        setProfileData(updatedUser); // Update profile data with the response
         setEditableData({ // Reset editable fields from the new profile data
            rank: updatedUser.rank || '',
            post: updatedUser.post || '',
            englishName: updatedUser.englishName || '',
            chineseName: updatedUser.chineseName || '',
            email: updatedUser.email || '',
          });
        setSuccess('Profile updated successfully!');
      })
      .catch(err => {
        console.error("Failed to update profile:", err);
        setError(err.message || 'Failed to update profile. Please check your input and try again.');
      });
  };

  if (loading) {
    return (
      <Container maxWidth="md" sx={{ mt: 4, mb: 4, display: 'flex', justifyContent: 'center' }}>
        <CircularProgress />
      </Container>
    );
  }

  return (
    <Container maxWidth="md" sx={{ mt: 4, mb: 4 }}>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          User Profile
        </Typography>

        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        {success && <Alert severity="success" sx={{ mb: 2 }}>{success}</Alert>}

        {profileData && (
          <Box component="form" onSubmit={handleSubmit} noValidate sx={{ mt: 1 }}>
            <Typography variant="h6">User Details</Typography>
            <Typography>Username: {profileData.username}</Typography>
            {/* <Typography>Email: {profileData.email}</Typography> */}
            <TextField
              margin="normal"
              fullWidth
              id="email"
              label="Email Address"
              name="email"
              autoComplete="email"
              value={editableData.email}
              onChange={handleChange}
            />
            <TextField
              margin="normal"
              fullWidth
              id="rank"
              label="Rank"
              name="rank"
              value={editableData.rank}
              onChange={handleChange}
            />
            <TextField
              margin="normal"
              fullWidth
              id="post"
              label="Post"
              name="post"
              value={editableData.post}
              onChange={handleChange}
            />
            <TextField
              margin="normal"
              fullWidth
              id="englishName"
              label="English Name"
              name="englishName"
              value={editableData.englishName}
              onChange={handleChange}
            />
            <TextField
              margin="normal"
              fullWidth
              id="chineseName"
              label="Chinese Name"
              name="chineseName"
              value={editableData.chineseName}
              onChange={handleChange}
            />
            <Typography sx={{mt: 1}}>Roles: {profileData.roles && profileData.roles.join(', ')}</Typography>
            <Typography>Status: {profileData.isActive ? 'Active' : 'Inactive'}</Typography>

            <Button
              type="submit"
              fullWidth
              variant="contained"
              sx={{ mt: 3, mb: 2 }}
            >
              Save Profile Changes
            </Button>
          </Box>
        )}

        <Box sx={{ mt: 4 }}> {/* Increased margin top for separation */}
          <Typography variant="h6" sx={{ mb: 1 }}>Change Password</Typography>
          <ChangePasswordForm />
        </Box>
      </Paper>
    </Container>
  );
};

export default UserProfilePage;
