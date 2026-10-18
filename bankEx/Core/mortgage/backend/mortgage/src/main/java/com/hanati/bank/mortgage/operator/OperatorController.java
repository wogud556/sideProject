package com.hanati.bank.mortgage.operator;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mortgages/operators")
@RequiredArgsConstructor
public class OperatorController {

    private final OperatorRepository operatorRepository;

    @GetMapping
    public ResponseEntity<List<Operator>> getOperators() {
        return ResponseEntity.ok(operatorRepository.findAll());
    }
}
