// survey-creator-portal/src/main.jsx
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import './index.css';
import AppWrapper from './App.jsx'; // Import AppWrapper
import { AuthProvider } from './contexts/AuthContext.jsx';

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <AppWrapper /> {/* Use AppWrapper */}
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
);
