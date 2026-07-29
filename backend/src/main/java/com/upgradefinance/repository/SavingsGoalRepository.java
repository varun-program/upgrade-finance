package com.upgradefinance.repository;

import com.upgradefinance.model.SavingsGoal;
import com.upgradefinance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, String> {
    List<SavingsGoal> findByUserAndIsDeletedFalse(User user);
    List<SavingsGoal> findByUserAndUpdatedAtGreaterThan(User user, long timestamp);
}
