package com.hanati.bank.bankEx.login.controller;

import com.hanati.bank.bankEx.login.dto.LoginRequest;
import com.hanati.bank.bankEx.login.dto.LoginResponse;
import com.hanati.bank.bankEx.login.dto.SignupRequest;
import com.hanati.bank.bankEx.login.dto.SignupResponse;
import com.hanati.bank.bankEx.login.service.loginService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/bank/user")
@RequiredArgsConstructor
public class loginController {

    private final loginService loginService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = loginService.login(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        try {
            SignupResponse response = loginService.signup(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestParam String userId) {
        try {
            return ResponseEntity.ok(loginService.getProfile(userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
