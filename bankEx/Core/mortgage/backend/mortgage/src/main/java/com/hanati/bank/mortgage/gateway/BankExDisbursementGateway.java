package com.hanati.bank.mortgage.gateway;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

/**
 * bankEx 내부 API(POST /api/internal/mortgage/disbursements)로 실행금을 입금한다.
 * bankEx는 idempotencyKey를 수신 거래번호로 써서 같은 키의 두 번째 요청에는 처음 거래를 돌려준다.
 */
@Component
@ConditionalOnProperty(name = "mortgage.gateway.mode", havingValue = "bankex", matchIfMissing = true)
public class BankExDisbursementGateway implements DisbursementGateway {

    private final RestClient restClient;

    public BankExDisbursementGateway(@Value("${mortgage.bankex.base-url}") String baseUrl,
                                     @Value("${mortgage.bankex.internal-token}") String internalToken) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Internal-Token", internalToken)
                .build();
    }

    @Override
    public String disburse(DisbursementCommand command) {
        try {
            DisbursementResponse response = restClient.post()
                    .uri("/api/internal/mortgage/disbursements")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "idempotencyKey", command.idempotencyKey(),
                            "accountNumber", command.accountNo(),
                            "amount", command.amount(),
                            "description", command.description()))
                    .retrieve()
                    .body(DisbursementResponse.class);
            return response.transactionNumber();
        } catch (RestClientResponseException e) {
            ErrorBody error = e.getResponseBodyAs(ErrorBody.class);
            throw new DisbursementFailedException(error != null && error.message() != null
                    ? error.message() : "bankEx 입금 요청이 거부되었습니다. (HTTP " + e.getStatusCode().value() + ")");
        } catch (ResourceAccessException e) {
            throw new DisbursementFailedException("bankEx에 연결할 수 없습니다.");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DisbursementResponse(String transactionNumber) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ErrorBody(String code, String message) {
    }
}
