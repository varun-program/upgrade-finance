package com.upgradefinance.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "smart_rules")
public class LocalSmartRule {
    @PrimaryKey
    @NonNull
    public String id;
    
    public String pattern;
    public String category;
    
    public boolean isDeleted;
    public long updatedAt;

    public LocalSmartRule() {}
}
