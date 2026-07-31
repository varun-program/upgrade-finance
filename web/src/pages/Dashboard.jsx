import React, { useState, useEffect } from 'react';
import { dataService, isLocalMode } from '../utils/api';
import { Plus, Search, Calendar, Landmark, CreditCard, ChevronDown, Check, TrendingDown, TrendingUp, Sparkles, Filter } from 'lucide-react';

export default function Dashboard() {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [filterType, setFilterType] = useState('ALL');
  
  // Quick Add Form States
  const [showAddForm, setShowAddForm] = useState(false);
  const [amount, setAmount] = useState('');
  const [merchant, setMerchant] = useState('');
  const [category, setCategory] = useState('Food');
  const [bank, setBank] = useState('SBI');
  const [txType, setTxType] = useState('DEBIT');
  
  // Edit & Delete All States
  const [editingId, setEditingId] = useState(null);
  const [editMerchant, setEditMerchant] = useState('');

  // Bank Balances States
  const [startingBalances, setStartingBalances] = useState(() => {
    const saved = localStorage.getItem('starting_balances');
    return saved ? JSON.parse(saved) : { 'Kotak Bank': 10000 };
  });
  const [editingBankName, setEditingBankName] = useState(null);
  const [editBalanceVal, setEditBalanceVal] = useState('');

  // Add Bank Form States
  const [showAddBank, setShowAddBank] = useState(false);
  const [newBankName, setNewBankName] = useState('');
  const [newBankSuffix, setNewBankSuffix] = useState('');
  const [newBankStart, setNewBankStart] = useState('');
  
  useEffect(() => {
    loadTransactions();
  }, []);

  const loadTransactions = async () => {
    setLoading(true);
    const data = await dataService.getTransactions();
    // Sort transactions by timestamp desc
    data.sort((a, b) => b.timestamp - a.timestamp);
    setTransactions(data);
    setLoading(false);
  };

  const handleAddTransaction = async (e) => {
    e.preventDefault();
    if (!amount || isNaN(amount)) return;

    const newTx = {
      amount: parseFloat(amount),
      merchant,
      category,
      bank,
      transactionType: txType,
      timestamp: Date.now(),
      upiId: txType === 'DEBIT' ? `${merchant.toLowerCase().replace(/\s+/g, '')}@okaxis` : null,
      referenceNumber: Math.floor(100000000000 + Math.random() * 900000000000).toString(),
    };

    await dataService.saveTransaction(newTx);
    setAmount('');
    setMerchant('');
    setShowAddForm(false);
    loadTransactions();
  };

  const handleDelete = async (id) => {
    await dataService.deleteTransaction(id);
    loadTransactions();
  };

  const handleDeleteAll = async () => {
    if (window.confirm("Are you sure you want to delete all transactions from your dashboard?")) {
      await dataService.deleteAllTransactions();
      loadTransactions();
    }
  };

  const handleEditStart = (tx) => {
    setEditingId(tx.id);
    setEditMerchant(tx.merchant || '');
  };

  const handleEditSave = async (id) => {
    if (!editMerchant.trim()) return;
    const originalTx = transactions.find(t => t.id === id);
    if (!originalTx) return;

    const updatedTx = { ...originalTx, merchant: editMerchant };
    await dataService.updateTransaction(id, updatedTx);
    setEditingId(null);
    loadTransactions();
  };

  const handleEditCancel = () => {
    setEditingId(null);
  };

  const handleSaveBankBalance = (bankName) => {
    const parsed = parseFloat(editBalanceVal);
    if (isNaN(parsed)) return;
    const updated = { ...startingBalances, [bankName]: parsed };
    setStartingBalances(updated);
    localStorage.setItem('starting_balances', JSON.stringify(updated));
    setEditingBankName(null);
  };

  const handleAddBank = (e) => {
    e.preventDefault();
    if (!newBankName.trim() || isNaN(newBankStart) || !newBankStart.trim()) return;
    
    const bankKey = newBankName.trim();
    const updated = { ...startingBalances, [bankKey]: parseFloat(newBankStart) };
    setStartingBalances(updated);
    localStorage.setItem('starting_balances', JSON.stringify(updated));
    
    const suffixes = JSON.parse(localStorage.getItem('bank_suffixes') || '{}');
    suffixes[bankKey] = newBankSuffix.trim() || 'XXXX';
    localStorage.setItem('bank_suffixes', JSON.stringify(suffixes));
    
    setNewBankName('');
    setNewBankSuffix('');
    setNewBankStart('');
    setShowAddBank(false);
  };

  const handleDeleteBank = (bankName) => {
    if (window.confirm(`Are you sure you want to remove ${bankName} from your accounts list?`)) {
      const updated = { ...startingBalances };
      delete updated[bankName];
      setStartingBalances(updated);
      localStorage.setItem('starting_balances', JSON.stringify(updated));
      
      const suffixes = JSON.parse(localStorage.getItem('bank_suffixes') || '{}');
      delete suffixes[bankName];
      localStorage.setItem('bank_suffixes', JSON.stringify(suffixes));
    }
  };

  // Metrics Calculations
  const now = new Date();
  const startOfDay = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1).getTime();

  const todaySpend = transactions
    .filter(t => t.timestamp >= startOfDay && t.transactionType === 'DEBIT')
    .reduce((sum, t) => sum + t.amount, 0);

  const monthSpend = transactions
    .filter(t => t.timestamp >= startOfMonth && t.transactionType === 'DEBIT')
    .reduce((sum, t) => sum + t.amount, 0);

  const monthIncome = transactions
    .filter(t => t.timestamp >= startOfMonth && t.transactionType === 'CREDIT')
    .reduce((sum, t) => sum + t.amount, 0);

  const filteredTransactions = transactions.filter(t => {
    const matchesSearch = 
      t.merchant?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      t.category?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      t.bank?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      t.amount.toString().includes(searchQuery);
    
    if (filterType === 'DEBIT') return matchesSearch && t.transactionType === 'DEBIT';
    if (filterType === 'CREDIT') return matchesSearch && t.transactionType === 'CREDIT';
    return matchesSearch;
  });

  // Group and compute active bank accounts dynamically
  const bankAccounts = React.useMemo(() => {
    const accountsList = [];
    const suffixes = JSON.parse(localStorage.getItem('bank_suffixes') || '{}');

    Object.keys(startingBalances).forEach(bankName => {
      const netChange = transactions
        .filter(tx => {
          const txBank = tx.bank || 'Unknown Bank';
          if (bankName === 'Kotak Bank' && txBank.toUpperCase().includes('KOTAK')) return true;
          return txBank.toUpperCase() === bankName.toUpperCase();
        })
        .reduce((sum, tx) => {
          return sum + (tx.transactionType === 'DEBIT' ? -tx.amount : tx.amount);
        }, 0);

      const suffix = suffixes[bankName] || (bankName === 'Kotak Bank' ? '7215' : 'XXXX');
      accountsList.push({
        name: bankName,
        suffix: suffix,
        balance: startingBalances[bankName] + netChange,
      });
    });

    return accountsList;
  }, [transactions, startingBalances]);

  const categories = ['Food', 'Travel', 'Shopping', 'Fuel', 'Entertainment', 'Healthcare', 'Bills', 'Education', 'Savings', 'Other'];

  return (
    <div className="space-y-8 animate-fade-in">
      {/* Header Info */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h1 className="text-4xl font-extrabold tracking-tight">Finances Overview</h1>
          <p className="text-slate-500 dark:text-slate-400 mt-1">
            {isLocalMode() ? '✨ Running in Local Mode (Offline-first)' : '☁️ Connected & Synchronized'}
          </p>
        </div>
        <button
          onClick={() => setShowAddForm(!showAddForm)}
          className="flex items-center space-x-2 px-4 py-2.5 bg-primary hover:bg-primary-hover text-white font-semibold rounded-lg shadow-md transition-all text-sm"
        >
          <Plus className="h-4 w-4" />
          <span>Quick Add Transaction</span>
        </button>
      </div>

      {/* Quick Add Form Container */}
      {showAddForm && (
        <form onSubmit={handleAddTransaction} className="p-6 glass rounded-2xl shadow-lg border border-primary/20 space-y-4 max-w-xl animate-slide-down">
          <h3 className="text-lg font-bold">Add New Transaction</h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-1">
              <label className="text-xs font-semibold uppercase text-slate-500">Amount (₹)</label>
              <input
                type="number"
                step="0.01"
                required
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                className="w-full px-3 py-2 rounded-lg border border-slate-200 dark:border-slate-800 bg-white/50 dark:bg-slate-900/50 outline-none text-sm focus:ring-2 focus:ring-primary"
                placeholder="0.00"
              />
            </div>
            <div className="space-y-1">
              <label className="text-xs font-semibold uppercase text-slate-500">Merchant / Description</label>
              <input
                type="text"
                required
                value={merchant}
                onChange={(e) => setMerchant(e.target.value)}
                className="w-full px-3 py-2 rounded-lg border border-slate-200 dark:border-slate-800 bg-white/50 dark:bg-slate-900/50 outline-none text-sm focus:ring-2 focus:ring-primary"
                placeholder="e.g. Swiggy"
              />
            </div>
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
              <label className="text-xs font-semibold uppercase text-slate-500">Bank Account</label>
              <input
                type="text"
                value={bank}
                onChange={(e) => setBank(e.target.value)}
                className="w-full px-3 py-2 rounded-lg border border-slate-200 dark:border-slate-800 bg-white/50 dark:bg-slate-900/50 outline-none text-sm focus:ring-2 focus:ring-primary"
                placeholder="e.g. HDFC 4522"
              />
            </div>
            <div className="space-y-1">
              <label className="text-xs font-semibold uppercase text-slate-500">Type</label>
              <div className="flex space-x-2">
                <button
                  type="button"
                  onClick={() => setTxType('DEBIT')}
                  className={`flex-1 py-1.5 rounded-lg text-xs font-bold border transition-all ${
                    txType === 'DEBIT' 
                      ? 'bg-red-500/10 border-red-500 text-red-500' 
                      : 'border-slate-200 dark:border-slate-800 text-slate-500'
                  }`}
                >
                  DEBIT
                </button>
                <button
                  type="button"
                  onClick={() => setTxType('CREDIT')}
                  className={`flex-1 py-1.5 rounded-lg text-xs font-bold border transition-all ${
                    txType === 'CREDIT' 
                      ? 'bg-green-500/10 border-green-500 text-green-500' 
                      : 'border-slate-200 dark:border-slate-800 text-slate-500'
                  }`}
                >
                  CREDIT
                </button>
              </div>
            </div>
          </div>
          <div className="flex justify-end space-x-2 pt-2">
            <button
              type="button"
              onClick={() => setShowAddForm(false)}
              className="px-4 py-2 border border-slate-200 dark:border-slate-800 text-slate-600 dark:text-slate-400 rounded-lg text-sm"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 bg-primary text-white rounded-lg text-sm font-semibold shadow"
            >
              Save
            </button>
          </div>
        </form>
      )}

      {/* Metrics Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="p-6 glass rounded-2xl shadow-sm space-y-2 border border-slate-100 dark:border-slate-900/50">
          <div className="flex justify-between items-center text-slate-500 dark:text-slate-400 text-xs font-bold uppercase tracking-wider">
            <span>Today's Spending</span>
            <TrendingDown className="h-4 w-4 text-red-500" />
          </div>
          <div className="text-3xl font-extrabold tracking-tight">₹{todaySpend.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</div>
        </div>

        <div className="p-6 glass rounded-2xl shadow-sm space-y-2 border border-slate-100 dark:border-slate-900/50">
          <div className="flex justify-between items-center text-slate-500 dark:text-slate-400 text-xs font-bold uppercase tracking-wider">
            <span>This Month's Spending</span>
            <TrendingDown className="h-4 w-4 text-red-500" />
          </div>
          <div className="text-3xl font-extrabold tracking-tight">₹{monthSpend.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</div>
        </div>

        <div className="p-6 glass rounded-2xl shadow-sm space-y-2 border border-slate-100 dark:border-slate-900/50">
          <div className="flex justify-between items-center text-slate-500 dark:text-slate-400 text-xs font-bold uppercase tracking-wider">
            <span>This Month's Income</span>
            <TrendingUp className="h-4 w-4 text-green-500" />
          </div>
          <div className="text-3xl font-extrabold tracking-tight">₹{monthIncome.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</div>
        </div>

        <div className="p-6 glass rounded-2xl shadow-sm space-y-2 border border-slate-100 dark:border-slate-900/50">
          <div className="flex justify-between items-center text-slate-500 dark:text-slate-400 text-xs font-bold uppercase tracking-wider">
            <span>Net Balance Delta</span>
            <span className="text-xs font-semibold px-2 py-0.5 rounded bg-indigo-500/10 text-indigo-400">Monthly</span>
          </div>
          <div className="text-3xl font-extrabold tracking-tight">
            ₹{(monthIncome - monthSpend).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
          </div>
        </div>
      </div>

      {/* Main Layout Area */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        
        {/* Transactions list */}
        <div className="lg:col-span-2 space-y-4">
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
            <div className="flex items-center space-x-4">
              <h2 className="text-2xl font-bold tracking-tight">Transactions Log</h2>
              {transactions.length > 0 && (
                <button
                  onClick={handleDeleteAll}
                  className="text-xs px-2.5 py-1.5 rounded-lg bg-red-500/10 hover:bg-red-500/20 text-red-500 font-semibold transition-colors"
                >
                  Delete All
                </button>
              )}
            </div>
            <div className="flex items-center space-x-2 w-full sm:w-auto">
              <div className="relative flex-grow sm:flex-grow-0">
                <Search className="absolute inset-y-0 left-0 pl-3 h-full w-4 text-slate-400" />
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-9 pr-4 py-1.5 w-full sm:w-60 rounded-lg border border-slate-200 dark:border-slate-800 bg-white/40 dark:bg-slate-900/40 text-xs focus:ring-2 focus:ring-primary outline-none"
                  placeholder="Search merchant, bank..."
                />
              </div>
              <select
                value={filterType}
                onChange={(e) => setFilterType(e.target.value)}
                className="px-3 py-1.5 rounded-lg border border-slate-200 dark:border-slate-800 bg-white/40 dark:bg-slate-900/40 text-xs focus:ring-2 focus:ring-primary outline-none font-semibold"
              >
                <option value="ALL">All</option>
                <option value="DEBIT">Debit</option>
                <option value="CREDIT">Credit</option>
              </select>
            </div>
          </div>

          <div className="glass rounded-2xl shadow-sm overflow-hidden border border-slate-100 dark:border-slate-900/50">
            {loading ? (
              <div className="p-8 text-center text-slate-400">Loading your transactions...</div>
            ) : filteredTransactions.length === 0 ? (
              <div className="p-12 text-center text-slate-400 space-y-2">
                <div className="text-4xl">🧾</div>
                <div className="font-semibold text-sm">No transactions matched filters</div>
              </div>
            ) : (
              <div className="divide-y divide-slate-100 dark:divide-slate-800/50 max-h-[500px] overflow-y-auto no-scrollbar">
                {filteredTransactions.map((tx) => (
                  <div key={tx.id} className="flex justify-between items-center p-4 hover:bg-slate-50/50 dark:hover:bg-slate-900/10 transition-colors">
                    <div className="flex items-center space-x-3">
                      <div className={`p-2.5 rounded-xl ${tx.transactionType === 'DEBIT' ? 'bg-red-500/10 text-red-500' : 'bg-green-500/10 text-green-500'}`}>
                        {tx.transactionType === 'DEBIT' ? <TrendingDown className="h-4 w-4" /> : <TrendingUp className="h-4 w-4" />}
                      </div>
                      <div>
                        {editingId === tx.id ? (
                          <div className="flex items-center space-x-2">
                            <input
                              type="text"
                              value={editMerchant}
                              onChange={(e) => setEditMerchant(e.target.value)}
                              className="text-xs px-2 py-0.5 rounded border border-indigo-400 bg-white dark:bg-slate-800 text-slate-800 dark:text-slate-100 focus:outline-none"
                            />
                            <button
                              onClick={() => handleEditSave(tx.id)}
                              className="p-1 text-green-500 hover:text-green-600"
                              title="Save"
                            >
                              <Check className="h-3.5 w-3.5" />
                            </button>
                            <button
                              onClick={handleEditCancel}
                              className="p-1 text-red-400 hover:text-red-500 text-xs font-bold"
                              title="Cancel"
                            >
                              ✕
                            </button>
                          </div>
                        ) : (
                          <div className="flex items-center space-x-2">
                            <div className="font-semibold text-sm">{tx.merchant || 'Unknown Merchant'}</div>
                            <button
                              onClick={() => handleEditStart(tx)}
                              className="text-[10px] opacity-60 hover:opacity-100 transition-opacity"
                              title="Rename spend description"
                            >
                              ✏️
                            </button>
                          </div>
                        )}
                        <div className="flex items-center space-x-2 text-[10px] text-slate-400 mt-0.5">
                          <span className="px-1.5 py-0.5 bg-slate-100 dark:bg-slate-800 text-slate-500 rounded font-medium">{tx.category}</span>
                          <span>•</span>
                          <span className="flex items-center space-x-0.5">
                            <Landmark className="h-3 w-3" />
                            <span>{tx.bank}</span>
                          </span>
                          <span>•</span>
                          <span>{new Date(tx.timestamp).toLocaleDateString()}</span>
                        </div>
                      </div>
                    </div>
                    <div className="flex items-center space-x-4">
                      <div className={`font-bold text-sm ${tx.transactionType === 'DEBIT' ? 'text-red-500' : 'text-green-500'}`}>
                        {tx.transactionType === 'DEBIT' ? '-' : '+'}₹{tx.amount.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                      </div>
                      <button
                        onClick={() => handleDelete(tx.id)}
                        className="text-xs text-red-400 hover:text-red-600 transition-colors"
                      >
                        Delete
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Sidebar Info Panels */}
        <div className="space-y-6">
          <h2 className="text-2xl font-bold tracking-tight">My Accounts</h2>
          <div className="glass p-6 rounded-2xl shadow-sm space-y-4 border border-slate-100 dark:border-slate-900/50">
            {bankAccounts.map((acc) => (
              <div key={acc.name} className="flex items-center justify-between p-3.5 rounded-xl bg-slate-50 dark:bg-slate-900/40 border border-slate-100 dark:border-slate-800/50">
                <div className="flex items-center space-x-3">
                  <div className={`p-2 rounded-lg ${acc.name.includes('Kotak') ? 'bg-red-500/10 text-red-400' : 'bg-indigo-500/10 text-indigo-400'}`}>
                    <CreditCard className="h-5 w-5" />
                  </div>
                  <div>
                    <div className="font-semibold text-sm">{acc.name}</div>
                    <div className="text-[10px] text-slate-400 mt-0.5">Suffix *{acc.suffix}</div>
                  </div>
                </div>
                <div className="text-right">
                  {editingBankName === acc.name ? (
                    <div className="flex items-center space-x-1.5 mt-1">
                      <input
                        type="number"
                        value={editBalanceVal}
                        onChange={(e) => setEditBalanceVal(e.target.value)}
                        className="w-20 text-xs px-1.5 py-0.5 rounded border border-slate-300 bg-white dark:bg-slate-800 text-slate-800 dark:text-slate-100 focus:outline-none"
                        placeholder="Start val"
                      />
                      <button
                        onClick={() => handleSaveBankBalance(acc.name)}
                        className="text-xs text-green-500 hover:text-green-600 font-bold"
                      >
                        ✓
                      </button>
                      <button
                        onClick={() => setEditingBankName(null)}
                        className="text-xs text-slate-400 hover:text-slate-500"
                      >
                        ✕
                      </button>
                    </div>
                  ) : (
                    <div className="flex items-center justify-end space-x-1">
                      <div className="font-bold text-sm">₹{acc.balance.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</div>
                      <button
                        onClick={() => {
                          setEditingBankName(acc.name);
                          setEditBalanceVal((startingBalances[acc.name] !== undefined ? startingBalances[acc.name] : 10000).toString());
                        }}
                        className="text-[10px] opacity-40 hover:opacity-100 transition-opacity"
                        title="Set starting balance"
                      >
                        ✏️
                      </button>
                      <button
                        onClick={() => handleDeleteBank(acc.name)}
                        className="text-[10px] text-red-400 hover:text-red-600 ml-1.5 font-bold"
                        title="Remove bank account"
                      >
                        ✕
                      </button>
                    </div>
                  )}
                </div>
              </div>
            ))}
            
            {/* Add Bank Account trigger */}
            {!showAddBank ? (
              <button
                onClick={() => setShowAddBank(true)}
                className="w-full py-2 border border-dashed border-slate-200 dark:border-slate-800 hover:border-indigo-400 text-xs text-slate-500 hover:text-indigo-500 rounded-xl transition-colors font-medium"
              >
                + Add Bank Account
              </button>
            ) : (
              <form onSubmit={handleAddBank} className="p-3 bg-slate-50 dark:bg-slate-900/40 rounded-xl border border-slate-100 dark:border-slate-800/50 space-y-2">
                <input
                  type="text"
                  placeholder="Bank Name (e.g. SBI)"
                  value={newBankName}
                  onChange={(e) => setNewBankName(e.target.value)}
                  className="w-full text-xs px-2 py-1 rounded border border-slate-200 bg-white dark:bg-slate-800 text-slate-800 dark:text-slate-100 focus:outline-none"
                  required
                />
                <div className="flex gap-2">
                  <input
                    type="text"
                    placeholder="Suffix (e.g. 9081)"
                    value={newBankSuffix}
                    onChange={(e) => setNewBankSuffix(e.target.value)}
                    className="w-1/2 text-xs px-2 py-1 rounded border border-slate-200 bg-white dark:bg-slate-800 text-slate-800 dark:text-slate-100 focus:outline-none"
                  />
                  <input
                    type="number"
                    placeholder="Starting Bal"
                    value={newBankStart}
                    onChange={(e) => setNewBankStart(e.target.value)}
                    className="w-1/2 text-xs px-2 py-1 rounded border border-slate-200 bg-white dark:bg-slate-800 text-slate-800 dark:text-slate-100 focus:outline-none"
                    required
                  />
                </div>
                <div className="flex justify-end space-x-2 pt-1">
                  <button
                    type="button"
                    onClick={() => setShowAddBank(false)}
                    className="px-2 py-1 text-xs text-slate-500 hover:text-slate-600"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="px-2 py-1 text-xs bg-indigo-500 text-white rounded hover:bg-indigo-600 font-semibold"
                  >
                    Add
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
