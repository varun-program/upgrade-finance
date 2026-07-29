package com.upgradefinance.service;

import com.upgradefinance.model.Transaction;
import com.upgradefinance.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiAssistantService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Autowired
    private TransactionRepository transactionRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    public String getAiResponse(com.upgradefinance.model.User user, String userQuery) {
        List<Transaction> txList = transactionRepository.findByUserAndIsDeletedFalse(user);

        // Standard rules context fallback
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty()) {
            return generateLocalFallbackResponse(txList, userQuery);
        }

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey;

            // Format transactions summary to keep context window small and token efficient
            String txSummary = txList.stream()
                    .map(t -> String.format("- %s: %.2f on %s (%s)", 
                            t.getTransactionType(), t.getAmount(), t.getMerchant(), t.getCategory()))
                    .limit(100) // limit to recent 100 transactions to save tokens
                    .collect(Collectors.joining("\n"));

            String prompt = "You are Antigravity AI, the financial assistant for the 'Upgrade Finance' app. " +
                    "Here is a summary of the user's recent transactions:\n" + txSummary + "\n\n" +
                    "Please answer their question in a friendly, concise, and helpful manner using the transaction data. " +
                    "Here is the user's question: \"" + userQuery + "\"";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> part = new HashMap<>();
            part.put("text", prompt);
            Map<String, Object> content = new HashMap<>();
            content.put("parts", Collections.singletonList(part));
            requestBody.put("contents", Collections.singletonList(content));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

            if (response != null && response.containsKey("candidates")) {
                List<?> candidates = (List<?>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<?, ?> firstCandidate = (Map<?, ?>) candidates.get(0);
                    Map<?, ?> contentMap = (Map<?, ?>) firstCandidate.get("content");
                    List<?> partsList = (List<?>) contentMap.get("parts");
                    if (!partsList.isEmpty()) {
                        Map<?, ?> firstPart = (Map<?, ?>) partsList.get(0);
                        return (String) firstPart.get("text");
                    }
                }
            }
        } catch (Exception e) {
            return "Unable to connect to Gemini API. Error: " + e.getMessage() + "\n\n" + generateLocalFallbackResponse(txList, userQuery);
        }

        return generateLocalFallbackResponse(txList, userQuery);
    }

    private String generateLocalFallbackResponse(List<Transaction> txList, String query) {
        String q = query.toLowerCase();
        double totalExpense = txList.stream().filter(t -> "DEBIT".equals(t.getTransactionType())).mapToDouble(Transaction::getAmount).sum();
        double totalIncome = txList.stream().filter(t -> "CREDIT".equals(t.getTransactionType())).mapToDouble(Transaction::getAmount).sum();

        if (q.contains("spend") || q.contains("expense") || q.contains("spent")) {
            // Find biggest expense
            Optional<Transaction> biggest = txList.stream()
                    .filter(t -> "DEBIT".equals(t.getTransactionType()))
                    .max(Comparator.comparingDouble(Transaction::getAmount));

            String biggestStr = biggest.map(transaction -> String.format("Your biggest single expense was ₹%.2f at %s.", 
                    transaction.getAmount(), transaction.getMerchant())).orElse("No expenses tracked yet.");

            return String.format("Locally Analyzed Insights:\n" +
                    "• Total Expenses: ₹%.2f\n" +
                    "• %s\n" +
                    "• Tip: Setup a free Gemini API Key in application.properties or your system environment variables to unlock the conversational AI assistant!",
                    totalExpense, biggestStr);
        } else if (q.contains("income") || q.contains("salary")) {
            return String.format("Locally Analyzed Insights:\n" +
                    "• Total Income: ₹%.2f\n" +
                    "• Net Balance: ₹%.2f",
                    totalIncome, (totalIncome - totalExpense));
        }

        return "Welcome to Upgrade Finance Offline AI Assistant!\n" +
                "To get precise conversational insights, set your `GEMINI_API_KEY` in environment variables. " +
                "In offline mode, I can help you with basic questions containing 'spend' or 'income'.";
    }

    public String suggestCategory(String merchant) {
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty()) {
            return "Unknown";
        }
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey;
            String prompt = "Given the merchant name '" + merchant + "', predict its single expense category. " +
                    "Choose exactly from: [Food, Travel, Shopping, Fuel, Entertainment, Healthcare, Bills, Education, Savings, Unknown]. " +
                    "Respond with ONLY the category word and nothing else.";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> part = new HashMap<>();
            part.put("text", prompt);
            Map<String, Object> content = new HashMap<>();
            content.put("parts", Collections.singletonList(part));
            requestBody.put("contents", Collections.singletonList(content));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

            if (response != null && response.containsKey("candidates")) {
                List<?> candidates = (List<?>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<?, ?> firstCandidate = (Map<?, ?>) candidates.get(0);
                    Map<?, ?> contentMap = (Map<?, ?>) firstCandidate.get("content");
                    List<?> partsList = (List<?>) contentMap.get("parts");
                    if (!partsList.isEmpty()) {
                        Map<?, ?> firstPart = (Map<?, ?>) partsList.get(0);
                        return ((String) firstPart.get("text")).trim();
                    }
                }
            }
        } catch (Exception e) {
            // Ignore and return Unknown
        }
        return "Unknown";
    }
}
