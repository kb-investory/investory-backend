package com.investory.tendency.infra.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.investory.tendency.domain.constant.PrincipleRuleType;
import com.investory.tendency.domain.ports.PrincipleRuleClassificationPort;
import com.investory.tendency.domain.ports.dto.PrincipleRuleClassification;
import com.investory.tendency.infra.clients.openai.OpenAiChatCompletionRequest;
import com.investory.tendency.infra.clients.openai.OpenAiChatCompletionResponse;
import com.investory.tendency.infra.clients.openai.PrincipleRuleClassificationContent;
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
public class LlmPrincipleRuleClassifier implements PrincipleRuleClassificationPort {

    private static final Logger log = LoggerFactory.getLogger(LlmPrincipleRuleClassifier.class);

    // 모델명은 환경별로 달라질 이유가 없어 env로 빼지 않고 상수로 고정한다 — 바꾸려면 코드/배포로 반영.
    private static final String MODEL = "gpt-5-nano";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${llm.base-url}")
    private String baseUrl;

    @Value("${llm.api-key}")
    private String apiKey;

    public LlmPrincipleRuleClassifier(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public PrincipleRuleClassification classify(String principleText) {
        OpenAiChatCompletionResponse response = callChatCompletions(buildRequest(principleText));
        String content = extractContent(response);
        return parseClassification(content);
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
            log.error("원칙 규칙 분류 LLM 호출 실패", e);
            throw new PrincipleAdherenceLlmException(e);
        }
    }

    private OpenAiChatCompletionRequest buildRequest(String principleText) {
        String allowedTypes = Arrays.stream(PrincipleRuleType.values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));

        String systemPrompt = "너는 개인 투자자가 작성한 투자 원칙 텍스트를 다음 유형 중 하나로 정확히 분류하는 분류기다: "
                + allowedTypes + ". "
                + "STOP_LOSS는 손실률이 일정 % 이상이면 매도한다는 규칙, TAKE_PROFIT은 수익률이 일정 % 이상이면 "
                + "매도한다는 규칙이다 — 이 두 경우 반드시 퍼센트 숫자(thresholdPercent)를 함께 추출해라. "
                + "\"N% 오르면 매수\" 같은 매수 트리거는 EXCLUDED로 분류해라(매수 안 한 이유는 데이터로 검증 불가). "
                + "숫자 임계값이 없는 정성적 원칙(예: \"장기적 관점을 유지한다\")은 ABSTRACT로 분류해라. "
                + "반드시 {\"type\": \"<유형 중 하나>\", \"thresholdPercent\": <숫자 또는 null>} 형태의 JSON으로만 답해라.";

        List<OpenAiChatCompletionRequest.Message> messages = List.of(
                new OpenAiChatCompletionRequest.Message("system", systemPrompt),
                new OpenAiChatCompletionRequest.Message("user", principleText));

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

    private PrincipleRuleClassification parseClassification(String content) {
        PrincipleRuleClassificationContent parsed;
        try {
            parsed = objectMapper.readValue(content, PrincipleRuleClassificationContent.class);
        } catch (Exception e) {
            log.error("원칙 규칙 분류 LLM 응답 파싱 실패. content={}", content, e);
            throw new PrincipleAdherenceLlmException(e);
        }

        try {
            PrincipleRuleType type = PrincipleRuleType.valueOf(parsed.getType());
            return new PrincipleRuleClassification(type, parsed.getThresholdPercent());
        } catch (IllegalArgumentException | NullPointerException e) {
            // JSON 형식 자체는 맞지만 taxonomy 밖의 값을 준 경우 — 인프라 실패가 아니라 LLM의 정상적인
            // 판단 보류로 취급해 EXCLUDED로 대체한다.
            log.warn("원칙 규칙 분류 LLM이 알 수 없는 타입을 반환함 — EXCLUDED로 대체. type={}", parsed.getType());
            return new PrincipleRuleClassification(PrincipleRuleType.EXCLUDED, null);
        }
    }
}
