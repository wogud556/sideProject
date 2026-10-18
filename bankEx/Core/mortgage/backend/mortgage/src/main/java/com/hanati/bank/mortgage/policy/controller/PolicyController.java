package com.hanati.bank.mortgage.policy.controller;

import com.hanati.bank.mortgage.policy.dto.PolicyCreateRequest;
import com.hanati.bank.mortgage.policy.entity.MortgagePolicy;
import com.hanati.bank.mortgage.policy.service.PolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mortgages/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    @GetMapping
    public ResponseEntity<List<MortgagePolicy>> getPolicies() {
        return ResponseEntity.ok(policyService.getPolicies());
    }

    @PostMapping
    public ResponseEntity<MortgagePolicy> create(@RequestHeader("X-Operator-Id") String operatorId,
                                                 @Valid @RequestBody PolicyCreateRequest req) {
        return ResponseEntity.ok(policyService.create(operatorId, req));
    }
}
