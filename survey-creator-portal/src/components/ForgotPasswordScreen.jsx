import React, { useState } from 'react';
import { TextField, Button, Box, Typography, Alert, CircularProgress } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import { requestPasswordReset } from '../services/userService';

const ForgotPasswordScreen = () => {
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setSuccess('');

    if (!email) {
      setError('Email address is required.');
      return;
    }
    // Basic email format validation (optional, as backend will validate)
    if (!/\S+@\S+\.\S+/.test(email)) {
        setError('Please enter a valid email address.');
        return;
    }

    setLoading(true);
    try {
      const response = await requestPasswordReset(email);
      // The backend often returns a generic success message to prevent email enumeration
      setSuccess(response.message || 'If your email is registered, you will receive a password reset link shortly.');
      setEmail(''); // Clear email field on success
    } catch (err) {
      // Even on error, backend might send a generic success-like message
      // or a specific error if it's not about user existence (e.g., server issue)
      const errorMessage = err?.message || err?.error || 'An error occurred. Please try again.';
      // To prevent email enumeration, we might want to show a generic success message even on some errors.
      // However, for this example, we'll display the error or a generic one.
      // If the backend is configured to always return OK to prevent enumeration, the catch block might not be hit for "user not found".
      setError(errorMessage);
      console.error("Forgot password request error:", err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box
      display="flex"
      flexDirection="column"
      alignItems="center"
      justifyContent="center"
      sx={{ mt: 4, p:2 }}
    >
      <Box
        component="form"
        onSubmit={handleSubmit}
        sx={{
          display: 'flex',
          flexDirection: 'column',
          gap: 2,
          width: '100%',
          maxWidth: '400px',
          p: {xs: 2, sm: 3},
          border: '1px solid #ccc',
          borderRadius: '8px',
          boxShadow: '0 2px 10px rgba(0,0,0,0.1)',
        }}
      >
        <Typography variant="h5" component="h1" sx={{ textAlign: 'center', mb: 2 }}>
          Forgot Your Password?
        </Typography>
        <Typography variant="body2" sx={{ textAlign: 'center', mb: 2 }}>
          Enter your email address below, and we'll send you a link to reset your password.
        </Typography>
        {error && <Alert severity="error" sx={{ width: '100%' }}>{error}</Alert>}
        {success && <Alert severity="success" sx={{ width: '100%' }}>{success}</Alert>}

        <TextField
          label="Email Address"
          type="email"
          value={email}
          onChange={(e) => {
            setEmail(e.target.value);
            if(error) setError(''); // Clear error on new input
            if(success) setSuccess(''); // Clear success on new input
          }}
          variant="outlined"
          fullWidth
          required
          disabled={loading || !!success} // Disable if successful
        />
        <Button
          type="submit"
          variant="contained"
          color="primary"
          disabled={loading || !!success} // Disable if successful
          sx={{ mt: 1, py: 1.5 }}
          fullWidth
        >
          {loading ? <CircularProgress size={24} color="inherit" /> : 'Send Reset Link'}
        </Button>
        <Button
            component={RouterLink}
            to="/login"
            color="primary"
            sx={{ mt: 1 }}
            fullWidth
        >
            Back to Login
        </Button>
      </Box>
    </Box>
  );
};

export default ForgotPasswordScreen;
