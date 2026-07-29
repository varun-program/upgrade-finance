package com.upgradefinance.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import com.upgradefinance.db.AppDatabase;
import com.upgradefinance.db.TransactionDao;
import com.upgradefinance.model.LocalTransaction;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SMSReceiver extends BroadcastReceiver {
    private static final String TAG = "SMSReceiver";

    // Simple robust regex matching debit/credit actions
    private static final Pattern DEBIT_PATTERN = Pattern.compile("(?i)(?:debited|spent|sent|paid)\\s*(?:rs\\.?|inr)\\s*([\\d,]+\\.?\\d*)");
    private static final Pattern CREDIT_PATTERN = Pattern.compile("(?i)(?:credited|received|added)\\s*(?:rs\\.?|inr)\\s*([\\d,]+\\.?\\d*)");
    private static final Pattern REF_PATTERN = Pattern.compile("(?i)(?:ref|upi|txn|id)\\.?\\s*(\\d{12}|\\d{6,})");

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!"android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) return;

        Bundle bundle = intent.getExtras();
        if (bundle == null) return;

        try {
            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus == null) return;

            for (Object pdu : pdus) {
                SmsMessage message = SmsMessage.createFromPdu((byte[]) pdu, bundle.getString("format"));
                String sender = message.getOriginatingAddress();
                String body = message.getMessageBody();

                Log.d(TAG, "SMS Received from: " + sender + " Body: " + body);
                
                // Parse transaction from body text
                parseAndSaveTransaction(context, body, sender);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing SMS", e);
        }
    }

    public void parseAndSaveTransaction(Context context, String body, String sender) {
        boolean isDebit = DEBIT_PATTERN.matcher(body).find();
        boolean isCredit = CREDIT_PATTERN.matcher(body).find();
        
        if (!isDebit && !isCredit) return; // Not a payment message

        Matcher amountMatcher = isDebit ? DEBIT_PATTERN.matcher(body) : CREDIT_PATTERN.matcher(body);
        if (!amountMatcher.find()) return;

        try {
            double amount = Double.parseDouble(amountMatcher.group(1).replace(",", ""));
            
            // Extract reference number
            String refNum = null;
            Matcher refMatcher = REF_PATTERN.matcher(body);
            if (refMatcher.find()) {
                refNum = refMatcher.group(1);
            }

            // Estimate merchant name from SMS keywords
            String merchant = "Unknown Merchant";
            if (body.contains("Swiggy") || body.contains("swiggy")) merchant = "Swiggy";
            else if (body.contains("Zomato") || body.contains("zomato")) merchant = "Zomato";
            else if (body.contains("Amazon") || body.contains("amazon")) merchant = "Amazon";
            else if (body.contains("Flipkart") || body.contains("flipkart")) merchant = "Flipkart";
            else if (body.contains("Uber") || body.contains("uber")) merchant = "Uber";
            else if (body.contains("Rapido") || body.contains("rapido")) merchant = "Rapido";

            // Extract Category
            String category = "Other";
            if (merchant.equals("Swiggy") || merchant.equals("Zomato")) category = "Food";
            else if (merchant.equals("Amazon") || merchant.equals("Flipkart")) category = "Shopping";
            else if (merchant.equals("Uber") || merchant.equals("Rapido")) category = "Travel";

            final String finalRefNum = refNum;
            final double finalAmount = amount;
            final String finalMerchant = merchant;
            final String finalCategory = category;
            final String finalTxType = isDebit ? "DEBIT" : "CREDIT";
            
            String bankName = "Unknown Bank";
            if (sender != null) {
                if (sender.contains("SBI")) bankName = "SBI";
                else if (sender.contains("HDFC")) bankName = "HDFC";
                else if (sender.contains("ICICI")) bankName = "ICICI";
                else if (sender.contains("AXIS")) bankName = "Axis";
            }
            final String finalBank = bankName;

            Executors.newSingleThreadExecutor().execute(() -> {
                AppDatabase db = AppDatabase.getDatabase(context);
                TransactionDao dao = db.transactionDao();

                // 14. Duplicate Detection (check if already saved by push notification listener)
                if (finalRefNum != null) {
                    LocalTransaction existing = dao.getTransactionByRefNum(finalRefNum);
                    if (existing != null) {
                        Log.d(TAG, "Duplicate detected via RefNum: " + finalRefNum + ". Skipping.");
                        return;
                    }
                }

                LocalTransaction tx = new LocalTransaction();
                tx.id = UUID.randomUUID().toString();
                tx.amount = finalAmount;
                tx.timestamp = System.currentTimeMillis();
                tx.merchant = finalMerchant;
                tx.referenceNumber = finalRefNum;
                tx.category = finalCategory;
                tx.transactionType = finalTxType;
                tx.bank = finalBank;
                tx.isDeleted = false;
                tx.updatedAt = System.currentTimeMillis();

                dao.insertOrReplace(tx);
                Log.d(TAG, "Successfully parsed and saved SMS transaction: ₹" + finalAmount);
            });

        } catch (Exception e) {
            Log.e(TAG, "Failed parsing transaction metrics", e);
        }
    }
}
