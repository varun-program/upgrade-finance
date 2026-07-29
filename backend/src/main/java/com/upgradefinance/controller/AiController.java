package com.upgradefinance.controller;

import com.upgradefinance.model.User;
import com.upgradefinance.repository.UserRepository;
import com.upgradefinance.service.AiAssistantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AiAssistantService aiAssistantService;

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody Map<String, String> request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        String userQuery = request.get("message");
        if (userQuery == null || userQuery.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Message cannot be empty");
        }

        String aiResponse = aiAssistantService.getAiResponse(user, userQuery);
        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("response", aiResponse);
        return ResponseEntity.ok(responseBody);
    }

    @GetMapping("/categorize")
    public ResponseEntity<?> categorize(@RequestParam("merchant") String merchant) {
        String category = aiAssistantService.suggestCategory(merchant);
        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("category", category);
        return ResponseEntity.ok(responseBody);
    }
}
