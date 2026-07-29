import axios from 'axios';

const API_BASE = 'http://localhost:8080/api';

// Check if we are running in cloud or local mode
export const isLocalMode = () => {
  return localStorage.getItem('auth_token') === null;
};

// Axios Instance
const api = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('auth_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
}, (error) => {
  return Promise.reject(error);
});

// Helper for local database simulation
const getLocalData = (key) => {
  const data = localStorage.getItem(key);
  return data ? JSON.parse(data) : [];
};

const saveLocalData = (key, data) => {
  localStorage.setItem(key, JSON.stringify(data));
};

export const syncWithServer = async () => {
  if (isLocalMode()) return { success: false, reason: 'Local mode active' };
  
  const lastSyncTimestamp = parseInt(localStorage.getItem('last_sync_timestamp') || '0');
  
  const clientChanges = {
    lastSyncTimestamp,
    transactions: getLocalData('local_transactions').filter(t => t.updatedAt > lastSyncTimestamp),
    bankAccounts: getLocalData('local_bank_accounts').filter(b => b.updatedAt > lastSyncTimestamp),
    budgets: getLocalData('local_budgets').filter(b => b.updatedAt > lastSyncTimestamp),
    savingsGoals: getLocalData('local_savings_goals').filter(s => s.updatedAt > lastSyncTimestamp),
    smartRules: getLocalData('local_smart_rules').filter(r => r.updatedAt > lastSyncTimestamp),
  };

  try {
    const response = await api.post('/sync', clientChanges);
    const { syncTimestamp, transactions, bankAccounts, budgets, savingsGoals, smartRules } = response.data;
    
    // Merge server changes into local DB (last-write-wins)
    const mergeData = (key, serverItems) => {
      const localItems = getLocalData(key);
      const itemMap = new Map(localItems.map(item => [item.id, item]));
      serverItems.forEach(item => {
        const localItem = itemMap.get(item.id);
        if (!localItem || item.updatedAt > localItem.updatedAt) {
          itemMap.set(item.id, item);
        }
      });
      saveLocalData(key, Array.from(itemMap.values()));
    };

    mergeData('local_transactions', transactions);
    mergeData('local_bank_accounts', bankAccounts);
    mergeData('local_budgets', budgets);
    mergeData('local_savings_goals', savingsGoals);
    mergeData('local_smart_rules', smartRules);

    localStorage.setItem('last_sync_timestamp', syncTimestamp.toString());
    return { success: true };
  } catch (error) {
    console.error('Sync failed', error);
    return { success: false, reason: error.message };
  }
};

