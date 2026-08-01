import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate, Link } from 'react-router-dom';
import Dashboard from './pages/Dashboard';
import Analytics from './pages/Analytics';
import Budgets from './pages/Budgets';
import Savings from './pages/Savings';
import Rules from './pages/Rules';
import Chat from './pages/Chat';
import Login from './pages/Login';
import Navbar from './components/Navbar';

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(!!localStorage.getItem('auth_token'));
  const [darkMode, setDarkMode] = useState(localStorage.getItem('theme') === 'dark');
  const [themePreset, setThemePreset] = useState(localStorage.getItem('theme_preset') || 'obsidian');

  useEffect(() => {
    const root = window.document.documentElement;
    if (darkMode) {
      root.classList.add('dark');
      localStorage.setItem('theme', 'dark');
    } else {
      root.classList.remove('dark');
      localStorage.setItem('theme', 'light');
    }

    // Apply active theme class
    root.classList.remove('theme-obsidian', 'theme-emerald', 'theme-cyberpunk');
    root.classList.add(`theme-${themePreset}`);
    localStorage.setItem('theme_preset', themePreset);
  }, [darkMode, themePreset]);

  const handleLogout = () => {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('auth_email');
    localStorage.removeItem('last_sync_timestamp');
    setIsAuthenticated(false);
  };

  return (
    <Router>
      <div className={`min-h-screen bg-background-light dark:bg-background-dark text-slate-800 dark:text-slate-100 transition-colors duration-300`}>
        <Navbar 
          isAuthenticated={isAuthenticated} 
          onLogout={handleLogout} 
          darkMode={darkMode} 
          setDarkMode={setDarkMode} 
          themePreset={themePreset}
          setThemePreset={setThemePreset}
        />
        <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <Routes>
            <Route path="/login" element={
              isAuthenticated ? <Navigate to="/dashboard" /> : <Login onAuthSuccess={() => setIsAuthenticated(true)} />
            } />
            
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/analytics" element={<Analytics />} />
            <Route path="/budgets" element={<Budgets />} />
            <Route path="/savings" element={<Savings />} />
            <Route path="/rules" element={<Rules />} />
            <Route path="/chat" element={<Chat />} />

            <Route path="*" element={<Navigate to="/dashboard" />} />
          </Routes>
        </main>
      </div>
    </Router>
  );
}

export default App;
