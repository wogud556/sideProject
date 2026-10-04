package com.hanati.bank.bankEx.deposit.transfer;

import tools.jackson.databind.ObjectMapper;
import com.hanati.bank.bankEx.deposit.general.dto.DepositRequest;
import com.hanati.bank.bankEx.deposit.general.service.transService;
import com.hanati.bank.bankEx.login.dto.SignupRequest;
import com.hanati.bank.bankEx.login.dto.SignupResponse;
import com.hanati.bank.bankEx.login.service.loginService;
import com.hanati.bank.bankEx.deposit.transfer.dto.TransferRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 이 프로젝트 최초의 MockMvc 기반 API 통합 테스트.
 * 서비스 레벨 테스트(TransferFlowTests 등)와 달리 HTTP 요청/응답(JSON)까지 검증한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class TransferControllerApiTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private loginService loginService;
    @Autowired
    private transService transService;

    private String signup(String userId) {
        SignupResponse response = loginService.signup(new SignupRequest(userId, "pw1234", "홍길동", "01012345678", "1234"));
        return response.getAccountNumber();
    }

    @Test
    void postTransfer_success_returns200WithCompletedResult() throws Exception {
        String withdrawalUser = "apiWithdraw1";
        String depositUser = "apiDeposit1";
        String withdrawalAccount = signup(withdrawalUser);
        String depositAccount = signup(depositUser);
        transService.deposit(withdrawalAccount, new DepositRequest(100_000L, "초기입금"));

        TransferRequest request = new TransferRequest("TRF-API-001", withdrawalUser, withdrawalAccount,
                depositAccount, 30_000L, "1234", "생활비", "홍길동");

        mockMvc.perform(post("/api/bank/user/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.amount").value(30_000))
                .andExpect(jsonPath("$.balanceAfterTransfer").value(70_000))
                .andExpect(jsonPath("$.transactionNumber").exists())
                .andExpect(jsonPath("$.transferId").exists());
    }

    @Test
    void getHolder_returnsMaskedAccountHolderName() throws Exception {
        String accountNumber = signup("apiHolder1");

        mockMvc.perform(get("/api/bank/user/transfers/accounts/{accountNumber}/holder", accountNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value(accountNumber))
                .andExpect(jsonPath("$.accountHolderName").value("홍*동"))
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"));
    }

    @Test
    void getTransfer_afterSuccessfulTransfer_returnsTransferResult() throws Exception {
        String withdrawalUser = "apiWithdraw2";
        String depositUser = "apiDeposit2";
        String withdrawalAccount = signup(withdrawalUser);
        String depositAccount = signup(depositUser);
        transService.deposit(withdrawalAccount, new DepositRequest(50_000L, "초기입금"));

        TransferRequest request = new TransferRequest("TRF-API-002", withdrawalUser, withdrawalAccount,
                depositAccount, 20_000L, "1234", "생활비", "홍길동");

        String responseBody = mockMvc.perform(post("/api/bank/user/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long transferId = objectMapper.readTree(responseBody).get("transferId").asLong();

        mockMvc.perform(get("/api/bank/user/transfers/{transferId}", transferId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.withdrawalAccountNumber").value(withdrawalAccount))
                .andExpect(jsonPath("$.depositAccountNumber").value(depositAccount))
                .andExpect(jsonPath("$.depositAccountHolderName").value("홍*동"))
                .andExpect(jsonPath("$.amount").value(20_000))
                .andExpect(jsonPath("$.balanceAfterTransfer").value(30_000))
                .andExpect(jsonPath("$.transferStatus").value("COMPLETED"));
    }

    @Test
    void postTransfer_insufficientBalance_returns400WithErrorCode() throws Exception {
        String withdrawalUser = "apiWithdraw3";
        String depositUser = "apiDeposit3";
        String withdrawalAccount = signup(withdrawalUser);
        String depositAccount = signup(depositUser);
        transService.deposit(withdrawalAccount, new DepositRequest(1_000L, "초기입금"));

        TransferRequest request = new TransferRequest("TRF-API-003", withdrawalUser, withdrawalAccount,
                depositAccount, 50_000L, "1234", "생활비", "홍길동");

        mockMvc.perform(post("/api/bank/user/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_BALANCE"));
    }
}
