package com.upgradefinance.controller;

import com.upgradefinance.dto.SyncRequest;
import com.upgradefinance.dto.SyncResponse;
import com.upgradefinance.model.*;
import com.upgradefinance.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private SavingsGoalRepository savingsGoalRepository;

    @Autowired
    private SmartRuleRepository smartRuleRepository;

    @PostMapping
    public ResponseEntity<SyncResponse> sync(@RequestBody SyncRequest syncRequest) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        long serverSyncTimestamp = System.currentTimeMillis();

        // 1. Process Client Transactions
        if (syncRequest.getTransactions() != null) {
            for (Transaction clientTx : syncRequest.getTransactions()) {
                clientTx.setUser(user);
                Optional<Transaction> serverTxOpt = transactionRepository.findById(clientTx.getId());
                if (serverTxOpt.isEmpty() || clientTx.getUpdatedAt() > serverTxOpt.get().getUpdatedAt()) {
                    transactionRepository.save(clientTx);
                }
            }
        }

        // 2. Process Client Bank Accounts
        if (syncRequest.getBankAccounts() != null) {
            for (BankAccount clientAcc : syncRequest.getBankAccounts()) {
                clientAcc.setUser(user);
                Optional<BankAccount> serverAccOpt = bankAccountRepository.findById(clientAcc.getId());
                if (serverAccOpt.isEmpty() || clientAcc.getUpdatedAt() > serverAccOpt.get().getUpdatedAt()) {
                    bankAccountRepository.save(clientAcc);
                }
            }
        }

        // 3. Process Client Budgets
        if (syncRequest.getBudgets() != null) {
            for (Budget clientBgt : syncRequest.getBudgets()) {
                clientBgt.setUser(user);
                Optional<Budget> serverBgtOpt = budgetRepository.findById(clientBgt.getId());
                if (serverBgtOpt.isEmpty() || clientBgt.getUpdatedAt() > serverBgtOpt.get().getUpdatedAt()) {
                    budgetRepository.save(clientBgt);
                }
            }
        }

        // 4. Process Client Savings Goals
        if (syncRequest.getSavingsGoals() != null) {
            for (SavingsGoal clientGoal : syncRequest.getSavingsGoals()) {
                clientGoal.setUser(user);
                Optional<SavingsGoal> serverGoalOpt = savingsGoalRepository.findById(clientGoal.getId());
                if (serverGoalOpt.isEmpty() || clientGoal.getUpdatedAt() > serverGoalOpt.get().getUpdatedAt()) {
                    savingsGoalRepository.save(clientGoal);
                }
            }
        }

        // 5. Process Client Smart Rules
        if (syncRequest.getSmartRules() != null) {
            for (SmartRule clientRule : syncRequest.getSmartRules()) {
                clientRule.setUser(user);
                Optional<SmartRule> serverRuleOpt = smartRuleRepository.findById(clientRule.getId());
                if (serverRuleOpt.isEmpty() || clientRule.getUpdatedAt() > serverRuleOpt.get().getUpdatedAt()) {
                    smartRuleRepository.save(clientRule);
                }
            }
        }

        // Fetch updates from database for client (all where updatedAt > client.lastSyncTimestamp)
        long lastSync = syncRequest.getLastSyncTimestamp();

        List<Transaction> serverTxs = transactionRepository.findByUserAndUpdatedAtGreaterThan(user, lastSync);
        List<BankAccount> serverAccs = bankAccountRepository.findByUserAndUpdatedAtGreaterThan(user, lastSync);
        List<Budget> serverBgts = budgetRepository.findByUserAndUpdatedAtGreaterThan(user, lastSync);
        List<SavingsGoal> serverGoals = savingsGoalRepository.findByUserAndUpdatedAtGreaterThan(user, lastSync);
        List<SmartRule> serverRules = smartRuleRepository.findByUserAndUpdatedAtGreaterThan(user, lastSync);

        // Strip User credentials from returned JPA objects to avoid loop JSON references (or keep it null)
        serverTxs.forEach(t -> t.setUser(null));
        serverAccs.forEach(a -> a.setUser(null));
        serverBgts.forEach(b -> b.setUser(null));
        serverGoals.forEach(g -> g.setUser(null));
        serverRules.forEach(r -> r.setUser(null));

        SyncResponse response = new SyncResponse(serverSyncTimestamp, serverTxs, serverAccs, serverBgts, serverGoals, serverRules);
        return ResponseEntity.ok(response);
    }
}
