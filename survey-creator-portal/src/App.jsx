import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './contexts/AuthContext';
import LoginScreen from './components/LoginScreen';
import SideMenu from './components/SideMenu';
import MainPanel from './components/MainPanel';
import UserList from './components/UserList'; // Import UserList
import Dashboard from './components/Dashboard'; // Import Dashboard
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
          <Route path="/users" element={<UserList />} />
          <Route path="/creator" element={<SurveyJsCreatorComponent />} />
          <Route path="*" element={<Navigate to="/" />} /> {/* Redirect unknown paths to Dashboard */}
        </Routes>
      </MainPanel>
    </Box>
  );
}

export default App;
