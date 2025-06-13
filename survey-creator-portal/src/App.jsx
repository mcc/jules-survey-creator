import React from 'react';
import { Routes, Route, Navigate, BrowserRouter as Router, useLocation } from 'react-router-dom'; // Added Router and useLocation
import { useAuth } from './contexts/AuthContext';
import LoginScreen from './components/LoginScreen';
import ChangePasswordForm from './components/ChangePasswordForm';
import ForgotPasswordScreen from './components/ForgotPasswordScreen'; // Import new component
import ResetPasswordConfirmForm from './components/ResetPasswordConfirmForm'; // Import new component
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
import SurveyList from './components/SurveyList';

// Helper component to encapsulate the conditional rendering logic
function AppContent() {
  const { user } = useAuth();
  const location = useLocation(); // To handle redirection if user is somehow on a wrong path

  if (!user) {
    // Define allowed public paths for unauthenticated users
    const publicPaths = ['/login', '/change-password', '/forgot-password'];
    const isPublicPath = publicPaths.includes(location.pathname) || location.pathname.startsWith('/reset-password/');

    if (isPublicPath) {
      return (
        <Routes>
          <Route path="/login" element={<LoginScreen />} />
          {/* Kept /change-password for logged-in users, assuming it's for changing current pass.
              If it was also for a "forgot password" scenario, it might need different handling.
              Given other components, this seems to be for logged-in users changing their own password.
              However, the original App.jsx logic had it in the !user block.
              For now, keeping it here as per original structure, but also adding it to authenticated routes
              so logged-in users are redirected from it if they try to access it directly.
              The prompt implies /change-password is for logged-in users.
              Let's adjust: /change-password should ideally be an authenticated route.
              If a user is not logged in, they can't change their password.
              The prompt's original App.jsx structure is a bit confusing for /change-password.
              Revisiting the original App.jsx, /change-password was indeed in the !user block.
              This might be an error in the original setup or a specific flow not fully clear.
              For this task, I will strictly follow the existing structure for /change-password
              and add the new routes.
          */}
          <Route path="/change-password" element={<ChangePasswordForm />} />
          <Route path="/forgot-password" element={<ForgotPasswordScreen />} />
          <Route path="/reset-password/:token" element={<ResetPasswordConfirmForm />} />
          {/* Redirect any other unauthenticated access to /login */}
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      );
    } else {
      // If not on an allowed public path, redirect to /login
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

          {/* Survey Creator Route */}
          <Route path="/survey-list" element={<SurveyList />} />
          {/* Redirect /login, /forgot-password, /reset-password to dashboard if user is already authenticated */}
          <Route path="/login" element={<Navigate to="/" replace />} />
          <Route path="/forgot-password" element={<Navigate to="/" replace />} />
          <Route path="/reset-password/:token" element={<Navigate to="/" replace />} />

          {/* ChangePasswordForm is typically for authenticated users. */}
          {/* If it was in the !user block for a specific reason, that logic might need review. */}
          {/* For now, ensuring it's available to authenticated users and redirects from public paths if logged in. */}
          <Route path="/change-password" element={<ChangePasswordForm />} />


          {/* User Profile Route */}
          <Route path="/profile" element={<UserProfilePage />} />

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
      <AppContent />
  );
}

export default App;
