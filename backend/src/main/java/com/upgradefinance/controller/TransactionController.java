package com.upgradefinance.controller;

import com.upgradefinance.model.Transaction;
import com.upgradefinance.model.User;
import com.upgradefinance.repository.TransactionRepository;
import com.upgradefinance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions(@RequestParam(value = "query", required = false) String query) {
        User user = getAuthenticatedUser();
        List<Transaction> txList;
        if (query != null && !query.trim().isEmpty()) {
            txList = transactionRepository.searchTransactions(user, query);
        } else {
            txList = transactionRepository.findByUserAndIsDeletedFalse(user);
        }
        txList.forEach(t -> t.setUser(null)); // Avoid circular references
        return ResponseEntity.ok(txList);
    }

    @PostMapping
    public ResponseEntity<Transaction> createTransaction(@RequestBody Transaction transaction) {
        User user = getAuthenticatedUser();
        transaction.setId(transaction.getId() != null ? transaction.getId() : UUID.randomUUID().toString());
        transaction.setUser(user);
        transaction.setUpdatedAt(System.currentTimeMillis());
        Transaction saved = transactionRepository.save(transaction);
        saved.setUser(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTransaction(@PathVariable String id, @RequestBody Transaction transactionDetails) {
        User user = getAuthenticatedUser();
        Optional<Transaction> txOpt = transactionRepository.findById(id);
        
        if (txOpt.isEmpty() || !txOpt.get().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Transaction not found");
        }

        Transaction tx = txOpt.get();
        tx.setAmount(transactionDetails.getAmount());
        tx.setCategory(transactionDetails.getCategory());
        tx.setMerchant(transactionDetails.getMerchant());
        tx.setBank(transactionDetails.getBank());
        tx.setTransactionType(transactionDetails.getTransactionType());
        tx.setUpdatedAt(System.currentTimeMillis());
        
        Transaction saved = transactionRepository.save(tx);
        saved.setUser(null);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTransaction(@PathVariable String id) {
        User user = getAuthenticatedUser();
        Optional<Transaction> txOpt = transactionRepository.findById(id);

        if (txOpt.isEmpty() || !txOpt.get().getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Transaction not found");
        }

        Transaction tx = txOpt.get();
        tx.setDeleted(true);
        tx.setUpdatedAt(System.currentTimeMillis());
        transactionRepository.save(tx);
        return ResponseEntity.ok().body("Transaction marked as deleted");
    }

    @DeleteMapping("/all")
    public ResponseEntity<?> deleteAllTransactions() {
        User user = getAuthenticatedUser();
        List<Transaction> transactions = transactionRepository.findByUserAndIsDeletedFalse(user);
        long now = System.currentTimeMillis();
        for (Transaction tx : transactions) {
            tx.setDeleted(true);
            tx.setUpdatedAt(now);
        }
        transactionRepository.saveAll(transactions);
        return ResponseEntity.ok().body("All transactions marked as deleted");
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportTransactions(@RequestParam("format") String format) {
        User user = getAuthenticatedUser();
        List<Transaction> transactions = transactionRepository.findByUserAndIsDeletedFalse(user);

        if ("csv".equalsIgnoreCase(format)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(out);
            writer.println("ID,Amount,Date,Merchant,UPI ID,Ref Number,Bank,Type,Category");
            for (Transaction tx : transactions) {
                writer.printf("%s,%.2f,%d,%s,%s,%s,%s,%s,%s\n",
                        tx.getId(), tx.getAmount(), tx.getTimestamp(),
                        tx.getMerchant() != null ? tx.getMerchant() : "",
                        tx.getUpiId() != null ? tx.getUpiId() : "",
                        tx.getReferenceNumber() != null ? tx.getReferenceNumber() : "",
                        tx.getBank() != null ? tx.getBank() : "",
                        tx.getTransactionType(), tx.getCategory());
            }
            writer.flush();
            byte[] data = out.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentDispositionFormData("attachment", "transactions.csv");
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        }

        // Return empty body for unsupported format (pdf/excel placeholder)
        return ResponseEntity.badRequest().build();
    }
}
