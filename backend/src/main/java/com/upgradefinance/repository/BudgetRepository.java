package com.upgradefinance.repository;

import com.upgradefinance.model.Budget;
import com.upgradefinance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, String> {
    List<Budget> findByUserAndIsDeletedFalse(User user);
    Optional<Budget> findByUserAndCategory(User user, String category);
    List<Budget> findByUserAndUpdatedAtGreaterThan(User user, long timestamp);
}
