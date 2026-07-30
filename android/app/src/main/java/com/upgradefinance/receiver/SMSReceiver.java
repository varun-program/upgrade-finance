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
import com.upgradefinance.utils.AppLogger;

public class SMSReceiver extends BroadcastReceiver {
    private static final String TAG = "SMSReceiver";

    // Simple robust regex matching debit/credit actions
    private static final Pattern DEBIT_PATTERN = Pattern.compile("(?i)(?:debited|spent|sent|paid)\\s*(?:rs\\.?|inr)\\s*([\\d,]+\\.?\\d*)");
    private static final Pattern CREDIT_PATTERN = Pattern.compile("(?i)(?:credited|received|added)\\s*(?:rs\\.?|inr)\\s*([\\d,]+\\.?\\d*)");
    private static final Pattern REF_PATTERN = Pattern.compile("(?i)(?:ref|upi|txn|id|reference)\\.?\\s*(?:no\\.?|num\\.?|number)?\\s*(\\d{12}|\\d{6,})");

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
                AppLogger.log("SMS: Received from " + sender);
                
                // Parse transaction from body text
                parseAndSaveTransaction(context, body, sender);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing SMS", e);
        }
    }

    public void parseAndSaveTransaction(Context context, String body, String sender) {
        try {
            double amount = -1;

            // 1. Robust amount extraction supporting Rs, INR, ₹, and currency placement
            Pattern amountPattern = Pattern.compile("(?i)(?:rs\\.?|inr|₹)\\s*([\\d,]+\\.?\\d*)");
            Matcher amountMatcher = amountPattern.matcher(body);
            if (amountMatcher.find()) {
                amount = Double.parseDouble(amountMatcher.group(1).replace(",", ""));
            } else {
                Pattern altAmountPattern = Pattern.compile("(?i)([\\d,]+\\.?\\d*)\\s*(?:rs\\.?|inr|₹)");
                Matcher altMatcher = altAmountPattern.matcher(body);
                if (altMatcher.find()) {
                    amount = Double.parseDouble(altMatcher.group(1).replace(",", ""));
                }
            }

            if (amount <= 0) {
                AppLogger.log("SMS Parser: Ignored (no amount found). Body: " + (body.length() > 30 ? body.substring(0, 30) + "..." : body));
                return;
            }

            // 2. Transaction type detection
            String lowerBody = body.toLowerCase();
            boolean isDebit = lowerBody.contains("paid") || lowerBody.contains("sent") || lowerBody.contains("debited") || lowerBody.contains("spent");
            boolean isCredit = lowerBody.contains("credited") || lowerBody.contains("received") || lowerBody.contains("added");

            if (!isDebit && !isCredit) {
                AppLogger.log("SMS Parser: Ignored (not debit/credit). Body: " + (body.length() > 30 ? body.substring(0, 30) + "..." : body));
                return;
            }
            
            // Extract reference number
            String refNum = null;
            Matcher refMatcher = REF_PATTERN.matcher(body);
            if (refMatcher.find()) {
                refNum = refMatcher.group(1);
            }

            // Estimate merchant name from SMS keywords
            String merchant = "Unknown Merchant";
            if (body.contains("to ")) {
                int start = body.indexOf("to ") + 3;
                String rawMerchant = body.substring(start).trim();
                int onIndex = rawMerchant.indexOf("on ");
                if (onIndex > 0) {
                    merchant = rawMerchant.substring(0, onIndex).trim();
                } else {
                    merchant = rawMerchant;
                }
            } else if (body.contains("at ")) {
                int start = body.indexOf("at ") + 3;
                merchant = body.substring(start).trim();
            }

            if (merchant.equals("Unknown Merchant")) {
                if (body.contains("Swiggy") || body.contains("swiggy")) merchant = "Swiggy";
                else if (body.contains("Zomato") || body.contains("zomato")) merchant = "Zomato";
                else if (body.contains("Amazon") || body.contains("amazon")) merchant = "Amazon";
                else if (body.contains("Flipkart") || body.contains("flipkart")) merchant = "Flipkart";
                else if (body.contains("Uber") || body.contains("uber")) merchant = "Uber";
                else if (body.contains("Rapido") || body.contains("rapido")) merchant = "Rapido";
            }

            // Clean up merchant name (truncate if too long or trailing dots/dashes)
            if (merchant.length() > 30) {
                merchant = merchant.substring(0, 30).trim();
            }

            // Extract Category
            String category = "Other";
            if (merchant.toLowerCase().contains("swiggy") || merchant.toLowerCase().contains("zomato")) category = "Food";
            else if (merchant.toLowerCase().contains("amazon") || merchant.toLowerCase().contains("flipkart")) category = "Shopping";
            else if (merchant.toLowerCase().contains("uber") || merchant.toLowerCase().contains("rapido")) category = "Travel";

            final String finalRefNum = refNum;
            final double finalAmount = amount;
            final String finalMerchant = merchant;
            final String finalCategory = category;
            final String finalTxType = isDebit ? "DEBIT" : "CREDIT";
            
            String bankName = "Unknown Bank";
            if (sender != null) {
                if (sender.contains("KOTAK")) bankName = "Kotak Bank";
                else if (sender.contains("SBI")) bankName = "SBI";
                else if (sender.contains("HDFC")) bankName = "HDFC";
                else if (sender.contains("ICICI")) bankName = "ICICI";
                else if (sender.contains("AXIS")) bankName = "Axis";
                else bankName = sender;
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
                        AppLogger.log("SMS Parser: Duplicate skipped (Ref: " + finalRefNum + ")");
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
                AppLogger.log("SMS Parser: Saved ₹" + finalAmount + " (" + finalMerchant + ") to local DB");
            });

        } catch (Exception e) {
            Log.e(TAG, "Failed parsing transaction metrics", e);
            AppLogger.log("SMS Parser Error: " + e.getMessage());
        }
    }
}
