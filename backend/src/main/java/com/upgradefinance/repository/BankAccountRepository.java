package com.upgradefinance.repository;

import com.upgradefinance.model.BankAccount;
import com.upgradefinance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, String> {
    List<BankAccount> findByUser(User user);
    List<BankAccount> findByUserAndUpdatedAtGreaterThan(User user, long timestamp);
}
