import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../utils/api';
import { Mail, Lock, ShieldCheck, ToggleLeft, ToggleRight, Sparkles } from 'lucide-react';

export default function Login({ onAuthSuccess }) {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isRegister, setIsRegister] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  
  const handleAuth = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    const url = isRegister ? '/auth/register' : '/auth/login';
    try {
      const res = await api.post(url, { email, password });
      
      if (!isRegister) {
        localStorage.setItem('auth_token', res.data.token);
        localStorage.setItem('auth_email', res.data.email);
        onAuthSuccess();
        navigate('/dashboard');
      } else {
        setIsRegister(false);
        setError('Registration successful! Please login.');
      }
    } catch (err) {
      setError(err.response?.data || 'Connection to authentication server failed. You can use Anonymous Local Mode below.');
    } finally {
      setLoading(false);
    }
  };

  const handleLocalMode = () => {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('auth_email');
    localStorage.setItem('local_mode_active', 'true');
    onAuthSuccess();
    navigate('/dashboard');
  };

  return (
    <div className="flex flex-col items-center justify-center min-h-[80vh] px-4">
      <div className="w-full max-w-md p-8 glass rounded-2xl shadow-xl space-y-6">
        <div className="text-center space-y-2">
          <div className="inline-flex p-3 rounded-full bg-primary/10 text-primary">
            <ShieldCheck className="h-8 w-8" />
          </div>
          <h2 className="text-3xl font-extrabold tracking-tight">
            {isRegister ? 'Create Account' : 'Welcome Back'}
          </h2>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Track your finances automatically & privately.
          </p>
        </div>

        {error && (
          <div className={`p-3 rounded-lg text-sm font-medium ${error.includes('successful') ? 'bg-green-500/10 text-green-500' : 'bg-red-500/10 text-red-500'}`}>
            {error}
          </div>
        )}

        <form onSubmit={handleAuth} className="space-y-4">
          <div className="space-y-1">
            <label className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">Email Address</label>
            <div className="relative">
              <span className="absolute inset-y-0 left-0 flex items-center pl-3 text-slate-400">
                <Mail className="h-4 w-4" />
              </span>
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full pl-10 pr-4 py-2.5 rounded-lg border border-slate-200 dark:border-slate-800 bg-white/50 dark:bg-slate-900/50 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all text-sm"
                placeholder="you@example.com"
              />
            </div>
          </div>

          <div className="space-y-1">
            <label className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">Password</label>
            <div className="relative">
              <span className="absolute inset-y-0 left-0 flex items-center pl-3 text-slate-400">
                <Lock className="h-4 w-4" />
              </span>
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full pl-10 pr-4 py-2.5 rounded-lg border border-slate-200 dark:border-slate-800 bg-white/50 dark:bg-slate-900/50 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all text-sm"
                placeholder="••••••••"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-2.5 bg-primary hover:bg-primary-hover text-white rounded-lg font-semibold shadow-md transition-all text-sm disabled:opacity-50"
          >
            {loading ? 'Processing...' : isRegister ? 'Sign Up' : 'Sign In'}
          </button>
        </form>

        <div className="flex items-center justify-between text-sm">
          <button
            onClick={() => setIsRegister(!isRegister)}
            className="text-primary dark:text-indigo-400 hover:underline font-medium"
          >
            {isRegister ? 'Already have an account? Sign In' : 'New to Upgrade? Create Account'}
          </button>
        </div>

        <div className="relative flex py-2 items-center">
          <div className="flex-grow border-t border-slate-200 dark:border-slate-800"></div>
          <span className="flex-shrink mx-4 text-slate-400 text-xs uppercase font-bold tracking-wider">Or</span>
          <div className="flex-grow border-t border-slate-200 dark:border-slate-800"></div>
        </div>

        <div className="space-y-3">
          <button
            onClick={handleLocalMode}
            className="w-full py-2.5 border border-slate-200 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-900 rounded-lg font-semibold transition-all text-sm flex items-center justify-center space-x-2"
          >
            <Sparkles className="h-4 w-4 text-indigo-400" />
            <span>Use Anonymous Local Mode</span>
          </button>
          <p className="text-center text-[10px] text-slate-400 max-w-xs mx-auto">
            Offline-first: Data is stored securely in your browser storage. You can register and sync to the cloud anytime.
          </p>
        </div>
      </div>
    </div>
  );
}
