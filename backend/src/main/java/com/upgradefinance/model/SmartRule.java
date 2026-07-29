package com.upgradefinance.model;

import jakarta.persistence.*;

@Entity
@Table(name = "smart_rules")
public class SmartRule {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String pattern;

    @Column(nullable = false)
    private String category;

    @Column(name = "is_deleted")
    private boolean isDeleted;

    @Column(name = "updated_at")
    private long updatedAt;

    public SmartRule() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
