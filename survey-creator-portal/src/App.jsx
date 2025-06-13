import React from 'react';
import { Routes, Route, Navigate, BrowserRouter as Router, useLocation } from 'react-router-dom'; // Added Router and useLocation
import { useAuth } from './contexts/AuthContext';
import LoginScreen from './components/LoginScreen';
import ChangePasswordForm from './components/ChangePasswordForm'; // Import new component
import SideMenu from './components/SideMenu';
import MainPanel from './components/MainPanel';
import UserList from './components/UserList';
import UserForm from './components/UserForm'; // Added UserForm import
import Dashboard from './components/Dashboard';
// import SurveyCreator from './components/SurveyCreator';
import SurveyJsCreatorComponent from './components/SurveyJsCreatorComponent';
import { Box, CssBaseline } from '@mui/material';
import './App.css';

// Helper component to encapsulate the conditional rendering logic
function AppContent() {
  const { user } = useAuth();
  const location = useLocation(); // To handle redirection if user is somehow on a wrong path

  if (!user) {
    // If not authenticated, only allow access to /login or /change-password
    if (location.pathname === '/login' || location.pathname === '/change-password') {
      return (
        <Routes>
          <Route path="/login" element={<LoginScreen />} />
          <Route path="/change-password" element={<ChangePasswordForm />} />
          {/* Redirect any other unauthenticated access to /login */}
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      );
    } else {
      // If not on /login or /change-password, redirect to /login
      return <Navigate to="/login" replace />;
    }
  }

  // Authenticated user view
  return (
    <Box sx={{ display: 'flex' }} className="App">
      <CssBaseline /> {/* Ensures consistent baseline styling */}
      <SideMenu />
      <MainPanel>
        <Routes>
          <Route path="/" element={<Dashboard />} />
          {/* Admin User Management Routes */}
          <Route path="/admin/users" element={<UserList />} />
          <Route path="/admin/users/new" element={<UserForm />} />
          <Route path="/admin/users/:userId/edit" element={<UserForm />} />

          {/* Survey Creator Route */}
          <Route path="/survey-creator" element={<SurveyJsCreatorComponent />} />

          {/* Redirect /login and /change-password to dashboard if user is already authenticated */}
          <Route path="/login" element={<Navigate to="/" replace />} />
          <Route path="/change-password" element={<Navigate to="/" replace />} />

          {/* Fallback Route - Consider if /dashboard is more appropriate than / if Dashboard is the main landing page */}
          <Route path="*" element={<Navigate to="/" />} />
        </Routes>
      </MainPanel>
    </Box>
  );
}

function App() {
  // Wrapping with Router here if it's not already higher up in the component tree (e.g. in index.js)
  // Based on the prompt, it seems Router should be here.
  return (
    <Router>
      <AppContent />
    </Router>
  );
}

export default App;
