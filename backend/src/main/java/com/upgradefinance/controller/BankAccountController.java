package com.upgradefinance.controller;

import com.upgradefinance.model.BankAccount;
import com.upgradefinance.model.User;
import com.upgradefinance.repository.BankAccountRepository;
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
@RequestMapping("/api/bank-accounts")
public class BankAccountController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    @GetMapping
    public ResponseEntity<List<BankAccount>> getBankAccounts() {
        User user = getAuthenticatedUser();
        List<BankAccount> accounts = bankAccountRepository.findByUser(user);
        accounts.forEach(a -> a.setUser(null));
        return ResponseEntity.ok(accounts);
    }

    @PostMapping
    public ResponseEntity<?> saveBankAccount(@RequestBody BankAccount account) {
        User user = getAuthenticatedUser();
        account.setId(account.getId() != null ? account.getId() : UUID.randomUUID().toString());
        account.setUser(user);
        account.setUpdatedAt(System.currentTimeMillis());
        BankAccount saved = bankAccountRepository.save(account);
        saved.setUser(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBankAccount(@PathVariable String id) {
        User user = getAuthenticatedUser();
        Optional<BankAccount> accountOpt = bankAccountRepository.findById(id);
        if (accountOpt.isEmpty() || !accountOpt.get().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Bank account not found");
        }
        bankAccountRepository.delete(accountOpt.get());
        return ResponseEntity.ok("Bank account deleted");
    }
}
