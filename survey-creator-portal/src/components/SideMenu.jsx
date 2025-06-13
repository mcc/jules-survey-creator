import React from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { Drawer, List, ListItemButton, ListItemIcon, ListItemText, Toolbar, Divider, Box, Typography } from '@mui/material';
import DashboardIcon from '@mui/icons-material/Dashboard';
import PeopleIcon from '@mui/icons-material/People';
import PostAddIcon from '@mui/icons-material/PostAdd';
import AccountCircleIcon from '@mui/icons-material/AccountCircle';
import LogoutIcon from '@mui/icons-material/Logout';
import BusinessIcon from '@mui/icons-material/Business'; // For Services
import GroupWorkIcon from '@mui/icons-material/GroupWork'; // For Teams

const drawerWidth = 240;

const SideMenu = () => {
  const { user, logout } = useAuth(); // Get user from useAuth
  const navigate = useNavigate();

  // Helper to check for role - adjust if your role structure is different
  const hasSystemAdminRole = user && user.roles && user.roles.some(role => role.name === 'ROLE_SYSTEM_ADMIN');
  const hasUserAdminRole = user && user.roles && user.roles.some(role => role.name === 'ROLE_USER_ADMIN');


  const handleLogout = () => {
    logout();
    navigate('/login'); // Redirect to login screen after logout
  };

  return (
    <Drawer
      variant="permanent"
      sx={{
        width: drawerWidth,
        flexShrink: 0,
        [`& .MuiDrawer-paper`]: {
          width: drawerWidth,
          boxSizing: 'border-box',
          display: 'flex',
          flexDirection: 'column',
        },
      }}
    >
      <Box sx={{ flexGrow: 1 }}>
        <Toolbar /> {/* To offset content below AppBar if one were present */}
        <List>
          <ListItemButton component={RouterLink} to="/">
            <ListItemIcon>
              <DashboardIcon />
            </ListItemIcon>
            <ListItemText primary="Dashboard" />
          </ListItemButton>

          {/* User Profile Link (visible to all logged-in users) */}
          <ListItemButton component={RouterLink} to="/profile">
            <ListItemIcon>
              <AccountCircleIcon />
            </ListItemIcon>
            <ListItemText primary="Profile" />
          </ListItemButton>

          {/* Survey Links (visible to all logged-in users) */}
          <ListItemButton component={RouterLink} to="/survey-list">
            <ListItemIcon>
              <PostAddIcon />
            </ListItemIcon>
            <ListItemText primary="Survey List" />
          </ListItemButton>
          <ListItemButton component={RouterLink} to="/survey-creator">
            <ListItemIcon>
              <PostAddIcon />
            </ListItemIcon>
            <ListItemText primary="Survey Creator" />
          </ListItemButton>

          {/* Admin Section - Conditionally Rendered */}
          {(hasSystemAdminRole || hasUserAdminRole) && ( // Show Admin section if user is System or User Admin
            <>
              <Divider sx={{ my: 1 }} />
              <Typography variant="overline" sx={{ pl: 2, display: 'block', color: 'text.secondary' }}>
                Admin
              </Typography>
              {/* User Management Link - Visible to System Admin and User Admin */}
              {(hasSystemAdminRole || hasUserAdminRole) && (
                  <ListItemButton component={RouterLink} to="/admin/users">
                    <ListItemIcon>
                      <PeopleIcon />
                    </ListItemIcon>
                    <ListItemText primary="Users" />
                  </ListItemButton>
              )}
              {/* Service and Team Management Links - Visible only to System Admin */}
              {hasSystemAdminRole && (
                <>
                  <ListItemButton component={RouterLink} to="/admin/services">
                    <ListItemIcon>
                      <BusinessIcon />
                    </ListItemIcon>
                    <ListItemText primary="Services" />
                  </ListItemButton>
                  <ListItemButton component={RouterLink} to="/admin/teams">
                    <ListItemIcon>
                      <GroupWorkIcon />
                    </ListItemIcon>
                    <ListItemText primary="Teams" />
                  </ListItemButton>
                </>
              )}
            </>
          )}
        </List>
      </Box>
      <Box sx={{ mt: 'auto' }}> {/* Pushes logout to the bottom */}
        <Divider />
        <List>
          <ListItemButton onClick={handleLogout}>
            <ListItemIcon>
              <LogoutIcon />
            </ListItemIcon>
            <ListItemText primary="Logout" />
          </ListItemButton>
        </List>
      </Box>
    </Drawer>
  );
};

export default SideMenu;
