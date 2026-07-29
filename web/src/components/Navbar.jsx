import React, { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Sun, Moon, RefreshCw, LogOut, Wallet, BarChart3, Receipt, PiggyBank, ShieldAlert, Cpu, Sparkles } from 'lucide-react';
import { syncWithServer, isLocalMode } from '../utils/api';

export default function Navbar({ isAuthenticated, onLogout, darkMode, setDarkMode }) {
  const location = useLocation();
  const [syncing, setSyncing] = useState(false);
  const [syncStatus, setSyncStatus] = useState('');

  const handleSync = async () => {
    setSyncing(true);
    setSyncStatus('Syncing...');
    const res = await syncWithServer();
    setSyncing(false);
    if (res.success) {
      setSyncStatus('Synced successfully!');
      setTimeout(() => setSyncStatus(''), 3000);
    } else {
      setSyncStatus(`Sync failed: ${res.reason}`);
      setTimeout(() => setSyncStatus(''), 4000);
    }
  };

  const navItems = [
    { name: 'Dashboard', path: '/dashboard', icon: Wallet },
    { name: 'Analytics', path: '/analytics', icon: BarChart3 },
    { name: 'Budgets', path: '/budgets', icon: Receipt },
    { name: 'Savings', path: '/savings', icon: PiggyBank },
    { name: 'Smart Rules', path: '/rules', icon: ShieldAlert },
    { name: 'AI Assistant', path: '/chat', icon: Sparkles },
  ];

  return (
    <nav className="sticky top-0 z-50 glass shadow-sm transition-all duration-300">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          <div className="flex items-center">
            <Link to="/dashboard" className="flex items-center space-x-2">
              <Cpu className="h-8 w-8 text-primary" />
              <span className="font-extrabold text-xl tracking-tight bg-gradient-to-r from-primary to-indigo-400 bg-clip-text text-transparent">
                Upgrade Finance
              </span>
            </Link>
            <div className="hidden md:flex space-x-1 ml-10">
              {navItems.map((item) => {
                const Icon = item.icon;
                const active = location.pathname === item.path;
                return (
                  <Link
                    key={item.name}
                    to={item.path}
                    className={`flex items-center space-x-1.5 px-3 py-2 rounded-lg text-sm font-medium transition-all ${
                      active
                        ? 'bg-primary/10 text-primary dark:bg-primary/20 dark:text-indigo-400'
                        : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
                    }`}
                  >
                    <Icon className="h-4 w-4" />
                    <span>{item.name}</span>
                  </Link>
                );
              })}
            </div>
          </div>

          <div className="flex items-center space-x-3">
            {syncStatus && (
              <span className="text-xs font-semibold px-2.5 py-1 rounded bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400">
                {syncStatus}
              </span>
            )}
            
            {!isLocalMode() && (
              <button
                onClick={handleSync}
                disabled={syncing}
                className="p-2 rounded-lg text-slate-500 hover:text-slate-800 dark:hover:text-slate-200 transition-colors disabled:opacity-50"
                title="Sync database changes"
              >
                <RefreshCw className={`h-5 w-5 ${syncing ? 'animate-spin' : ''}`} />
              </button>
            )}

            <button
              onClick={() => setDarkMode(!darkMode)}
              className="p-2 rounded-lg text-slate-500 hover:text-slate-800 dark:hover:text-slate-200 transition-colors"
            >
              {darkMode ? <Sun className="h-5 w-5" /> : <Moon className="h-5 w-5" />}
            </button>

            {isAuthenticated ? (
              <button
                onClick={onLogout}
                className="flex items-center space-x-1 px-3 py-2 text-sm font-medium text-red-500 hover:bg-red-50/50 dark:hover:bg-red-950/20 rounded-lg transition-colors"
              >
                <LogOut className="h-4 w-4" />
                <span className="hidden sm:inline">Logout</span>
              </button>
            ) : (
              <Link
                to="/login"
                className="flex items-center space-x-1 px-3.5 py-2 text-sm font-medium bg-primary text-white hover:bg-primary-hover rounded-lg transition-all shadow-sm"
              >
                Sign In
              </Link>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
}
