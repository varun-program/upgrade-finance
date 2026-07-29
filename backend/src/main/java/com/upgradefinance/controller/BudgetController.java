package com.upgradefinance.controller;

import com.upgradefinance.model.Budget;
import com.upgradefinance.model.User;
import com.upgradefinance.repository.BudgetRepository;
import com.upgradefinance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    @GetMapping
    public ResponseEntity<List<Budget>> getBudgets() {
        User user = getAuthenticatedUser();
        List<Budget> budgets = budgetRepository.findByUserAndIsDeletedFalse(user);
        budgets.forEach(b -> b.setUser(null));
        return ResponseEntity.ok(budgets);
    }

    @PostMapping
    public ResponseEntity<?> saveBudget(@RequestBody Budget budget) {
        User user = getAuthenticatedUser();
        budget.setId(budget.getId() != null ? budget.getId() : UUID.randomUUID().toString());
        budget.setUser(user);
        budget.setUpdatedAt(System.currentTimeMillis());
        Budget saved = budgetRepository.save(budget);
        saved.setUser(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBudget(@PathVariable String id) {
        User user = getAuthenticatedUser();
        Optional<Budget> budgetOpt = budgetRepository.findById(id);
        if (budgetOpt.isEmpty() || !budgetOpt.get().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Budget not found");
        }
        Budget budget = budgetOpt.get();
        budget.setDeleted(true);
        budget.setUpdatedAt(System.currentTimeMillis());
        budgetRepository.save(budget);
        return ResponseEntity.ok("Budget deleted");
    }
}
