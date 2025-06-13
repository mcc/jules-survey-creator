import React, { useState } from 'react';
import { TextField, Button, Box, Typography, Alert } from '@mui/material';
import { changeCurrentUserPassword } from '../services/userService'; // Ensure path is correct

const ChangePasswordForm = () => {
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setSuccess('');

    if (!oldPassword || !newPassword || !confirmPassword) {
      setError('All fields are required.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setError('New passwords do not match.');
      return;
    }

    // Optional: Add client-side complexity rules for newPassword here if desired
    // For example:
    // if (newPassword.length < 8) {
    //   setError('New password must be at least 8 characters long.');
    //   return;
    // }

    setLoading(true);
    try {
      const response = await changeCurrentUserPassword({ oldPassword, newPassword });
      setSuccess(response.message || 'Password changed successfully!'); // Assuming backend returns a message field
      setOldPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err) {
      const errorMessage = err?.message || err?.error || 'Failed to change password. Please try again.';
      setError(errorMessage);
      console.error("Password change error:", err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box
      component="form"
      onSubmit={handleSubmit}
      sx={{
        display: 'flex',
        flexDirection: 'column',
        gap: 2,
        maxWidth: '400px', // Adjust as needed
        p: 2,
        border: '1px solid #ccc', // Optional: for visual separation
        borderRadius: '4px'     // Optional
      }}
    >
      <Typography variant="h6" component="h3">
        Change Password
      </Typography>
      {error && <Alert severity="error">{error}</Alert>}
      {success && <Alert severity="success">{success}</Alert>}
      <TextField
        label="Current Password"
        type="password"
        value={oldPassword}
        onChange={(e) => setOldPassword(e.target.value)}
        variant="outlined"
        fullWidth
        required
        disabled={loading}
      />
      <TextField
        label="New Password"
        type="password"
        value={newPassword}
        onChange={(e) => setNewPassword(e.target.value)}
        variant="outlined"
        fullWidth
        required
        disabled={loading}
      />
      <TextField
        label="Confirm New Password"
        type="password"
        value={confirmPassword}
        onChange={(e) => setConfirmPassword(e.target.value)}
        variant="outlined"
        fullWidth
        required
        disabled={loading}
      />
      <Button
        type="submit"
        variant="contained"
        color="primary"
        disabled={loading}
        sx={{ mt: 1 }}
      >
        {loading ? 'Changing...' : 'Change Password'}
      </Button>
    </Box>
  );
};

export default ChangePasswordForm;
