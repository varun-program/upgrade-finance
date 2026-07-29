package com.upgradefinance.dto;

import com.upgradefinance.model.*;
import java.util.List;

public class SyncRequest {
    private long lastSyncTimestamp;
    private List<Transaction> transactions;
    private List<BankAccount> bankAccounts;
    private List<Budget> budgets;
    private List<SavingsGoal> savingsGoals;
    private List<SmartRule> smartRules;

    public SyncRequest() {}

    public SyncRequest(long lastSyncTimestamp, List<Transaction> transactions, List<BankAccount> bankAccounts,
                       List<Budget> budgets, List<SavingsGoal> savingsGoals, List<SmartRule> smartRules) {
        this.lastSyncTimestamp = lastSyncTimestamp;
        this.transactions = transactions;
        this.bankAccounts = bankAccounts;
        this.budgets = budgets;
        this.savingsGoals = savingsGoals;
        this.smartRules = smartRules;
    }

    public long getLastSyncTimestamp() { return lastSyncTimestamp; }
    public void setLastSyncTimestamp(long lastSyncTimestamp) { this.lastSyncTimestamp = lastSyncTimestamp; }
    public List<Transaction> getTransactions() { return transactions; }
    public void setTransactions(List<Transaction> transactions) { this.transactions = transactions; }
    public List<BankAccount> getBankAccounts() { return bankAccounts; }
    public void setBankAccounts(List<BankAccount> bankAccounts) { this.bankAccounts = bankAccounts; }
    public List<Budget> getBudgets() { return budgets; }
    public void setBudgets(List<Budget> budgets) { this.budgets = budgets; }
    public List<SavingsGoal> getSavingsGoals() { return savingsGoals; }
    public void setSavingsGoals(List<SavingsGoal> savingsGoals) { this.savingsGoals = savingsGoals; }
    public List<SmartRule> getSmartRules() { return smartRules; }
    public void setSmartRules(List<SmartRule> smartRules) { this.smartRules = smartRules; }
}
