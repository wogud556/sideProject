package com.hanati.bank.refinance.customer.controller;

import com.hanati.bank.refinance.customer.dto.CustomerResponse;
import com.hanati.bank.refinance.customer.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public List<CustomerResponse> search(
            @RequestParam(required = false) String customerNo,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthDate,
            @RequestParam(required = false) String phone,
            @RequestHeader("X-Operator-Id") String operatorId) {
        return customerService.search(customerNo, name, birthDate, phone, operatorId);
    }

    @GetMapping("/{customerId}")
    public CustomerResponse get(@PathVariable Long customerId,
                                 @RequestHeader("X-Operator-Id") String operatorId) {
        return customerService.get(customerId, operatorId);
    }
}
