import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './contexts/AuthContext';
import LoginScreen from './components/LoginScreen';
import SideMenu from './components/SideMenu';
import MainPanel from './components/MainPanel';
import UserList from './components/UserList';
import UserForm from './components/UserForm'; // Added UserForm import
import Dashboard from './components/Dashboard';
import UserProfilePage from './components/UserProfilePage'; // Added UserProfilePage import
// import SurveyCreator from './components/SurveyCreator';
import SurveyJsCreatorComponent from './components/SurveyJsCreatorComponent';
import { Box, CssBaseline } from '@mui/material';
import './App.css';

function App() {
  const { user } = useAuth();

  if (!user) {
    return <LoginScreen />;
  }

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

          {/* User Profile Route */}
          <Route path="/profile" element={<UserProfilePage />} />

          {/* Fallback Route - Consider if /dashboard is more appropriate than / if Dashboard is the main landing page */}
          <Route path="*" element={<Navigate to="/" />} />
        </Routes>
      </MainPanel>
    </Box>
  );
}

export default App;
