import React from 'react';
import { Typography, Paper, Box } from '@mui/material';

const Dashboard = () => {
  return (
    <Paper sx={{ p: 2, margin: 'auto', maxWidth: 1000, flexGrow: 1 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h6" component="h2">
          Dashboard
        </Typography>
      </Box>
      <Typography variant="body1">
        Welcome to your dashboard! This is a placeholder page.
      </Typography>
      {/* Add more dashboard specific content here later */}
    </Paper>
  );
};

export default Dashboard;
