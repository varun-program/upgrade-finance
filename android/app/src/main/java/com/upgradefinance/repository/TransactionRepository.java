package com.upgradefinance.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.upgradefinance.db.AppDatabase;
import com.upgradefinance.db.TransactionDao;
import com.upgradefinance.model.LocalTransaction;
import com.upgradefinance.worker.SyncWorker;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransactionRepository {

    private final TransactionDao transactionDao;
    private final LiveData<List<LocalTransaction>> allTransactions;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Context context;

    public TransactionRepository(Context context) {
        this.context = context.getApplicationContext();
        AppDatabase db = AppDatabase.getDatabase(context);
        this.transactionDao = db.transactionDao();
        this.allTransactions = transactionDao.getAllActiveTransactions();
    }

    public LiveData<List<LocalTransaction>> getAllTransactions() {
        return allTransactions;
    }

    public void insert(LocalTransaction transaction) {
        executorService.execute(() -> {
            transactionDao.insertOrReplace(transaction);
            triggerBackgroundSync();
        });
    }

    public void delete(String id) {
        executorService.execute(() -> {
            LocalTransaction tx = transactionDao.getTransactionById(id);
            if (tx != null) {
                tx.isDeleted = true;
                tx.updatedAt = System.currentTimeMillis();
                transactionDao.update(tx);
                triggerBackgroundSync();
            }
        });
    }

    private void triggerBackgroundSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueue(syncRequest);
    }
}
