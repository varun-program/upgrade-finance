package com.upgradefinance.model;

import jakarta.persistence.*;

@Entity
@Table(name = "savings_goals")
public class SavingsGoal {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(name = "target_amount", nullable = false)
    private double targetAmount;

    @Column(name = "saved_amount")
    private double savedAmount;

    @Column(name = "target_date")
    private Long targetDate;

    @Column(name = "is_deleted")
    private boolean isDeleted;

    @Column(name = "updated_at")
    private long updatedAt;

    public SavingsGoal() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getTargetAmount() { return targetAmount; }
    public void setTargetAmount(double targetAmount) { this.targetAmount = targetAmount; }
    public double getSavedAmount() { return savedAmount; }
    public void setSavedAmount(double savedAmount) { this.savedAmount = savedAmount; }
    public Long getTargetDate() { return targetDate; }
    public void setTargetDate(Long targetDate) { this.targetDate = targetDate; }
    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
