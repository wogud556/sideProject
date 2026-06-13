package com.hanati.bank.screening.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class SignupRequest {
    @NotBlank
    private String userId;
    @NotBlank
    private String password;
    @NotBlank
    private String userName;
    private LocalDate birthDate;
    private String phoneNumber;
}
