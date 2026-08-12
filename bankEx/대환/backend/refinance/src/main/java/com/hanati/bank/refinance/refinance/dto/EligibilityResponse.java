package com.hanati.bank.refinance.refinance.dto;

import java.util.List;

public record EligibilityResponse(boolean eligible, List<EligibilityResult> results) {
}
