package com.upgradefinance.model;

import jakarta.persistence.*;

@Entity
@Table(name = "bank_accounts")
public class BankAccount {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "bank_name", nullable = false)
    private String bankName;

    @Column(name = "account_number_suffix", nullable = false)
    private String accountSuffix;

    private double balance;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    public BankAccount() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getAccountSuffix() { return accountSuffix; }
    public void setAccountSuffix(String accountSuffix) { this.accountSuffix = accountSuffix; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
