package com.upgradefinance.repository;

import com.upgradefinance.model.Transaction;
import com.upgradefinance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {
    List<Transaction> findByUserAndIsDeletedFalse(User user);
    Optional<Transaction> findByUserAndReferenceNumber(User user, String referenceNumber);
    List<Transaction> findByUserAndUpdatedAtGreaterThan(User user, long timestamp);
    
    @Query("SELECT t FROM Transaction t WHERE t.user = :user AND t.isDeleted = false AND (" +
           "LOWER(t.merchant) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(t.category) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(t.bank) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(t.upiId) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(t.referenceNumber) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Transaction> searchTransactions(@Param("user") User user, @Param("query") String query);
}
