package com.upgradefinance.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "transactions")
public class LocalTransaction {
    @PrimaryKey
    @NonNull
    public String id;
    
    public double amount;
    public long timestamp;
    public String merchant;
    public String upiId;
    public String referenceNumber;
    public String bank;
    public String transactionType; // DEBIT, CREDIT
    public String category;
    
    public boolean isDeleted;
    public long updatedAt;

    public LocalTransaction() {}
}
