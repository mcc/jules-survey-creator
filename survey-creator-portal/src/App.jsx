// survey-creator-portal/src/App.jsx
import React, { useState, useEffect, useMemo, createContext, useContext } from 'react';
import { Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './contexts/AuthContext';
import LoginScreen from './components/LoginScreen';
import ChangePasswordForm from './components/ChangePasswordForm';
import ForgotPasswordScreen from './components/ForgotPasswordScreen';
import ResetPasswordConfirmForm from './components/ResetPasswordConfirmForm';
import SideMenu from './components/SideMenu';
import MainPanel from './components/MainPanel';
import UserList from './components/UserList';
import UserForm from './components/UserForm';
import Dashboard from './components/Dashboard';
import UserProfilePage from './components/UserProfilePage';
import SurveyJsCreatorComponent from './components/SurveyJsCreatorComponent';
import SurveyList from './components/SurveyList';
import ThemeToggler from './components/ThemeToggler';
import {
    getSurvey,
    createSurvey,
    updateSurvey,
    fetchSharedUsers, // Placeholder: To be implemented in surveyService.js
    shareSurvey,      // Placeholder: To be implemented in surveyService.js
    unshareSurvey     // Placeholder: To be implemented in surveyService.js
} from './services/surveyService.js';
import { Box, CssBaseline } from '@mui/material';
import { ThemeProvider } from '@mui/material/styles';
import { getTheme } from './theme';
import './index.css';

export const ThemeModeContext = createContext({
  themeMode: 'light',
  themePreference: 'system',
  toggleThemePreference: (preference) => {},
});

const ThemeModeProvider = ({ children }) => {
  const [themePreference, setThemePreference] = useState(() => {
    return localStorage.getItem('themePreference') || 'system';
  });
  const [actualThemeMode, setActualThemeMode] = useState('light');

  useEffect(() => {
    const systemPrefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    let currentMode = 'light';

    if (themePreference === 'dark') {
      currentMode = 'dark';
    } else if (themePreference === 'system' && systemPrefersDark) {
      currentMode = 'dark';
    }

    setActualThemeMode(currentMode);

    if (currentMode === 'dark') {
      document.body.classList.add('dark-theme');
      document.body.classList.remove('light-theme');
    } else {
      document.body.classList.add('light-theme');
      document.body.classList.remove('dark-theme');
    }
  }, [themePreference]);

  useEffect(() => {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
    const handleSystemChange = (e) => {
      if (themePreference === 'system') {
        const newMode = e.matches ? 'dark' : 'light';
        setActualThemeMode(newMode);
        if (newMode === 'dark') {
          document.body.classList.add('dark-theme');
          document.body.classList.remove('light-theme');
        } else {
          document.body.classList.add('light-theme');
          document.body.classList.remove('dark-theme');
        }
      }
    };

    mediaQuery.addEventListener('change', handleSystemChange);
    return () => mediaQuery.removeEventListener('change', handleSystemChange);
  }, [themePreference]);

  const toggleThemePreference = (preference) => {
    localStorage.setItem('themePreference', preference);
    setThemePreference(preference);
  };

  return (
    <ThemeModeContext.Provider value={{ themeMode: actualThemeMode, themePreference, toggleThemePreference }}>
      {children}
    </ThemeModeContext.Provider>
  );
};

function AppContent() {
  const { user } = useAuth();
  const location = useLocation();

  if (!user) {
    return (
      <Routes>
        <Route path="/login" element={<LoginScreen />} />
        <Route path="/change-password" element={<ChangePasswordForm />} />
        <Route path="/forgot-password" element={<ForgotPasswordScreen />} />
        <Route path="/reset-password/:token" element={<ResetPasswordConfirmForm />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    );
  }

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }} className="App">
      <SideMenu />
      <MainPanel>
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/admin/users" element={<UserList />} />
          <Route path="/admin/users/new" element={<UserForm />} />
          <Route path="/admin/users/:userId/edit" element={<UserForm />} />
          <Route
            path="/survey-creator/:surveyId?"
            element={
              <SurveyJsCreatorComponent
                onGetSurvey={getSurvey}
                onCreateSurvey={createSurvey}
                onUpdateSurvey={updateSurvey}
                onFetchSharedUsers={fetchSharedUsers} // Pass placeholder
                onShareSurvey={shareSurvey}         // Pass placeholder
                onUnshareSurvey={unshareSurvey}     // Pass placeholder
              />
            }
          />
          <Route path="/survey-list" element={<SurveyList />} />
          <Route path="/login" element={<Navigate to="/" replace />} />
          <Route path="/forgot-password" element={<Navigate to="/" replace />} />
          <Route path="/reset-password/:token" element={<Navigate to="/" replace />} />
          <Route path="/change-password" element={<ChangePasswordForm />} />
          <Route path="/profile" element={<UserProfilePage />} />
          <Route path="*" element={<Navigate to="/" />} />
        </Routes>
      </MainPanel>
    </Box>
  );
}

function App() {
  const { themeMode } = useContext(ThemeModeContext);
  const muiTheme = useMemo(() => getTheme(themeMode), [themeMode]);

  return (
    <ThemeProvider theme={muiTheme}>
      <CssBaseline />
      <ThemeToggler />
      <AppContent />
    </ThemeProvider>
  );
}

export default function AppWrapper() {
  return (
    <ThemeModeProvider>
      <App />
    </ThemeModeProvider>
  );
}
