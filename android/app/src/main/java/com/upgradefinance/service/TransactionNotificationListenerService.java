package com.upgradefinance.service;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import com.upgradefinance.db.AppDatabase;
import com.upgradefinance.db.TransactionDao;
import com.upgradefinance.model.LocalTransaction;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.upgradefinance.utils.AppLogger;

public class TransactionNotificationListenerService extends NotificationListenerService {
    private static final String TAG = "NotificationListener";

    // Target UPI applications package IDs
    private static final String GPAY_PKG = "com.google.android.apps.nbu.paisa.user";
    private static final String PHONEPE_PKG = "com.phonepe.app";
    private static final String PAYTM_PKG = "net.one97.paytm";

    // Notification body matcher: e.g. "Paid Rs. 500 to Swiggy"
    private static final Pattern PAYMENT_PATTERN = Pattern.compile("(?i)(?:paid|sent|received|added|credited|debited)\\s*(?:rs\\.?|inr)\\s*([\\d,]+\\.?\\d*)");

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String pkg = sbn.getPackageName();
        
        Notification notification = sbn.getNotification();
        if (notification == null || notification.extras == null) return;

        CharSequence title = notification.extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text = notification.extras.getCharSequence(Notification.EXTRA_TEXT);

        if (text == null) return;
        
        String body = text.toString();
        String titleStr = title != null ? title.toString() : "";

        // Whitelist packages: payment apps OR SMS apps OR banking apps
        boolean isPaymentApp = GPAY_PKG.equals(pkg) || PHONEPE_PKG.equals(pkg) || PAYTM_PKG.equals(pkg);
        boolean isSmsApp = pkg.contains("messaging") || pkg.contains("mms") || pkg.contains("sms") || pkg.contains("telephony");
        boolean isBankApp = pkg.contains("kotak") || pkg.contains("kbank") || pkg.contains("msf") || pkg.contains("sbi") || pkg.contains("hdfc") || pkg.contains("icici") || pkg.contains("axis");

        if (!isPaymentApp && !isSmsApp && !isBankApp) {
            return; // Ignore other apps (e.g. WhatsApp, emails)
        }

        Log.d(TAG, "Notification intercepted. Pkg: " + pkg + " Title: " + titleStr + " Text: " + body);
        AppLogger.log("Notif: Intercepted " + (titleStr.isEmpty() ? pkg : titleStr));

        parseAndSaveNotification(body, titleStr, pkg);
    }

    private void parseAndSaveNotification(String body, String title, String pkg) {
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
                AppLogger.log("Notif Parser: Ignored (no amount found). Text: " + (body.length() > 35 ? body.substring(0, 35) + "..." : body));
                return;
            }

            // 2. Transaction type detection
            String lowerBody = body.toLowerCase();
            boolean isDebit = lowerBody.contains("paid") || lowerBody.contains("sent") || lowerBody.contains("debited") || lowerBody.contains("spent");
            boolean isCredit = lowerBody.contains("credited") || lowerBody.contains("received") || lowerBody.contains("added");

            if (!isDebit && !isCredit) {
                AppLogger.log("Notif Parser: Ignored (not debit/credit). Text: " + (body.length() > 35 ? body.substring(0, 35) + "..." : body));
                return;
            }

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

            // Clean up merchant name (truncate if too long or trailing dots/dashes)
            if (merchant.length() > 30) {
                merchant = merchant.substring(0, 30).trim();
            }

            // Estimate Category
            String category = "Other";
            if (merchant.toLowerCase().contains("swiggy") || merchant.toLowerCase().contains("zomato")) category = "Food";
            else if (merchant.toLowerCase().contains("amazon") || merchant.toLowerCase().contains("flipkart")) category = "Shopping";
            else if (merchant.toLowerCase().contains("uber") || merchant.toLowerCase().contains("rapido")) category = "Travel";

            final String finalMerchant = merchant;
            final String finalCategory = category;
            final double finalAmount = amount;
            final boolean finalIsDebit = isDebit;
            
            // Extract bank name
            String bankName = "UPI";
            if (pkg.contains("paisa")) bankName = "Google Pay";
            else if (pkg.contains("phonepe")) bankName = "PhonePe";
            else if (pkg.contains("paytm")) bankName = "Paytm";
            else if (title != null && !title.isEmpty()) {
                if (title.contains("KOTAK")) bankName = "Kotak Bank";
                else if (title.contains("SBI")) bankName = "SBI";
                else if (title.contains("HDFC")) bankName = "HDFC";
                else if (title.contains("ICICI")) bankName = "ICICI";
                else if (title.contains("AXIS")) bankName = "Axis";
                else bankName = title; // fallback to sender code
            }
            final String finalBank = bankName;

            // Extract reference number
            String refNum = null;
            Pattern refPattern = Pattern.compile("(?i)(?:ref|upi|txn|id|reference)\\.?\\s*(?:no\\.?|num\\.?|number)?\\s*(\\d{12}|\\d{6,})");
            Matcher refMatcher = refPattern.matcher(body);
            if (refMatcher.find()) {
                refNum = refMatcher.group(1);
            }
            final String finalRefNum = refNum != null ? refNum : "NOTIF_" + (System.currentTimeMillis() / 1000) + "_" + (int)(amount);

            Executors.newSingleThreadExecutor().execute(() -> {
                AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
                TransactionDao dao = db.transactionDao();

                // Duplicate check via RefNum
                LocalTransaction existing = null;
                if (finalRefNum != null) {
                    existing = dao.getTransactionByRefNum(finalRefNum);
                }
                if (existing == null) {
                    existing = dao.findDuplicateFuzzy(finalAmount, System.currentTimeMillis());
                }

                if (existing != null) {
                    Log.d(TAG, "Duplicate transaction detected. Skipping.");
                    AppLogger.log("Notif Parser: Duplicate skipped (Amount: " + finalAmount + ")");
                    return;
                }

                // Build a LocalTransaction item
                LocalTransaction tx = new LocalTransaction();
                tx.id = UUID.randomUUID().toString();
                tx.amount = finalAmount;
                tx.timestamp = System.currentTimeMillis();
                tx.merchant = finalMerchant;
                tx.category = finalCategory;
                tx.transactionType = finalIsDebit ? "DEBIT" : "CREDIT";
                tx.bank = finalBank;
                tx.isDeleted = false;
                tx.updatedAt = System.currentTimeMillis();
                tx.referenceNumber = finalRefNum;

                dao.insertOrReplace(tx);
                Log.d(TAG, "Saved push transaction: ₹" + finalAmount + " for " + finalMerchant);
                AppLogger.log("Notif Parser: Saved ₹" + finalAmount + " (" + finalMerchant + ") to local DB");
            });

        } catch (Exception e) {
            Log.e(TAG, "Error matching notification regex", e);
            AppLogger.log("Notif Parser Error: " + e.getMessage());
        }
    }

    @Override
    public void onListenerConnected() {
        Log.d(TAG, "Notification listener connected successfully.");
        AppLogger.log("Notif Service: Connected!");
    }

    @Override
    public void onListenerDisconnected() {
        Log.d(TAG, "Notification listener disconnected.");
        AppLogger.log("Notif Service: Disconnected!");
    }
}
