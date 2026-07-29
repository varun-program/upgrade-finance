import React, { useState, useEffect } from 'react';
import { dataService } from '../utils/api';
import { Trash2, AlertCircle, Plus } from 'lucide-react';

export default function Rules() {
  const [rules, setRules] = useState([]);
  const [loading, setLoading] = useState(true);

  // New Rule Form
  const [pattern, setPattern] = useState('');
  const [category, setCategory] = useState('Food');

  const categories = ['Food', 'Travel', 'Shopping', 'Fuel', 'Entertainment', 'Healthcare', 'Bills', 'Education', 'Savings', 'Other'];

  useEffect(() => {
    loadRules();
  }, []);

  const loadRules = async () => {
    setLoading(true);
    const data = await dataService.getSmartRules();
    setRules(data);
    setLoading(false);
  };

  const handleSaveRule = async (e) => {
    e.preventDefault();
    if (!pattern.trim()) return;

    const newRule = {
      pattern: pattern.trim(),
      category
    };

    await dataService.saveSmartRule(newRule);
    setPattern('');
    loadRules();
  };

  const handleDelete = async (id) => {
    await dataService.deleteSmartRule(id);
    loadRules();
  };

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-4xl font-extrabold tracking-tight">Automation Rules</h1>
        <p className="text-slate-500 dark:text-slate-400 mt-1">
          Create rules to auto-categorize incoming transactions when matched against description keywords.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        
        {/* Set Rule Form */}
        <div className="p-6 glass rounded-2xl border shadow-sm space-y-4 h-fit">
          <h3 className="text-lg font-bold">New Smart Rule</h3>
          <form onSubmit={handleSaveRule} className="space-y-4">
            <div className="space-y-1">
              <label className="text-xs font-semibold uppercase text-slate-500">If merchant contains (keyword)</label>
              <input
                type="text"
                required
                value={pattern}
                onChange={(e) => setPattern(e.target.value)}
                className="w-full px-3 py-2 rounded-lg border border-slate-200 dark:border-slate-800 bg-white/50 dark:bg-slate-900/50 outline-none text-sm focus:ring-2 focus:ring-primary"
                placeholder="e.g. Swiggy, Zomato"
              />
            </div>
            
            <div className="space-y-1">
              <label className="text-xs font-semibold uppercase text-slate-500">Set Category to</label>
              <select
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                className="w-full px-3 py-2 rounded-lg border border-slate-200 dark:border-slate-800 bg-white/50 dark:bg-slate-900/50 outline-none text-sm focus:ring-2 focus:ring-primary"
              >
                {categories.map(cat => <option key={cat} value={cat}>{cat}</option>)}
              </select>
            </div>

            <button
              type="submit"
              className="w-full py-2 bg-primary hover:bg-primary-hover text-white rounded-lg font-semibold text-sm shadow transition-all flex items-center justify-center space-x-1"
            >
              <Plus className="h-4 w-4" />
              <span>Add Rule</span>
            </button>
          </form>
        </div>

        {/* Rules List */}
        <div className="lg:col-span-2 space-y-4">
          <h2 className="text-2xl font-bold tracking-tight">Active Rules</h2>

          {loading ? (
            <div className="text-slate-400">Loading rules...</div>
          ) : rules.length === 0 ? (
            <div className="glass p-8 text-center text-slate-400 rounded-2xl border flex flex-col items-center space-y-2">
              <AlertCircle className="h-8 w-8 text-slate-300" />
              <span>No custom automation rules defined yet.</span>
            </div>
          ) : (
            <div className="glass rounded-2xl border shadow-sm overflow-hidden divide-y divide-slate-100 dark:divide-slate-800/50">
              {rules.map((r) => (
                <div key={r.id} className="flex justify-between items-center p-4 hover:bg-slate-50/50 dark:hover:bg-slate-900/10">
                  <div className="flex items-center space-x-3">
                    <span className="text-sm font-semibold">
                      If Merchant contains <span className="underline decoration-indigo-400 font-bold">"{r.pattern}"</span>
                    </span>
                    <span className="text-xs text-slate-400">→</span>
                    <span className="text-xs font-semibold px-2 py-0.5 bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 rounded">
                      {r.category}
                    </span>
                  </div>
                  
                  <button
                    onClick={() => handleDelete(r.id)}
                    className="text-slate-400 hover:text-red-500 transition-colors"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
