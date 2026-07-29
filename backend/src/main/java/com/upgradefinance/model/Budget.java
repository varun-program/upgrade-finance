package com.upgradefinance.model;

import jakarta.persistence.*;

@Entity
@Table(name = "budgets", uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "category"})})
public class Budget {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String category;

    @Column(name = "limit_amount", nullable = false)
    private double limitAmount;

    private String period = "MONTHLY";

    @Column(name = "is_deleted")
    private boolean isDeleted;

    @Column(name = "updated_at")
    private long updatedAt;

    public Budget() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getLimitAmount() { return limitAmount; }
    public void setLimitAmount(double limitAmount) { this.limitAmount = limitAmount; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
