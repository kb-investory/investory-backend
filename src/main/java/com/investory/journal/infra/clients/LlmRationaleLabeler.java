package com.investory.journal.infra.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.investory.journal.domain.constant.RationaleLabelType;
import com.investory.journal.domain.ports.RationaleLabelingPort;
import com.investory.journal.infra.clients.openai.OpenAiChatCompletionRequest;
import com.investory.journal.infra.clients.openai.OpenAiChatCompletionResponse;
import com.investory.journal.infra.clients.openai.RationaleLabelContent;
import com.investory.journal.infra.exception.RationaleLabelingException;
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
@Conditional(RationaleLabelingEnabledCondition.class)
public class LlmRationaleLabeler implements RationaleLabelingPort {

    private static final Logger log = LoggerFactory.getLogger(LlmRationaleLabeler.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${llm.rationale-labeling.base-url}")
    private String baseUrl;

    @Value("${llm.rationale-labeling.api-key}")
    private String apiKey;

    @Value("${llm.rationale-labeling.model}")
    private String model;

    public LlmRationaleLabeler(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public RationaleLabelType classify(String rationaleText) {
        OpenAiChatCompletionResponse response = callChatCompletions(buildRequest(rationaleText));
        String content = extractContent(response);
        return parseLabel(content);
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
            log.error("근거 라벨링 LLM 호출 실패", e);
            throw new RationaleLabelingException(e);
        }
    }

    private OpenAiChatCompletionRequest buildRequest(String rationaleText) {
        String allowedLabels = Arrays.stream(RationaleLabelType.values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));

        String systemPrompt = "너는 주식 매수 판단 근거 텍스트를 다음 유형 중 하나로 정확히 분류하는 분류기다: " + allowedLabels + ". "
                + "근거가 어느 유형에도 명확히 해당하지 않으면 UNCLASSIFIED를 선택해라. "
                + "반드시 {\"label\": \"<유형 중 하나>\"} 형태의 JSON으로만 답해라.";

        List<OpenAiChatCompletionRequest.Message> messages = List.of(
                new OpenAiChatCompletionRequest.Message("system", systemPrompt),
                new OpenAiChatCompletionRequest.Message("user", rationaleText));

        return new OpenAiChatCompletionRequest(
                model, messages, 0.0, new OpenAiChatCompletionRequest.ResponseFormat("json_object"));
    }

    private String extractContent(OpenAiChatCompletionResponse response) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new RationaleLabelingException(new IllegalStateException("LLM 응답에 choices가 없음"));
        }
        String content = response.getChoices().get(0).getMessage().getContent();
        if (content == null || content.isBlank()) {
            throw new RationaleLabelingException(new IllegalStateException("LLM 응답 content가 비어있음"));
        }
        return content;
    }

    private RationaleLabelType parseLabel(String content) {
        RationaleLabelContent parsed;
        try {
            parsed = objectMapper.readValue(content, RationaleLabelContent.class);
        } catch (Exception e) {
            log.error("근거 라벨링 LLM 응답 파싱 실패. content={}", content, e);
            throw new RationaleLabelingException(e);
        }

        try {
            return RationaleLabelType.valueOf(parsed.getLabel());
        } catch (IllegalArgumentException | NullPointerException e) {
            // JSON 형식 자체는 맞지만 taxonomy 밖의 값을 준 경우 — 인프라 실패가 아니라 LLM의 정상적인
            // 판단 보류로 취급해 UNCLASSIFIED로 대체한다.
            log.warn("근거 라벨링 LLM이 알 수 없는 라벨을 반환함 — UNCLASSIFIED로 대체. label={}", parsed.getLabel());
            return RationaleLabelType.UNCLASSIFIED;
        }
    }
}
