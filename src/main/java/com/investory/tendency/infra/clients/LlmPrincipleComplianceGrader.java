package com.investory.tendency.infra.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.investory.tendency.domain.constant.PrincipleComplianceGrade;
import com.investory.tendency.domain.ports.PrincipleComplianceGradingPort;
import com.investory.tendency.domain.ports.dto.PrincipleTradingSummary;
import com.investory.tendency.infra.clients.openai.OpenAiChatCompletionRequest;
import com.investory.tendency.infra.clients.openai.OpenAiChatCompletionResponse;
import com.investory.tendency.infra.clients.openai.PrincipleComplianceGradeContent;
import com.investory.tendency.infra.exception.PrincipleAdherenceLlmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Conditional(PrincipleAdherenceLlmEnabledCondition.class)
public class LlmPrincipleComplianceGrader implements PrincipleComplianceGradingPort {

    private static final Logger log = LoggerFactory.getLogger(LlmPrincipleComplianceGrader.class);

    // 모델명은 환경별로 달라질 이유가 없어 env로 빼지 않고 상수로 고정한다 — 바꾸려면 코드/배포로 반영.
    private static final String MODEL = "gpt-5-nano";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${llm.base-url}")
    private String baseUrl;

    @Value("${llm.api-key}")
    private String apiKey;

    public LlmPrincipleComplianceGrader(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public PrincipleComplianceGrade grade(String principleText, PrincipleTradingSummary summary) {
        OpenAiChatCompletionResponse response = callChatCompletions(buildRequest(principleText, summary));
        String content = extractContent(response);
        return parseGrade(content);
    }

    private OpenAiChatCompletionResponse callChatCompletions(OpenAiChatCompletionRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<OpenAiChatCompletionRequest> entity = new HttpEntity<>(request, headers);
        try {
            ResponseEntity<OpenAiChatCompletionResponse> result = restTemplate.exchange(
                    baseUrl + "/chat/completions", HttpMethod.POST, entity, OpenAiChatCompletionResponse.class);
            return result.getBody();
        } catch (RestClientException e) {
            log.error("원칙 준수 채점 LLM 호출 실패", e);
            throw new PrincipleAdherenceLlmException(e);
        }
    }

    private OpenAiChatCompletionRequest buildRequest(String principleText, PrincipleTradingSummary summary) {
        String allowedGrades = Arrays.stream(PrincipleComplianceGrade.values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));

        String systemPrompt = "너는 투자자가 세운 정성적 투자 원칙과 실제 매매 요약 통계를 비교해 그 원칙을 "
                + "얼마나 지켰는지 판정하는 채점기다. 다음 등급 중 하나로만 답해라: " + allowedGrades + ". "
                + "원시 거래 내역이 아니라 집계된 요약 수치만 근거로 판단해라. "
                + "반드시 {\"grade\": \"<등급 중 하나>\"} 형태의 JSON으로만 답해라.";

        String userMessage = String.format(
                "원칙: %s%n%n최근 90일 거래 요약 — 총 거래 횟수: %d건, 거래한 종목 수: %d개, 주간 평균 거래 횟수: %.1f회",
                principleText, summary.totalTradeCountInWindow(), summary.distinctSecuritiesTradedInWindow(),
                summary.avgTradesPerWeek());

        List<OpenAiChatCompletionRequest.Message> messages = List.of(
                new OpenAiChatCompletionRequest.Message("system", systemPrompt),
                new OpenAiChatCompletionRequest.Message("user", userMessage));

        return new OpenAiChatCompletionRequest(
                MODEL, messages, null, new OpenAiChatCompletionRequest.ResponseFormat("json_object"));
    }

    private String extractContent(OpenAiChatCompletionResponse response) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new PrincipleAdherenceLlmException(new IllegalStateException("LLM 응답에 choices가 없음"));
        }
        String content = response.getChoices().get(0).getMessage().getContent();
        if (content == null || content.isBlank()) {
            throw new PrincipleAdherenceLlmException(new IllegalStateException("LLM 응답 content가 비어있음"));
        }
        return content;
    }

    private PrincipleComplianceGrade parseGrade(String content) {
        PrincipleComplianceGradeContent parsed;
        try {
            parsed = objectMapper.readValue(content, PrincipleComplianceGradeContent.class);
        } catch (Exception e) {
            log.error("원칙 준수 채점 LLM 응답 파싱 실패. content={}", content, e);
            throw new PrincipleAdherenceLlmException(e);
        }

        try {
            return PrincipleComplianceGrade.valueOf(parsed.getGrade());
        } catch (IllegalArgumentException | NullPointerException e) {
            // JSON 형식 자체는 맞지만 taxonomy 밖의 값을 준 경우 — 어느 쪽으로도 치우치지 않는 중립값(PARTIAL)으로 대체.
            log.warn("원칙 준수 채점 LLM이 알 수 없는 등급을 반환함 — PARTIAL로 대체. grade={}", parsed.getGrade());
            return PrincipleComplianceGrade.PARTIAL;
        }
    }
}
