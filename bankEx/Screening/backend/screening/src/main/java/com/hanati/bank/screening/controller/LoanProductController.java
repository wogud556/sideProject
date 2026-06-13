package com.hanati.bank.screening.controller;

import com.hanati.bank.screening.dto.LoanProductResponse;
import com.hanati.bank.screening.service.LoanProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/screening/products")
@RequiredArgsConstructor
public class LoanProductController {

    private final LoanProductService loanProductService;

    @GetMapping
    public ResponseEntity<List<LoanProductResponse>> getProducts() {
        return ResponseEntity.ok(loanProductService.getActiveProducts());
    }

    @GetMapping("/{productId}")
    public ResponseEntity<LoanProductResponse> getProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(loanProductService.getProduct(productId));
    }
}
