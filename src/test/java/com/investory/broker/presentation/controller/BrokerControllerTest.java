package com.investory.broker.presentation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.investory.broker.domain.model.BrokerConnectionFixture;
import com.investory.broker.domain.model.BrokerProviderFixture;
import com.investory.broker.domain.ports.FakeBrokerFeedPort;
import com.investory.broker.domain.ports.FakeHoldingDetailPort;
import com.investory.broker.domain.ports.FakeHoldingIngestionPort;
import com.investory.broker.domain.ports.FakeHoldingSummaryPort;
import com.investory.broker.domain.ports.FakeTradeIngestionPort;
import com.investory.broker.domain.ports.dto.AccountHoldingsInfo;
import com.investory.broker.domain.ports.dto.HoldingDetailInfo;
import com.investory.broker.domain.ports.dto.HoldingSummaryInfo;
import com.investory.broker.domain.repositories.FakeAccountSyncBatchRepository;
import com.investory.broker.domain.repositories.FakeBrokerConnectionRepository;
import com.investory.broker.domain.repositories.FakeBrokerProviderRepository;
import com.investory.broker.domain.repositories.FakeInvestmentAccountRepository;
import com.investory.broker.domain.services.BrokerConnectionService;
import com.investory.broker.domain.services.BrokerProviderService;
import com.investory.broker.domain.services.InvestmentAccountService;
import com.investory.broker.infra.exception.BrokerInfraException;
import com.investory.global.error.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BrokerControllerTest {

    // 컨트롤러 파라미터의 @AuthenticationPrincipal Long userId가 SecurityContext의 principal(=userId)을
    // 읽어오므로, standaloneSetup 테스트에서는 리졸버 등록과 인증 정보 세팅을 직접 해줘야 한다.
    // 기존 픽스처들이 전부 userId=1L 기준으로 데이터를 넣어뒀으므로 그 값을 그대로 쓴다.
    @BeforeEach
    void setUpAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null, List.of()));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    private static BrokerConnectionService newConnectionService(FakeBrokerConnectionRepository repository) {
        return newConnectionService(repository, new FakeBrokerProviderRepository());
    }

    private static BrokerConnectionService newConnectionService(
            FakeBrokerConnectionRepository repository, FakeBrokerProviderRepository providerRepository) {
        return new BrokerConnectionService(
                repository,
                providerRepository,
                new FakeInvestmentAccountRepository(),
                new FakeAccountSyncBatchRepository(),
                new FakeTradeIngestionPort(),
                new FakeHoldingIngestionPort(),
                new FakeBrokerFeedPort()
        );
    }

    private static BrokerProviderService newProviderService(
            com.investory.broker.domain.repositories.BrokerProviderRepository repository) {
        return new BrokerProviderService(repository, new FakeBrokerFeedPort());
    }

    private static InvestmentAccountService newAccountService() {
        return newAccountService(new FakeInvestmentAccountRepository(), new FakeBrokerConnectionRepository());
    }

    private static InvestmentAccountService newAccountService(
            FakeInvestmentAccountRepository accountRepository, FakeBrokerConnectionRepository connectionRepository) {
        return new InvestmentAccountService(
                accountRepository, connectionRepository, new FakeHoldingSummaryPort(), new FakeHoldingDetailPort());
    }

    private static InvestmentAccountService newAccountService(
            FakeInvestmentAccountRepository accountRepository,
            FakeBrokerConnectionRepository connectionRepository,
            FakeHoldingDetailPort holdingDetailPort) {
        return new InvestmentAccountService(
                accountRepository, connectionRepository, new FakeHoldingSummaryPort(), holdingDetailPort);
    }

    @Test
    void 지원_증권사_목록을_반환한다() throws Exception {
        FakeBrokerProviderRepository repository = new FakeBrokerProviderRepository();
        repository.add(BrokerProviderFixture.provider(1L, "S9990001A", "미래에셋증권(모의)"));
        BrokerProviderService providerService = newProviderService(repository);
        BrokerConnectionService connectionService = newConnectionService(new FakeBrokerConnectionRepository());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new BrokerController(providerService, connectionService, newAccountService())).setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();

        MvcResult result = mockMvc.perform(get("/broker/providers"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = new ObjectMapper()
                .readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        JsonNode provider = json.get("providers").get(0);

        assertEquals(1, json.get("providers").size());
        assertEquals(1, provider.get("brokerId").asLong());
        assertEquals("S9990001A", provider.get("brokerCode").asText());
        assertEquals("미래에셋증권(모의)", provider.get("brokerName").asText());
    }

    @Test
    void 인프라_예외는_GlobalExceptionHandler_응답_포맷으로_변환된다() throws Exception {
        BrokerProviderService providerService = newProviderService(new FailingBrokerProviderRepository());
        BrokerConnectionService connectionService = newConnectionService(new FakeBrokerConnectionRepository());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new BrokerController(providerService, connectionService, newAccountService()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();

        MvcResult result = mockMvc.perform(get("/broker/providers"))
                .andExpect(status().isInternalServerError())
                .andReturn();

        JsonNode json = new ObjectMapper()
                .readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertEquals("INTERNAL_ERROR", json.get("errorCode").asText());
        assertTrue(json.get("fieldErrors") == null || json.get("fieldErrors").isEmpty());
    }

    @Test
    void 연동된_증권사_목록을_반환한다() throws Exception {
        BrokerProviderService providerService = newProviderService(new FakeBrokerProviderRepository());
        FakeBrokerConnectionRepository repository = new FakeBrokerConnectionRepository();
        repository.add(1L, BrokerConnectionFixture.connected(15L, 1L, "S9990001A", "미래에셋증권(모의)"));
        BrokerConnectionService connectionService = newConnectionService(repository);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new BrokerController(providerService, connectionService, newAccountService())).setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();

        MvcResult result = mockMvc.perform(get("/broker/connections"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = new ObjectMapper()
                .readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        JsonNode connection = json.get("connections").get(0);

        assertEquals(1, json.get("connections").size());
        assertEquals(15, connection.get("connectionId").asLong());
        assertEquals("S9990001A", connection.get("brokerCode").asText());
        assertEquals("CONNECTED", connection.get("connectionStatus").asText());
        assertEquals(2, connection.get("accountCount").asInt());
    }

    @Test
    void 연동한_증권사가_없으면_빈_배열을_반환한다() throws Exception {
        BrokerProviderService providerService = newProviderService(new FakeBrokerProviderRepository());
        BrokerConnectionService connectionService = newConnectionService(new FakeBrokerConnectionRepository());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new BrokerController(providerService, connectionService, newAccountService())).setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();

        MvcResult result = mockMvc.perform(get("/broker/connections"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = new ObjectMapper()
                .readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertTrue(json.get("connections").isEmpty());
    }

    @Test
    void 증권사_연동_요청이_성공하면_201과_연동결과를_반환한다() throws Exception {
        FakeBrokerProviderRepository providerRepository = new FakeBrokerProviderRepository();
        providerRepository.add(BrokerProviderFixture.provider(1L, "S9990001A", "미래에셋증권(모의)"));
        BrokerProviderService providerService = newProviderService(providerRepository);
        BrokerConnectionService connectionService = newConnectionService(new FakeBrokerConnectionRepository(), providerRepository);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new BrokerController(providerService, connectionService, newAccountService())).setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();

        MvcResult result = mockMvc.perform(post("/broker/connections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"brokerId\":1,\"loginId\":\"demo1\",\"password\":\"1234\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = new ObjectMapper()
                .readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertEquals("CONNECTED", json.get("connectionStatus").asText());
        assertEquals("S9990001A", json.get("brokerCode").asText());
        assertEquals("SUCCESS", json.get("syncResult").get("syncStatus").asText());
    }

    @Test
    void 존재하지_않는_브로커로_연동_요청하면_404를_반환한다() throws Exception {
        BrokerProviderService providerService = newProviderService(new FakeBrokerProviderRepository());
        BrokerConnectionService connectionService = newConnectionService(new FakeBrokerConnectionRepository());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new BrokerController(providerService, connectionService, newAccountService()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();

        MvcResult result = mockMvc.perform(post("/broker/connections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"brokerId\":999,\"loginId\":\"demo1\",\"password\":\"1234\"}"))
                .andExpect(status().isNotFound())
                .andReturn();

        JsonNode json = new ObjectMapper()
                .readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertEquals("BRK_001", json.get("errorCode").asText());
    }

    @Test
    void 연결_상세를_조회한다() throws Exception {
        BrokerProviderService providerService = newProviderService(new FakeBrokerProviderRepository());
        FakeBrokerConnectionRepository repository = new FakeBrokerConnectionRepository();
        repository.add(1L, BrokerConnectionFixture.connected(15L, 1L, "S9990001A", "미래에셋증권(모의)"));
        BrokerConnectionService connectionService = newConnectionService(repository);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new BrokerController(providerService, connectionService, newAccountService())).setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();

        MvcResult result = mockMvc.perform(get("/broker/connections/15"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = new ObjectMapper()
                .readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertEquals(15, json.get("connectionId").asLong());
        assertEquals("S9990001A", json.get("brokerCode").asText());
        assertEquals("CONNECTED", json.get("connectionStatus").asText());
        assertTrue(json.get("latestSync").isNull());
    }

    @Test
    void 존재하지_않는_연결을_조회하면_404를_반환한다() throws Exception {
        BrokerProviderService providerService = newProviderService(new FakeBrokerProviderRepository());
        BrokerConnectionService connectionService = newConnectionService(new FakeBrokerConnectionRepository());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new BrokerController(providerService, connectionService, newAccountService()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();

        MvcResult result = mockMvc.perform(get("/broker/connections/999"))
                .andExpect(status().isNotFound())
                .andReturn();

        JsonNode json = new ObjectMapper()
                .readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertEquals("BRK_005", json.get("errorCode").asText());
    }

    @Test
    void 연결의_계좌_목록을_반환한다() throws Exception {
        BrokerProviderService providerService = newProviderService(new FakeBrokerProviderRepository());
        FakeBrokerConnectionRepository connectionRepository = new FakeBrokerConnectionRepository();
        connectionRepository.add(1L, BrokerConnectionFixture.connected(15L, 1L, "S9990001A", "미래에셋증권(모의)"));
        BrokerConnectionService connectionService = newConnectionService(connectionRepository);
        FakeInvestmentAccountRepository accountRepository = new FakeInvestmentAccountRepository();
        accountRepository.add(1L, com.investory.broker.domain.model.InvestmentAccount.of(
                25L, 15L, "ext-1", "1234-****-5678", "종합주식계좌",
                com.investory.broker.domain.constant.AccountType.STOCK, "KRW"));
        InvestmentAccountService accountService = newAccountService(accountRepository, connectionRepository);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new BrokerController(providerService, connectionService, accountService)).setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();

        MvcResult result = mockMvc.perform(get("/broker/connections/15/accounts"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = new ObjectMapper()
                .readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertEquals(15, json.get("connectionId").asLong());
        assertEquals("미래에셋증권(모의)", json.get("brokerName").asText());
        assertEquals(1, json.get("accounts").size());
        assertEquals(25, json.get("accounts").get(0).get("accountId").asLong());
    }

    @Test
    void 연결_재동기화에_성공하면_200과_결과를_반환한다() throws Exception {
        BrokerProviderService providerService = newProviderService(new FakeBrokerProviderRepository());
        FakeBrokerConnectionRepository connectionRepository = new FakeBrokerConnectionRepository();
        connectionRepository.add(1L, BrokerConnectionFixture.connected(15L, 1L, "S9990001A", "미래에셋증권(모의)"));
        connectionRepository.addMockProfileCode(15L, "demo1");
        BrokerConnectionService connectionService = newConnectionService(connectionRepository);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new BrokerController(providerService, connectionService, newAccountService())).setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();

        MvcResult result = mockMvc.perform(post("/broker/connections/15/sync"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = new ObjectMapper()
                .readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertEquals(15, json.get("connectionId").asLong());
        assertEquals("SUCCESS", json.get("syncStatus").asText());
    }

    @Test
    void 존재하지_않는_연결을_재동기화하면_404를_반환한다() throws Exception {
        BrokerProviderService providerService = newProviderService(new FakeBrokerProviderRepository());
        BrokerConnectionService connectionService = newConnectionService(new FakeBrokerConnectionRepository());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                        new BrokerController(providerService, connectionService, newAccountService()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();

        MvcResult result = mockMvc.perform(post("/broker/connections/999/sync"))
                .andExpect(status().isNotFound())
                .andReturn();

        JsonNode json = new ObjectMapper()
                .readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertEquals("BRK_005", json.get("errorCode").asText());
    }

    @Test
    void 전체_계좌_목록을_반환한다() throws Exception {
        BrokerProviderService providerService = newProviderService(new FakeBrokerProviderRepository());
        FakeBrokerConnectionRepository connectionRepository = new FakeBrokerConnectionRepository();
        connectionRepository.add(1L, BrokerConnectionFixture.connected(15L, 1L, "S9990001A", "미래에셋증권(모의)"));
        BrokerConnectionService connectionService = newConnectionService(connectionRepository);
        FakeInvestmentAccountRepository accountRepository = new FakeInvestmentAccountRepository();
        accountRepository.add(1L, com.investory.broker.domain.model.InvestmentAccount.of(
                25L, 15L, "ext-1", "1234-****-5678", "종합주식계좌",
                com.investory.broker.domain.constant.AccountType.STOCK, "KRW"));
        InvestmentAccountService accountService = newAccountService(accountRepository, connectionRepository);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new BrokerController(providerService, connectionService, accountService)).setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();

        MvcResult result = mockMvc.perform(get("/broker/accounts"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = new ObjectMapper()
                .readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertEquals(1, json.get("summary").get("accountCount").asInt());
        assertEquals(1, json.get("accounts").size());
        JsonNode account = json.get("accounts").get(0);
        assertEquals(25, account.get("accountId").asLong());
        assertEquals(1, account.get("brokerId").asLong());
        assertEquals("미래에셋증권(모의)", account.get("brokerName").asText());
    }

    @Test
    void 계좌가_없으면_전체_계좌_목록도_빈_배열을_반환한다() throws Exception {
        BrokerProviderService providerService = newProviderService(new FakeBrokerProviderRepository());
        BrokerConnectionService connectionService = newConnectionService(new FakeBrokerConnectionRepository());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new BrokerController(providerService, connectionService, newAccountService())).setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();

        MvcResult result = mockMvc.perform(get("/broker/accounts"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = new ObjectMapper()
                .readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertEquals(0, json.get("summary").get("accountCount").asInt());
        assertTrue(json.get("accounts").isEmpty());
    }

    @Test
    void 계좌_상세와_보유종목_목록을_반환한다() throws Exception {
        BrokerProviderService providerService = newProviderService(new FakeBrokerProviderRepository());
        FakeBrokerConnectionRepository connectionRepository = new FakeBrokerConnectionRepository();
        connectionRepository.add(1L, BrokerConnectionFixture.connected(15L, 1L, "S9990001A", "미래에셋증권(모의)"));
        BrokerConnectionService connectionService = newConnectionService(connectionRepository);
        FakeInvestmentAccountRepository accountRepository = new FakeInvestmentAccountRepository();
        accountRepository.add(1L, com.investory.broker.domain.model.InvestmentAccount.of(
                25L, 15L, "ext-1", "1234-****-5678", "종합주식계좌",
                com.investory.broker.domain.constant.AccountType.STOCK, "KRW"));
        FakeHoldingDetailPort holdingDetailPort = new FakeHoldingDetailPort();
        holdingDetailPort.willReturn(new AccountHoldingsInfo(
                new HoldingSummaryInfo(1, BigDecimal.valueOf(750000), BigDecimal.valueOf(30000)),
                List.of(new HoldingDetailInfo(
                        101L, "005930", "삼성전자", "KOSPI",
                        BigDecimal.TEN, BigDecimal.valueOf(72000), BigDecimal.valueOf(750000),
                        BigDecimal.valueOf(30000), BigDecimal.valueOf(8.91), LocalDate.parse("2026-07-29")))));
        InvestmentAccountService accountService = newAccountService(accountRepository, connectionRepository, holdingDetailPort);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new BrokerController(providerService, connectionService, accountService)).setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();

        MvcResult result = mockMvc.perform(get("/broker/accounts/25"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = new ObjectMapper()
                .readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertEquals(25, json.get("accountId").asLong());
        assertEquals(1, json.get("summary").get("holdingCount").asInt());
        assertEquals(1, json.get("holdings").size());
        assertEquals("005930", json.get("holdings").get(0).get("securityCode").asText());
    }

    @Test
    void 존재하지_않는_계좌_상세를_조회하면_404를_반환한다() throws Exception {
        BrokerProviderService providerService = newProviderService(new FakeBrokerProviderRepository());
        BrokerConnectionService connectionService = newConnectionService(new FakeBrokerConnectionRepository());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new BrokerController(providerService, connectionService, newAccountService()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();

        MvcResult result = mockMvc.perform(get("/broker/accounts/999"))
                .andExpect(status().isNotFound())
                .andReturn();

        JsonNode json = new ObjectMapper()
                .readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertEquals("BRK_006", json.get("errorCode").asText());
    }

    @Test
    void 계좌_이름을_변경한다() throws Exception {
        BrokerProviderService providerService = newProviderService(new FakeBrokerProviderRepository());
        FakeBrokerConnectionRepository connectionRepository = new FakeBrokerConnectionRepository();
        FakeInvestmentAccountRepository accountRepository = new FakeInvestmentAccountRepository();
        accountRepository.add(1L, com.investory.broker.domain.model.InvestmentAccount.of(
                25L, 15L, "ext-1", "1234-****-5678", "종합주식계좌",
                com.investory.broker.domain.constant.AccountType.STOCK, "KRW"));
        BrokerConnectionService connectionService = newConnectionService(connectionRepository);
        InvestmentAccountService accountService = newAccountService(accountRepository, connectionRepository);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new BrokerController(providerService, connectionService, accountService)).setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();

        MvcResult result = mockMvc.perform(patch("/broker/accounts/25")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountName\":\"장기 투자용 계좌\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = new ObjectMapper()
                .readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertEquals(25, json.get("accountId").asLong());
        assertEquals("장기 투자용 계좌", json.get("accountName").asText());
    }

    @Test
    void 본인_소유가_아닌_계좌_이름을_변경하면_404를_반환한다() throws Exception {
        BrokerProviderService providerService = newProviderService(new FakeBrokerProviderRepository());
        BrokerConnectionService connectionService = newConnectionService(new FakeBrokerConnectionRepository());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new BrokerController(providerService, connectionService, newAccountService()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();

        MvcResult result = mockMvc.perform(patch("/broker/accounts/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountName\":\"장기 투자용 계좌\"}"))
                .andExpect(status().isNotFound())
                .andReturn();

        JsonNode json = new ObjectMapper()
                .readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertEquals("BRK_006", json.get("errorCode").asText());
    }

    private static class FailingBrokerProviderRepository implements com.investory.broker.domain.repositories.BrokerProviderRepository {
        @Override
        public java.util.List<com.investory.broker.domain.model.BrokerProvider> findAllActive() {
            throw new BrokerInfraException(new RuntimeException("DB down"));
        }

        @Override
        public java.util.Optional<com.investory.broker.domain.model.BrokerProvider> findById(Long brokerId) {
            throw new BrokerInfraException(new RuntimeException("DB down"));
        }

        @Override
        public void upsertByCode(String brokerCode, String brokerName) {
            throw new BrokerInfraException(new RuntimeException("DB down"));
        }

        @Override
        public void deactivateExcept(java.util.List<String> brokerCodes) {
            throw new BrokerInfraException(new RuntimeException("DB down"));
        }
    }
}
