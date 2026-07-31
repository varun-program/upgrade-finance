import React, { useState, useEffect } from 'react';
import { dataService } from '../utils/api';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, Legend } from 'recharts';

const COLORS = ['#6366f1', '#10b981', '#ef4444', '#f59e0b', '#ec4899', '#8b5cf6', '#06b6d4', '#e2e8f0'];

export default function Analytics() {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadTransactions();
  }, []);

  const loadTransactions = async () => {
    setLoading(true);
    const data = await dataService.getTransactions();
    setTransactions(data);
    setLoading(false);
  };

  // Group by category
  const categoryDataMap = {};
  transactions
    .filter(t => t.transactionType === 'DEBIT')
    .forEach(t => {
      const cat = t.category || 'Other';
      categoryDataMap[cat] = (categoryDataMap[cat] || 0) + t.amount;
    });

  const categoryData = Object.keys(categoryDataMap).map(key => ({
    name: key,
    value: categoryDataMap[key]
  })).sort((a, b) => b.value - a.value);

  // Group by merchant (Top Merchants)
  const merchantDataMap = {};
  transactions
    .filter(t => t.transactionType === 'DEBIT')
    .forEach(t => {
      const merc = t.merchant || 'Unknown';
      merchantDataMap[merc] = (merchantDataMap[merc] || 0) + t.amount;
    });

  const merchantData = Object.keys(merchantDataMap).map(key => ({
    name: key,
    value: merchantDataMap[key]
  })).sort((a, b) => b.value - a.value).slice(0, 5);

  // Group by day of week or month for trends
  const daysOfWeek = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
  const dailyDataMap = { Debit: {}, Credit: {} };

  // Setup default values for last 7 days
  for (let i = 6; i >= 0; i--) {
    const d = new Date();
    d.setDate(d.getDate() - i);
    const label = daysOfWeek[d.getDay()];
    dailyDataMap.Debit[label] = 0;
    dailyDataMap.Credit[label] = 0;
  }

  transactions.forEach(t => {
    const date = new Date(t.timestamp);
    const label = daysOfWeek[date.getDay()];
    if (dailyDataMap[t.transactionType === 'DEBIT' ? 'Debit' : 'Credit'][label] !== undefined) {
      dailyDataMap[t.transactionType === 'DEBIT' ? 'Debit' : 'Credit'][label] += t.amount;
    }
  });

  const weeklyTrendData = Object.keys(dailyDataMap.Debit).map(day => ({
    name: day,
    Debit: dailyDataMap.Debit[day],
    Credit: dailyDataMap.Credit[day]
  }));

  // Group by Month (Last 6 Months)
  const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
  const monthlyDataMap = { Debit: {}, Credit: {} };

  // Setup default values for last 6 months
  for (let i = 5; i >= 0; i--) {
    const d = new Date();
    d.setMonth(d.getMonth() - i);
    const label = `${monthNames[d.getMonth()]} ${d.getFullYear().toString().substring(2)}`;
    monthlyDataMap.Debit[label] = 0;
    monthlyDataMap.Credit[label] = 0;
  }

  transactions.forEach(t => {
    const date = new Date(t.timestamp);
    const label = `${monthNames[date.getMonth()]} ${date.getFullYear().toString().substring(2)}`;
    if (monthlyDataMap.Debit[label] !== undefined) {
      if (t.transactionType === 'DEBIT') {
        monthlyDataMap.Debit[label] += t.amount;
      } else {
        monthlyDataMap.Credit[label] += t.amount;
      }
    }
  });

  const monthlyTrendData = Object.keys(monthlyDataMap.Debit).map(month => ({
    name: month,
    Debit: monthlyDataMap.Debit[month],
    Credit: monthlyDataMap.Credit[month]
  }));

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-4xl font-extrabold tracking-tight">Financial Insights</h1>
        <p className="text-slate-500 dark:text-slate-400 mt-1">Deep analysis of your spending habits and trends.</p>
      </div>

      {loading ? (
        <div className="text-center py-12 text-slate-400">Analyzing data...</div>
      ) : transactions.length === 0 ? (
        <div className="glass p-12 text-center text-slate-400 rounded-2xl border">
          No data available yet. Add transactions on the Dashboard to see charts!
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          {/* Bar Chart: Weekly Income vs Expenses */}
          <div className="p-6 glass rounded-2xl border shadow-sm space-y-4">
            <h3 className="text-lg font-bold">Debit vs Credit (Last 7 Days)</h3>
            <div className="h-80">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={weeklyTrendData}>
                  <CartesianGrid strokeDasharray="3 3" opacity={0.1} />
                  <XAxis dataKey="name" />
                  <YAxis />
                  <Tooltip formatter={(value) => `₹${value}`} />
                  <Legend />
                  <Bar dataKey="Debit" fill="#ef4444" radius={[4, 4, 0, 0]} />
                  <Bar dataKey="Credit" fill="#10b981" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>

          {/* Bar Chart: Monthly Income vs Expenses (Last 6 Months) */}
          <div className="p-6 glass rounded-2xl border shadow-sm space-y-4">
            <h3 className="text-lg font-bold">Monthly Expense vs Income (Last 6 Months)</h3>
            <div className="h-80">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={monthlyTrendData}>
                  <CartesianGrid strokeDasharray="3 3" opacity={0.1} />
                  <XAxis dataKey="name" />
                  <YAxis />
                  <Tooltip formatter={(value) => `₹${value.toFixed(2)}`} />
                  <Legend />
                  <Bar dataKey="Debit" name="Monthly Expense" fill="#ef4444" radius={[4, 4, 0, 0]} />
                  <Bar dataKey="Credit" name="Monthly Income" fill="#10b981" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>

          {/* Pie Chart: Categories */}
          <div className="p-6 glass rounded-2xl border shadow-sm space-y-4 flex flex-col justify-between">
            <h3 className="text-lg font-bold">Spending by Category</h3>
            <div className="h-80 flex items-center justify-center">
              {categoryData.length === 0 ? (
                <div className="text-sm text-slate-400">No expenses recorded</div>
              ) : (
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={categoryData}
                      cx="50%"
                      cy="50%"
                      innerRadius={60}
                      outerRadius={80}
                      paddingAngle={5}
                      dataKey="value"
                    >
                      {categoryData.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip formatter={(value) => `₹${value.toFixed(2)}`} />
                    <Legend layout="horizontal" verticalAlign="bottom" align="center" />
                  </PieChart>
                </ResponsiveContainer>
              )}
            </div>
          </div>

          {/* Bar Chart: Top Merchants */}
          <div className="p-6 glass rounded-2xl border shadow-sm space-y-4 lg:col-span-2">
            <h3 className="text-lg font-bold">Top 5 Spending Merchants</h3>
            <div className="h-80">
              {merchantData.length === 0 ? (
                <div className="text-sm text-slate-400 text-center py-20">No spending tracked</div>
              ) : (
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={merchantData} layout="vertical">
                    <CartesianGrid strokeDasharray="3 3" opacity={0.1} />
                    <XAxis type="number" />
                    <YAxis dataKey="name" type="category" width={100} />
                    <Tooltip formatter={(value) => `₹${value.toFixed(2)}`} />
                    <Bar dataKey="value" fill="#6366f1" radius={[0, 4, 4, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
