import React from 'react';
import { Box, Typography, Toolbar } from '@mui/material';

const MainPanel = ({ children }) => {
  return (
    <Box
      component="main"
      sx={{ flexGrow: 1, p: 3 }}
    >
      <Toolbar /> {/* To offset content below AppBar if one were present */}
      {children ? children : (
        <Typography variant="h6">
          Welcome to the Main Panel!
        </Typography>
      )}
    </Box>
  );
};

export default MainPanel;
