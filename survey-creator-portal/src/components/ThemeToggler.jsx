// survey-creator-portal/src/components/ThemeToggler.jsx
import React, { useContext } from 'react'; // Import useContext
import { ThemeModeContext } from '../App.jsx'; // Adjust path as needed, assuming App.jsx is in src/

// THEME_KEY is no longer needed here as localStorage is handled by ThemeModeProvider

function ThemeToggler() {
  // Get themePreference and toggleThemePreference from context
  const { themePreference, toggleThemePreference } = useContext(ThemeModeContext);

  // The local 'theme' state, 'applyTheme', and 'useEffect' are no longer needed here.
  // ThemeModeProvider in App.jsx now manages the theme state, localStorage, and body classes.

  const handleThemeChange = (newPreference) => {
    toggleThemePreference(newPreference);
  };

  // The component might not be visible if themePreference is not yet available from context during initial render.
  // However, ThemeModeProvider initializes themePreference from localStorage, so it should be available.
  if (!toggleThemePreference) {
    // This can happen if the context is not properly provided yet, though unlikely with the setup.
    // Or, if the ThemeToggler is rendered outside a ThemeModeProvider.
    // For robustness, you could return null or a placeholder.
    console.warn("ThemeToggler rendered outside of ThemeModeProvider or context not available yet.");
    return null;
  }

  return (
    <div style={{ position: 'fixed', top: '10px', right: '10px', zIndex: 1000, background: 'rgba(128,128,128,0.1)', padding: '10px', borderRadius: '5px' }}>
      <label htmlFor="theme-select" style={{ marginRight: '5px', color: 'inherit' }}>Theme:</label> {/* Changed color to inherit for better theme adaptability */}
      <select
        id="theme-select"
        value={themePreference} // Bind to themePreference from context
        onChange={(e) => handleThemeChange(e.target.value)}
        style={{padding: '5px'}}
      >
        <option value="system">System</option>
        <option value="light">Light</option>
        <option value="dark">Dark</option>
      </select>
    </div>
  );
}

export default ThemeToggler;
