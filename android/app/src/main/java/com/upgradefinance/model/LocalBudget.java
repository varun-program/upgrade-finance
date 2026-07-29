package com.upgradefinance.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "budgets")
public class LocalBudget {
    @PrimaryKey
    @NonNull
    public String id;
    
    public String category;
    public double limitAmount;
    public String period;
    
    public boolean isDeleted;
    public long updatedAt;

    public LocalBudget() {}
}
