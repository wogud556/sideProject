package com.hanati.bank.bankEx.loan.mortgage.client;

import com.hanati.bank.bankEx.common.exception.BusinessException;
import com.hanati.bank.bankEx.common.exception.ErrorCode;
import com.hanati.bank.bankEx.loan.mortgage.client.MortgageCoreDtos.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Core/mortgage(8084) 고객용 API 호출. 담당자 API(평가·심사·승인·실행)는 부르지 않는다.
 * Core의 업무 오류는 MortgageCoreException으로, 연결 실패는 MORTGAGE_CORE_UNAVAILABLE로 바꾼다.
 */
@Component
public class MortgageCoreClient {

    private final RestClient restClient;

    public MortgageCoreClient(@Value("${mortgage.core.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public List<Product> getProducts() {
        return call(() -> restClient.get().uri("/products").retrieve()
                .body(new ParameterizedTypeReference<List<Product>>() {}));
    }

    public ApplicationDetail apply(ApplyCommand command) {
        return post("/applications", command, ApplicationDetail.class);
    }

    public List<ApplicationSummary> getApplications(String customerId) {
        return call(() -> restClient.get().uri("/customers/{customerId}/applications", customerId).retrieve()
                .body(new ParameterizedTypeReference<List<ApplicationSummary>>() {}));
    }

    public ApplicationDetail getApplication(Long applicationId) {
        return call(() -> restClient.get().uri("/applications/{id}", applicationId).retrieve()
                .body(ApplicationDetail.class));
    }

    public ApplicationDetail agree(Long applicationId, String customerId, String disbursementAccountNo, boolean consent) {
        return post("/applications/" + applicationId + "/agreements", Map.of(
                "customerId", customerId,
                "disbursementAccountNo", disbursementAccountNo,
                "consent", consent), ApplicationDetail.class);
    }

    public ApplicationDetail withdraw(Long applicationId, String customerId) {
        return post("/applications/" + applicationId + "/withdraw", Map.of("customerId", customerId),
                ApplicationDetail.class);
    }

    public List<LoanAccount> getLoanAccounts(String customerId) {
        return call(() -> restClient.get().uri("/customers/{customerId}/accounts", customerId).retrieve()
                .body(new ParameterizedTypeReference<List<LoanAccount>>() {}));
    }

    public LoanAccount getLoanAccount(Long loanAccountId) {
        return call(() -> restClient.get().uri("/accounts/{id}", loanAccountId).retrieve()
                .body(LoanAccount.class));
    }

    public List<Installment> getSchedule(Long loanAccountId) {
        return call(() -> restClient.get().uri("/accounts/{id}/repayments", loanAccountId).retrieve()
                .body(new ParameterizedTypeReference<List<Installment>>() {}));
    }

    public Repayment repay(Long loanAccountId, String customerId, long amount, String sourceAccountNo,
                           String idempotencyKey) {
        return post("/accounts/" + loanAccountId + "/repayments", Map.of(
                "customerId", customerId,
                "amount", amount,
                "sourceAccountNo", sourceAccountNo,
                "idempotencyKey", idempotencyKey), Repayment.class);
    }

    private <T> T post(String uri, Object body, Class<T> type) {
        return call(() -> restClient.post().uri(uri).contentType(MediaType.APPLICATION_JSON).body(body)
                .retrieve().body(type));
    }

    private <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientResponseException e) {
            ErrorBody error = e.getResponseBodyAs(ErrorBody.class);
            if (error == null || error.code() == null) {
                throw new BusinessException(ErrorCode.MORTGAGE_CORE_UNAVAILABLE);
            }
            throw new MortgageCoreException(error.code(), error.message());
        } catch (ResourceAccessException e) {
            throw new BusinessException(ErrorCode.MORTGAGE_CORE_UNAVAILABLE);
        }
    }
}
