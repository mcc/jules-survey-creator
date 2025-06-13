import React from 'react';
import { Container, Typography, Paper, Box } from '@mui/material';
import ChangePasswordForm from './ChangePasswordForm'; // Adjust path if needed

const UserProfilePage = () => {
  return (
    <Container maxWidth="md" sx={{ mt: 4, mb: 4 }}>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          User Profile
        </Typography>

        {/* Placeholder for other user profile information if needed in the future */}
        {/* For example:
        <Box sx={{ mb: 4 }}>
          <Typography variant="h6">User Details</Typography>
          <Typography>Username: currentUser.username</Typography>
          <Typography>Email: currentUser.email</Typography>
        </Box>
        */}

        <Box sx={{ mt: 2 }}>
          <ChangePasswordForm />
        </Box>
      </Paper>
    </Container>
  );
};

export default UserProfilePage;
