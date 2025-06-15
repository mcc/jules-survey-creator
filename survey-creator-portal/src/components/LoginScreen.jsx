import React, { useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate, Link as RouterLink } from 'react-router-dom'; // Import Link
import { Button, TextField, Container, Typography, Box, Alert, Grid } from '@mui/material'; // Import Grid for layout

const LoginScreen = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(''); // To store and display login errors
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError(''); // Clear previous errors
    try {
      await login(username, password);
    } catch (error) { // Renamed err to error for clarity
      console.error('Login failed:', error); // Keep for debugging
      // error.message should now contain the message from ErrorResponseDto or the standardized interceptor message
      const displayMessage = error.message || 'Login failed. An unexpected error occurred.';
      setError(displayMessage);

      if (displayMessage.includes('password has expired')) { // Check for specific error message
          // Optionally, keep the error message like "Your password has expired. Please change it."
          // setError('Your password has expired. Please change it.'); // Or append to existing
          navigate('/change-password'); // Redirect to change password page
      }
    }
  };

  return (
    <Container component="main" maxWidth="xs">
      <Box
        sx={{
          marginTop: 8,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
        }}
      >
        <Typography component="h1" variant="h5">
          Login
        </Typography>
        <Box component="form" onSubmit={handleSubmit} sx={{ mt: 1 }}>
          {error && (
            <Alert severity="error" sx={{ width: '100%', mt: 2 }}>
              {error}
            </Alert>
          )}
          <TextField
            margin="normal"
            required
            fullWidth
            id="username"
            label="Name"
            name="username"
            autoComplete="username"
            autoFocus
            value={username}
            onChange={(e) => setUsername(e.target.value)}
          />
          <TextField
            margin="normal"
            required
            fullWidth
            name="password"
            label="Password"
            type="password"
            id="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          <Button
            type="submit"
            fullWidth
            variant="contained"
            sx={{ mt: 3, mb: 2 }}
          >
            Login
          </Button>
          <Grid container justifyContent="flex-end">
            <Grid item>
              <RouterLink to="/forgot-password" variant="body2">
                Forgot password?
              </RouterLink>
            </Grid>
          </Grid>
        </Box>
      </Box>
    </Container>
  );
};

export default LoginScreen;
