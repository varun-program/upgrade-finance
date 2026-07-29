import React, { useState, useEffect } from 'react';
import { dataService } from '../utils/api';
import { PiggyBank, Plus, Trash2, ArrowUpRight } from 'lucide-react';

export default function Savings() {
  const [goals, setGoals] = useState([]);
  const [loading, setLoading] = useState(true);

  // New Goal Form
  const [name, setName] = useState('');
  const [target, setTarget] = useState('');
  const [saved, setSaved] = useState('');

  // Deposit Form
  const [depositAmount, setDepositAmount] = useState({});

  useEffect(() => {
    loadGoals();
  }, []);

  const loadGoals = async () => {
    setLoading(true);
    const data = await dataService.getSavingsGoals();
    setGoals(data);
    setLoading(false);
  };

  const handleSaveGoal = async (e) => {
    e.preventDefault();
    if (!name || !target || isNaN(target)) return;

    const goal = {
      name,
      targetAmount: parseFloat(target),
      savedAmount: saved ? parseFloat(saved) : 0,
      targetDate: Date.now() + 180 * 24 * 60 * 60 * 1000 // default 6 months estimate
    };

    await dataService.saveSavingsGoal(goal);
    setName('');
    setTarget('');
    setSaved('');
    loadGoals();
  };

  const handleAddSavings = async (goal, amountToAdd) => {
    if (!amountToAdd || isNaN(amountToAdd)) return;
    const updated = {
      ...goal,
      savedAmount: goal.savedAmount + parseFloat(amountToAdd)
    };
    await dataService.saveSavingsGoal(updated);
    // Clear input
    setDepositAmount(prev => ({ ...prev, [goal.id]: '' }));
    loadGoals();
  };

  const handleDelete = async (id) => {
    await dataService.deleteSavingsGoal(id);
    loadGoals();
  };

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-4xl font-extrabold tracking-tight">Savings Goals</h1>
        <p className="text-slate-500 dark:text-slate-400 mt-1">Plan for your future milestones with structured saving goals.</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        
        {/* Set Savings Goal Form */}
        <div className="p-6 glass rounded-2xl border shadow-sm space-y-4 h-fit">
          <h3 className="text-lg font-bold">New Savings Goal</h3>
          <form onSubmit={handleSaveGoal} className="space-y-4">
            <div className="space-y-1">
              <label className="text-xs font-semibold uppercase text-slate-500">Goal Name</label>
              <input
                type="text"
                required
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="w-full px-3 py-2 rounded-lg border border-slate-200 dark:border-slate-800 bg-white/50 dark:bg-slate-900/50 outline-none text-sm focus:ring-2 focus:ring-primary"
                placeholder="e.g. New Laptop"
              />
            </div>
            
            <div className="space-y-1">
              <label className="text-xs font-semibold uppercase text-slate-500">Target Amount (₹)</label>
              <input
                type="number"
                required
                value={target}
                onChange={(e) => setTarget(e.target.value)}
                className="w-full px-3 py-2 rounded-lg border border-slate-200 dark:border-slate-800 bg-white/50 dark:bg-slate-900/50 outline-none text-sm focus:ring-2 focus:ring-primary"
                placeholder="e.g. 120000"
              />
            </div>

            <div className="space-y-1">
              <label className="text-xs font-semibold uppercase text-slate-500">Initial Savings (₹)</label>
              <input
                type="number"
                value={saved}
                onChange={(e) => setSaved(e.target.value)}
                className="w-full px-3 py-2 rounded-lg border border-slate-200 dark:border-slate-800 bg-white/50 dark:bg-slate-900/50 outline-none text-sm focus:ring-2 focus:ring-primary"
                placeholder="0.00"
              />
            </div>

            <button
              type="submit"
              className="w-full py-2 bg-primary hover:bg-primary-hover text-white rounded-lg font-semibold text-sm shadow transition-all"
            >
              Create Goal
            </button>
          </form>
        </div>

        {/* Goals List */}
        <div className="lg:col-span-2 space-y-4">
          <h2 className="text-2xl font-bold tracking-tight">Active Savings Targets</h2>

          {loading ? (
            <div className="text-slate-400">Loading savings goals...</div>
          ) : goals.length === 0 ? (
            <div className="glass p-8 text-center text-slate-400 rounded-2xl border">
              No savings goals set. Define a target on the left to start saving!
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {goals.map((g) => {
                const pct = (g.savedAmount / g.targetAmount) * 100;
                const remaining = g.targetAmount - g.savedAmount;

                return (
                  <div key={g.id} className="p-6 glass rounded-2xl border shadow-sm space-y-4 relative group">
                    <div className="flex justify-between items-start">
                      <div className="flex items-center space-x-2">
                        <PiggyBank className="h-5 w-5 text-indigo-400" />
                        <h4 className="font-bold text-sm">{g.name}</h4>
                      </div>
                      <button
                        onClick={() => handleDelete(g.id)}
                        className="opacity-0 group-hover:opacity-100 text-slate-400 hover:text-red-500 transition-opacity"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>

                    <div className="space-y-1">
                      <div className="flex justify-between text-xs font-semibold">
                        <span>Progress: {Math.round(pct)}%</span>
                        <span>₹{g.savedAmount.toLocaleString('en-IN')} / ₹{g.targetAmount.toLocaleString('en-IN')}</span>
                      </div>
                      <div className="w-full bg-slate-200 dark:bg-slate-800 h-2.5 rounded-full overflow-hidden">
                        <div 
                          className="h-full rounded-full bg-indigo-500 transition-all duration-500" 
                          style={{ width: `${Math.min(pct, 100)}%` }}
                        />
                      </div>
                    </div>

                    <div className="text-[10px] text-slate-400 flex justify-between">
                      <span>Remaining: ₹{remaining.toLocaleString('en-IN')}</span>
                      <span>ETA: ~6 months</span>
                    </div>

                    {/* Contribute input */}
                    <div className="flex space-x-2 pt-2 border-t border-slate-100 dark:border-slate-800/50">
                      <input
                        type="number"
                        value={depositAmount[g.id] || ''}
                        onChange={(e) => setDepositAmount(prev => ({ ...prev, [g.id]: e.target.value }))}
                        className="flex-grow px-2.5 py-1 text-xs rounded-lg border border-slate-200 dark:border-slate-800 bg-white/40 dark:bg-slate-900/40 outline-none"
                        placeholder="Add savings ₹"
                      />
                      <button
                        onClick={() => handleAddSavings(g, depositAmount[g.id])}
                        className="p-1.5 bg-slate-100 dark:bg-slate-800 hover:bg-primary hover:text-white rounded-lg text-xs font-bold transition-all flex items-center"
                      >
                        <ArrowUpRight className="h-3.5 w-3.5" />
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
