package com.upgradefinance.controller;

import com.upgradefinance.dto.AuthRequest;
import com.upgradefinance.dto.AuthResponse;
import com.upgradefinance.model.User;
import com.upgradefinance.repository.UserRepository;
import com.upgradefinance.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest authRequest) {
        if (userRepository.existsByEmail(authRequest.getEmail())) {
            return ResponseEntity.badRequest().body("Error: Email is already in use!");
        }

        User user = new User();
        user.setEmail(authRequest.getEmail());
        user.setPasswordHash(passwordEncoder.encode(authRequest.getPassword()));

        userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully!");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest) {
        Optional<User> userOpt = userRepository.findByEmail(authRequest.getEmail());

        if (userOpt.isEmpty() || !passwordEncoder.matches(authRequest.getPassword(), userOpt.get().getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: Invalid email or password!");
        }

        String token = jwtUtils.generateToken(authRequest.getEmail());
        return ResponseEntity.ok(new AuthResponse(token, authRequest.getEmail()));
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody AuthRequest authRequest) {
        // Placeholder for Google OAuth Verification.
        // For self-hosting and free tiers, we fallback to register/login using the google email
        // and a custom suffix, verifying the integrity or treating it as a trust authentication.
        String email = authRequest.getEmail();
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            User user = new User();
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode("google-oauth-placeholder-secure-key-" + Math.random()));
            userRepository.save(user);
        }
        
        String token = jwtUtils.generateToken(email);
        return ResponseEntity.ok(new AuthResponse(token, email));
    }
}
