import React, { useState, useEffect, useCallback } from 'react';

const THEME_KEY = 'themePreference';

function ThemeToggler() {
  const [theme, setTheme] = useState('system'); // 'light', 'dark', 'system'

  const applyTheme = useCallback((selectedTheme) => {
    if (selectedTheme === 'light') {
      document.body.classList.add('light-theme');
      document.body.classList.remove('dark-theme');
    } else if (selectedTheme === 'dark') {
      document.body.classList.add('dark-theme');
      document.body.classList.remove('light-theme');
    } else { // system
      document.body.classList.remove('light-theme');
      document.body.classList.remove('dark-theme');
    }
  }, []);

  useEffect(() => {
    const storedTheme = localStorage.getItem(THEME_KEY);
    const initialTheme = storedTheme || 'system';
    setTheme(initialTheme);
    applyTheme(initialTheme);
  }, [applyTheme]);

  const handleThemeChange = (newTheme) => {
    setTheme(newTheme);
    localStorage.setItem(THEME_KEY, newTheme);
    applyTheme(newTheme);
  };

  return (
    <div style={{ position: 'fixed', top: '10px', right: '10px', zIndex: 1000, background: 'rgba(255,255,255,0.1)', padding: '10px', borderRadius: '5px' }}>
      <label htmlFor="theme-select" style={{ marginRight: '5px', color: '#888' }}>Theme:</label>
      <select id="theme-select" value={theme} onChange={(e) => handleThemeChange(e.target.value)} style={{padding: '5px'}}>
        <option value="system">System</option>
        <option value="light">Light</option>
        <option value="dark">Dark</option>
      </select>
    </div>
  );
}

export default ThemeToggler;
