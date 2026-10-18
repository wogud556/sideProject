package com.hanati.bank.mortgage.product;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mortgages/products")
@RequiredArgsConstructor
public class MortgageProductController {

    private final MortgageProductRepository productRepository;

    @GetMapping
    public ResponseEntity<List<MortgageProduct>> getProducts() {
        return ResponseEntity.ok(productRepository.findAll());
    }
}
