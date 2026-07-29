package com.upgradefinance.db;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.upgradefinance.model.LocalTransaction;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class TransactionDao_Impl implements TransactionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<LocalTransaction> __insertionAdapterOfLocalTransaction;

  private final EntityDeletionOrUpdateAdapter<LocalTransaction> __updateAdapterOfLocalTransaction;

  public TransactionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfLocalTransaction = new EntityInsertionAdapter<LocalTransaction>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `transactions` (`id`,`amount`,`timestamp`,`merchant`,`upiId`,`referenceNumber`,`bank`,`transactionType`,`category`,`isDeleted`,`updatedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final LocalTransaction entity) {
        if (entity.id == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.id);
        }
        statement.bindDouble(2, entity.amount);
        statement.bindLong(3, entity.timestamp);
        if (entity.merchant == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.merchant);
        }
        if (entity.upiId == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.upiId);
        }
        if (entity.referenceNumber == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.referenceNumber);
        }
        if (entity.bank == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.bank);
        }
        if (entity.transactionType == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.transactionType);
        }
        if (entity.category == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.category);
        }
        final int _tmp = entity.isDeleted ? 1 : 0;
        statement.bindLong(10, _tmp);
        statement.bindLong(11, entity.updatedAt);
      }
    };
    this.__updateAdapterOfLocalTransaction = new EntityDeletionOrUpdateAdapter<LocalTransaction>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `transactions` SET `id` = ?,`amount` = ?,`timestamp` = ?,`merchant` = ?,`upiId` = ?,`referenceNumber` = ?,`bank` = ?,`transactionType` = ?,`category` = ?,`isDeleted` = ?,`updatedAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          final LocalTransaction entity) {
        if (entity.id == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.id);
        }
        statement.bindDouble(2, entity.amount);
        statement.bindLong(3, entity.timestamp);
        if (entity.merchant == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.merchant);
        }
        if (entity.upiId == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.upiId);
        }
        if (entity.referenceNumber == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.referenceNumber);
        }
        if (entity.bank == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.bank);
        }
        if (entity.transactionType == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.transactionType);
        }
        if (entity.category == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.category);
        }
        final int _tmp = entity.isDeleted ? 1 : 0;
        statement.bindLong(10, _tmp);
        statement.bindLong(11, entity.updatedAt);
        if (entity.id == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.id);
        }
      }
    };
  }

  @Override
  public void insertOrReplace(final LocalTransaction transaction) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfLocalTransaction.insert(transaction);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insertOrReplaceAll(final List<LocalTransaction> transactions) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfLocalTransaction.insert(transactions);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void update(final LocalTransaction transaction) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfLocalTransaction.handle(transaction);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public LiveData<List<LocalTransaction>> getAllActiveTransactions() {
    final String _sql = "SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[] {"transactions"}, false, new Callable<List<LocalTransaction>>() {
      @Override
      @Nullable
      public List<LocalTransaction> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfMerchant = CursorUtil.getColumnIndexOrThrow(_cursor, "merchant");
          final int _cursorIndexOfUpiId = CursorUtil.getColumnIndexOrThrow(_cursor, "upiId");
          final int _cursorIndexOfReferenceNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "referenceNumber");
          final int _cursorIndexOfBank = CursorUtil.getColumnIndexOrThrow(_cursor, "bank");
          final int _cursorIndexOfTransactionType = CursorUtil.getColumnIndexOrThrow(_cursor, "transactionType");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<LocalTransaction> _result = new ArrayList<LocalTransaction>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalTransaction _item;
            _item = new LocalTransaction();
            if (_cursor.isNull(_cursorIndexOfId)) {
              _item.id = null;
            } else {
              _item.id = _cursor.getString(_cursorIndexOfId);
            }
            _item.amount = _cursor.getDouble(_cursorIndexOfAmount);
            _item.timestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            if (_cursor.isNull(_cursorIndexOfMerchant)) {
              _item.merchant = null;
            } else {
              _item.merchant = _cursor.getString(_cursorIndexOfMerchant);
            }
            if (_cursor.isNull(_cursorIndexOfUpiId)) {
              _item.upiId = null;
            } else {
              _item.upiId = _cursor.getString(_cursorIndexOfUpiId);
            }
            if (_cursor.isNull(_cursorIndexOfReferenceNumber)) {
              _item.referenceNumber = null;
            } else {
              _item.referenceNumber = _cursor.getString(_cursorIndexOfReferenceNumber);
            }
            if (_cursor.isNull(_cursorIndexOfBank)) {
              _item.bank = null;
            } else {
              _item.bank = _cursor.getString(_cursorIndexOfBank);
            }
            if (_cursor.isNull(_cursorIndexOfTransactionType)) {
              _item.transactionType = null;
            } else {
              _item.transactionType = _cursor.getString(_cursorIndexOfTransactionType);
            }
            if (_cursor.isNull(_cursorIndexOfCategory)) {
              _item.category = null;
            } else {
              _item.category = _cursor.getString(_cursorIndexOfCategory);
            }
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDeleted);
            _item.isDeleted = _tmp != 0;
            _item.updatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public LocalTransaction getTransactionById(final String id) {
    final String _sql = "SELECT * FROM transactions WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (id == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, id);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
      final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
      final int _cursorIndexOfMerchant = CursorUtil.getColumnIndexOrThrow(_cursor, "merchant");
      final int _cursorIndexOfUpiId = CursorUtil.getColumnIndexOrThrow(_cursor, "upiId");
      final int _cursorIndexOfReferenceNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "referenceNumber");
      final int _cursorIndexOfBank = CursorUtil.getColumnIndexOrThrow(_cursor, "bank");
      final int _cursorIndexOfTransactionType = CursorUtil.getColumnIndexOrThrow(_cursor, "transactionType");
      final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
      final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
      final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
      final LocalTransaction _result;
      if (_cursor.moveToFirst()) {
        _result = new LocalTransaction();
        if (_cursor.isNull(_cursorIndexOfId)) {
          _result.id = null;
        } else {
          _result.id = _cursor.getString(_cursorIndexOfId);
        }
        _result.amount = _cursor.getDouble(_cursorIndexOfAmount);
        _result.timestamp = _cursor.getLong(_cursorIndexOfTimestamp);
        if (_cursor.isNull(_cursorIndexOfMerchant)) {
          _result.merchant = null;
        } else {
          _result.merchant = _cursor.getString(_cursorIndexOfMerchant);
        }
        if (_cursor.isNull(_cursorIndexOfUpiId)) {
          _result.upiId = null;
        } else {
          _result.upiId = _cursor.getString(_cursorIndexOfUpiId);
        }
        if (_cursor.isNull(_cursorIndexOfReferenceNumber)) {
          _result.referenceNumber = null;
        } else {
          _result.referenceNumber = _cursor.getString(_cursorIndexOfReferenceNumber);
        }
        if (_cursor.isNull(_cursorIndexOfBank)) {
          _result.bank = null;
        } else {
          _result.bank = _cursor.getString(_cursorIndexOfBank);
        }
        if (_cursor.isNull(_cursorIndexOfTransactionType)) {
          _result.transactionType = null;
        } else {
          _result.transactionType = _cursor.getString(_cursorIndexOfTransactionType);
        }
        if (_cursor.isNull(_cursorIndexOfCategory)) {
          _result.category = null;
        } else {
          _result.category = _cursor.getString(_cursorIndexOfCategory);
        }
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfIsDeleted);
        _result.isDeleted = _tmp != 0;
        _result.updatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public LocalTransaction getTransactionByRefNum(final String refNum) {
    final String _sql = "SELECT * FROM transactions WHERE referenceNumber = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (refNum == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, refNum);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
      final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
      final int _cursorIndexOfMerchant = CursorUtil.getColumnIndexOrThrow(_cursor, "merchant");
      final int _cursorIndexOfUpiId = CursorUtil.getColumnIndexOrThrow(_cursor, "upiId");
      final int _cursorIndexOfReferenceNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "referenceNumber");
      final int _cursorIndexOfBank = CursorUtil.getColumnIndexOrThrow(_cursor, "bank");
      final int _cursorIndexOfTransactionType = CursorUtil.getColumnIndexOrThrow(_cursor, "transactionType");
      final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
      final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
      final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
      final LocalTransaction _result;
      if (_cursor.moveToFirst()) {
        _result = new LocalTransaction();
        if (_cursor.isNull(_cursorIndexOfId)) {
          _result.id = null;
        } else {
          _result.id = _cursor.getString(_cursorIndexOfId);
        }
        _result.amount = _cursor.getDouble(_cursorIndexOfAmount);
        _result.timestamp = _cursor.getLong(_cursorIndexOfTimestamp);
        if (_cursor.isNull(_cursorIndexOfMerchant)) {
          _result.merchant = null;
        } else {
          _result.merchant = _cursor.getString(_cursorIndexOfMerchant);
        }
        if (_cursor.isNull(_cursorIndexOfUpiId)) {
          _result.upiId = null;
        } else {
          _result.upiId = _cursor.getString(_cursorIndexOfUpiId);
        }
        if (_cursor.isNull(_cursorIndexOfReferenceNumber)) {
          _result.referenceNumber = null;
        } else {
          _result.referenceNumber = _cursor.getString(_cursorIndexOfReferenceNumber);
        }
        if (_cursor.isNull(_cursorIndexOfBank)) {
          _result.bank = null;
        } else {
          _result.bank = _cursor.getString(_cursorIndexOfBank);
        }
        if (_cursor.isNull(_cursorIndexOfTransactionType)) {
          _result.transactionType = null;
        } else {
          _result.transactionType = _cursor.getString(_cursorIndexOfTransactionType);
        }
        if (_cursor.isNull(_cursorIndexOfCategory)) {
          _result.category = null;
        } else {
          _result.category = _cursor.getString(_cursorIndexOfCategory);
        }
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfIsDeleted);
        _result.isDeleted = _tmp != 0;
        _result.updatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public List<LocalTransaction> getChangesSince(final long timestamp) {
    final String _sql = "SELECT * FROM transactions WHERE updatedAt > ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, timestamp);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
      final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
      final int _cursorIndexOfMerchant = CursorUtil.getColumnIndexOrThrow(_cursor, "merchant");
      final int _cursorIndexOfUpiId = CursorUtil.getColumnIndexOrThrow(_cursor, "upiId");
      final int _cursorIndexOfReferenceNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "referenceNumber");
      final int _cursorIndexOfBank = CursorUtil.getColumnIndexOrThrow(_cursor, "bank");
      final int _cursorIndexOfTransactionType = CursorUtil.getColumnIndexOrThrow(_cursor, "transactionType");
      final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
      final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
      final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
      final List<LocalTransaction> _result = new ArrayList<LocalTransaction>(_cursor.getCount());
      while (_cursor.moveToNext()) {
        final LocalTransaction _item;
        _item = new LocalTransaction();
        if (_cursor.isNull(_cursorIndexOfId)) {
          _item.id = null;
        } else {
          _item.id = _cursor.getString(_cursorIndexOfId);
        }
        _item.amount = _cursor.getDouble(_cursorIndexOfAmount);
        _item.timestamp = _cursor.getLong(_cursorIndexOfTimestamp);
        if (_cursor.isNull(_cursorIndexOfMerchant)) {
          _item.merchant = null;
        } else {
          _item.merchant = _cursor.getString(_cursorIndexOfMerchant);
        }
        if (_cursor.isNull(_cursorIndexOfUpiId)) {
          _item.upiId = null;
        } else {
          _item.upiId = _cursor.getString(_cursorIndexOfUpiId);
        }
        if (_cursor.isNull(_cursorIndexOfReferenceNumber)) {
          _item.referenceNumber = null;
        } else {
          _item.referenceNumber = _cursor.getString(_cursorIndexOfReferenceNumber);
        }
        if (_cursor.isNull(_cursorIndexOfBank)) {
          _item.bank = null;
        } else {
          _item.bank = _cursor.getString(_cursorIndexOfBank);
        }
        if (_cursor.isNull(_cursorIndexOfTransactionType)) {
          _item.transactionType = null;
        } else {
          _item.transactionType = _cursor.getString(_cursorIndexOfTransactionType);
        }
        if (_cursor.isNull(_cursorIndexOfCategory)) {
          _item.category = null;
        } else {
          _item.category = _cursor.getString(_cursorIndexOfCategory);
        }
        final int _tmp;
        _tmp = _cursor.getInt(_cursorIndexOfIsDeleted);
        _item.isDeleted = _tmp != 0;
        _item.updatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
