package com.upgradefinance.db;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.upgradefinance.model.LocalTransaction;

import java.util.List;

@Dao
public interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY timestamp DESC")
    LiveData<List<LocalTransaction>> getAllActiveTransactions();

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    LocalTransaction getTransactionById(String id);

    @Query("SELECT * FROM transactions WHERE referenceNumber = :refNum LIMIT 1")
    LocalTransaction getTransactionByRefNum(String refNum);

    @Query("SELECT * FROM transactions WHERE updatedAt > :timestamp")
    List<LocalTransaction> getChangesSince(long timestamp);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrReplace(LocalTransaction transaction);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrReplaceAll(List<LocalTransaction> transactions);

    @Update
    void update(LocalTransaction transaction);
}