// CRUD handlers that automatically handle Local vs Cloud Mode
export const dataService = {
  getTransactions: async () => {
    if (isLocalMode()) {
      return getLocalData('local_transactions').filter(t => !t.isDeleted);
    }
    try {
      const res = await api.get('/transactions');
      return res.data;
    } catch (e) {
      // Offline fallback
      return getLocalData('local_transactions').filter(t => !t.isDeleted);
    }
  },

  saveTransaction: async (tx) => {
    const enriched = {
      ...tx,
      id: tx.id || Math.random().toString(36).substr(2, 9),
      updatedAt: Date.now(),
      isDeleted: false,
    };
    
    const local = getLocalData('local_transactions');
    const idx = local.findIndex(t => t.id === enriched.id);
    if (idx >= 0) local[idx] = enriched;
    else local.push(enriched);
    saveLocalData('local_transactions', local);

    if (!isLocalMode()) {
      try {
        await api.post('/transactions', enriched);
      } catch (e) {
        console.log('Saved locally, will sync later.');
      }
    }
    return enriched;
  },

  deleteTransaction: async (id) => {
    const local = getLocalData('local_transactions');
    const idx = local.findIndex(t => t.id === id);
    if (idx >= 0) {
      local[idx].isDeleted = true;
      local[idx].updatedAt = Date.now();
      saveLocalData('local_transactions', local);
    }
    if (!isLocalMode()) {
      try {
        await api.delete(`/transactions/${id}`);
      } catch (e) {
        console.log('Marked as deleted locally, will sync later.');
      }
    }
  },

  getBudgets: async () => {
    if (isLocalMode()) {
      return getLocalData('local_budgets').filter(b => !b.isDeleted);
    }
    try {
      const res = await api.get('/budgets');
      return res.data;
    } catch (e) {
      return getLocalData('local_budgets').filter(b => !b.isDeleted);
    }
  },

  saveBudget: async (budget) => {
    const enriched = {
      ...budget,
      id: budget.id || Math.random().toString(36).substr(2, 9),
      updatedAt: Date.now(),
      isDeleted: false,
    };
    const local = getLocalData('local_budgets');
    const idx = local.findIndex(b => b.id === enriched.id);
    if (idx >= 0) local[idx] = enriched;
    else local.push(enriched);
    saveLocalData('local_budgets', local);

    if (!isLocalMode()) {
      try {
        await api.post('/budgets', enriched);
      } catch (e) {
        console.log('Saved budget locally.');
      }
    }
    return enriched;
  },

  deleteBudget: async (id) => {
    const local = getLocalData('local_budgets');
    const idx = local.findIndex(b => b.id === id);
    if (idx >= 0) {
      local[idx].isDeleted = true;
      local[idx].updatedAt = Date.now();
      saveLocalData('local_budgets', local);
    }
    if (!isLocalMode()) {
      try {
        await api.delete(`/budgets/${id}`);
      } catch (e) {
        console.log('Deleted budget locally.');
      }
    }
  },

  getSavingsGoals: async () => {
    return getLocalData('local_savings_goals').filter(g => !g.isDeleted);
  },

  saveSavingsGoal: async (goal) => {
    const enriched = {
      ...goal,
      id: goal.id || Math.random().toString(36).substr(2, 9),
      updatedAt: Date.now(),
      isDeleted: false,
    };
    const local = getLocalData('local_savings_goals');
    const idx = local.findIndex(g => g.id === enriched.id);
    if (idx >= 0) local[idx] = enriched;
    else local.push(enriched);
    saveLocalData('local_savings_goals', local);
    return enriched;
  },

  deleteSavingsGoal: async (id) => {
    const local = getLocalData('local_savings_goals');
    const idx = local.findIndex(g => g.id === id);
    if (idx >= 0) {
      local[idx].isDeleted = true;
      local[idx].updatedAt = Date.now();
      saveLocalData('local_savings_goals', local);
    }
  },

  getSmartRules: async () => {
    return getLocalData('local_smart_rules').filter(r => !r.isDeleted);
  },

  saveSmartRule: async (rule) => {
    const enriched = {
      ...rule,
      id: rule.id || Math.random().toString(36).substr(2, 9),
      updatedAt: Date.now(),
      isDeleted: false,
    };
    const local = getLocalData('local_smart_rules');
    const idx = local.findIndex(r => r.id === enriched.id);
    if (idx >= 0) local[idx] = enriched;
    else local.push(enriched);
    saveLocalData('local_smart_rules', local);
    return enriched;
  },

  deleteSmartRule: async (id) => {
    const local = getLocalData('local_smart_rules');
    const idx = local.findIndex(r => r.id === id);
    if (idx >= 0) {
      local[idx].isDeleted = true;
      local[idx].updatedAt = Date.now();
      saveLocalData('local_smart_rules', local);
    }
  },
  
  askAi: async (message) => {
    if (isLocalMode()) {
      return "Local Mode AI response: I can parse your request when synced to cloud. Set your Gemini API key in backend configurations for full capabilities!";
    }
    try {
      const res = await api.post('/ai/chat', { message });
      return res.data.response;
    } catch (e) {
      return "Unable to connect to AI server. Check connection.";
    }
  }
};

export default api;
