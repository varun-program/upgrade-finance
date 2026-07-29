import React, { useState, useEffect } from 'react';
import { dataService } from '../utils/api';
import { AlertCircle, Plus, Trash2, PiggyBank } from 'lucide-react';

export default function Budgets() {
  const [budgets, setBudgets] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);

  // New Budget Form
  const [category, setCategory] = useState('Food');
  const [limit, setLimit] = useState('');

  const categories = ['Food', 'Travel', 'Shopping', 'Fuel', 'Entertainment', 'Healthcare', 'Bills', 'Education', 'Savings', 'Other'];

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    const bData = await dataService.getBudgets();
    const tData = await dataService.getTransactions();
    setBudgets(bData);
    setTransactions(tData);
    setLoading(false);
  };

  const handleSaveBudget = async (e) => {
    e.preventDefault();
    if (!limit || isNaN(limit)) return;

    const existing = budgets.find(b => b.category.toLowerCase() === category.toLowerCase());
    const bgt = {
      id: existing ? existing.id : undefined,
      category,
      limitAmount: parseFloat(limit),
      period: 'MONTHLY'
    };

    await dataService.saveBudget(bgt);
    setLimit('');
    loadData();
  };

  const handleDelete = async (id) => {
    await dataService.deleteBudget(id);
    loadData();
  };

  // Calculate current month's spent for each category
  const startOfMonth = new Date(new Date().getFullYear(), new Date().getMonth(), 1).getTime();
  
  const getSpent = (cat) => {
    return transactions
      .filter(t => t.timestamp >= startOfMonth && 
                   t.transactionType === 'DEBIT' && 
                   t.category?.toLowerCase() === cat.toLowerCase())
      .reduce((sum, t) => sum + t.amount, 0);
  };

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-4xl font-extrabold tracking-tight">Category Budgets</h1>
        <p className="text-slate-500 dark:text-slate-400 mt-1">Set monthly limits to keep your spending in check.</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        
        {/* Set Budget Form */}
        <div className="p-6 glass rounded-2xl border shadow-sm space-y-4 h-fit">
          <h3 className="text-lg font-bold">Configure Budget Limit</h3>
          <form onSubmit={handleSaveBudget} className="space-y-4">
            <div className="space-y-1">
              <label className="text-xs font-semibold uppercase text-slate-500">Category</label>
              <select
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                className="w-full px-3 py-2 rounded-lg border border-slate-200 dark:border-slate-800 bg-white/50 dark:bg-slate-900/50 outline-none text-sm focus:ring-2 focus:ring-primary"
              >
                {categories.map(cat => <option key={cat} value={cat}>{cat}</option>)}
              </select>
            </div>
            
            <div className="space-y-1">
              <label className="text-xs font-semibold uppercase text-slate-500">Monthly Limit (₹)</label>
              <input
                type="number"
                required
                value={limit}
                onChange={(e) => setLimit(e.target.value)}
                className="w-full px-3 py-2 rounded-lg border border-slate-200 dark:border-slate-800 bg-white/50 dark:bg-slate-900/50 outline-none text-sm focus:ring-2 focus:ring-primary"
                placeholder="e.g. 5000"
              />
            </div>

            <button
              type="submit"
              className="w-full py-2 bg-primary hover:bg-primary-hover text-white rounded-lg font-semibold text-sm shadow transition-all"
            >
              Set Budget
            </button>
          </form>
        </div>

        {/* Budgets List & Progress bars */}
        <div className="lg:col-span-2 space-y-4">
          <h2 className="text-2xl font-bold tracking-tight">Active Limits</h2>

          {loading ? (
            <div className="text-slate-400">Loading budgets...</div>
          ) : budgets.length === 0 ? (
            <div className="glass p-8 text-center text-slate-400 rounded-2xl border">
              No budgets configured. Use the form on the left to set up limits!
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {budgets.map((b) => {
                const spent = getSpent(b.category);
                const pct = (spent / b.limitAmount) * 100;
                
                let barColor = 'bg-primary';
                let alertMsg = '';
                
                if (pct >= 100) {
                  barColor = 'bg-red-500';
                  alertMsg = 'Exceeded!';
                } else if (pct >= 80) {
                  barColor = 'bg-yellow-500';
                  alertMsg = '80% Exceeded!';
                } else if (pct >= 50) {
                  barColor = 'bg-indigo-400';
                  alertMsg = '50% Used';
                }

                return (
                  <div key={b.id} className="p-5 glass rounded-2xl border shadow-sm space-y-3 relative group">
                    <div className="flex justify-between items-start">
                      <div>
                        <h4 className="font-bold text-sm">{b.category}</h4>
                        <div className="text-xs text-slate-400 mt-0.5">
                          ₹{spent.toLocaleString('en-IN')} of ₹{b.limitAmount.toLocaleString('en-IN')}
                        </div>
                      </div>
                      
                      <div className="flex items-center space-x-2">
                        {alertMsg && (
                          <span className={`text-[10px] font-bold px-2 py-0.5 rounded flex items-center space-x-0.5 ${
                            pct >= 100 ? 'bg-red-500/10 text-red-500' : 'bg-yellow-500/10 text-yellow-500'
                          }`}>
                            <AlertCircle className="h-3 w-3" />
                            <span>{alertMsg}</span>
                          </span>
                        )}
                        <button
                          onClick={() => handleDelete(b.id)}
                          className="opacity-0 group-hover:opacity-100 p-1 hover:text-red-500 transition-opacity"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    </div>

                    <div className="w-full bg-slate-200 dark:bg-slate-800 h-2.5 rounded-full overflow-hidden">
                      <div 
                        className={`h-full rounded-full transition-all duration-500 ${barColor}`} 
                        style={{ width: `${Math.min(pct, 100)}%` }}
                      />
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
