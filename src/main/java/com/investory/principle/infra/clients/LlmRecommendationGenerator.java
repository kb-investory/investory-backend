package com.investory.principle.infra.clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.investory.principle.domain.ports.RecommendationGenerationPort;
import com.investory.principle.domain.ports.dto.GeneratedRecommendation;
import com.investory.principle.infra.clients.openai.GeneratedRecommendationListContent;
import com.investory.principle.infra.clients.openai.OpenAiChatCompletionRequest;
import com.investory.principle.infra.clients.openai.OpenAiChatCompletionResponse;
import com.investory.principle.infra.exception.RecommendationGenerationException;
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

import java.util.List;
import java.util.stream.Collectors;

@Component
@Conditional(RecommendationGenerationEnabledCondition.class)
public class LlmRecommendationGenerator implements RecommendationGenerationPort {

    private static final Logger log = LoggerFactory.getLogger(LlmRecommendationGenerator.class);

    // 모델명은 환경별로 달라질 이유가 없어 env로 빼지 않고 상수로 고정한다 — 바꾸려면 코드/배포로 반영.
    private static final String MODEL = "gpt-5-nano";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${llm.base-url}")
    private String baseUrl;

    @Value("${llm.api-key}")
    private String apiKey;

    public LlmRecommendationGenerator(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public List<GeneratedRecommendation> generate(String analysisTypeCode, String analysisTypeName) {
        OpenAiChatCompletionResponse response = callChatCompletions(buildRequest(analysisTypeCode, analysisTypeName));
        String content = extractContent(response);
        return parseRecommendations(content);
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
            log.error("추천 생성 LLM 호출 실패", e);
            throw new RecommendationGenerationException(e);
        }
    }

    private OpenAiChatCompletionRequest buildRequest(String analysisTypeCode, String analysisTypeName) {
        String systemPrompt = "너는 개인 투자자의 투자 성향 분석 결과를 보고, 그 성향에 맞는 투자 원칙을 정확히 1개만 추천하는 "
                + "투자 코치다. 반드시 recommendations 배열에 항목을 1개만 담아라. 각 추천은 실제로 지킬 수 있는 구체적인 행동 규칙이어야 한다. "
                + "손실률/수익률 퍼센트 임계값처럼 수치로 검증 가능한 규칙이면 ruleJson에 "
                + "{\"type\":\"STOP_LOSS\"|\"TAKE_PROFIT\",\"value\":<숫자>,\"unit\":\"PERCENT\"} 형태로 채우고, "
                + "정성적인 규칙이면 ruleJson은 null로 둬라. "
                + "반드시 {\"recommendations\":[{\"text\":\"...\",\"reason\":\"...\",\"ruleJson\":null 또는 객체}, ...]} "
                + "형태의 JSON으로만 답해라.";

        String userPrompt = "성향 유형 코드: " + analysisTypeCode + ", 성향 이름: " + analysisTypeName;

        List<OpenAiChatCompletionRequest.Message> messages = List.of(
                new OpenAiChatCompletionRequest.Message("system", systemPrompt),
                new OpenAiChatCompletionRequest.Message("user", userPrompt));

        return new OpenAiChatCompletionRequest(
                MODEL, messages, null, new OpenAiChatCompletionRequest.ResponseFormat("json_object"));
    }

    private String extractContent(OpenAiChatCompletionResponse response) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new RecommendationGenerationException(new IllegalStateException("LLM 응답에 choices가 없음"));
        }
        String content = response.getChoices().get(0).getMessage().getContent();
        if (content == null || content.isBlank()) {
            throw new RecommendationGenerationException(new IllegalStateException("LLM 응답 content가 비어있음"));
        }
        return content;
    }

    private List<GeneratedRecommendation> parseRecommendations(String content) {
        GeneratedRecommendationListContent parsed;
        try {
            parsed = objectMapper.readValue(content, GeneratedRecommendationListContent.class);
        } catch (Exception e) {
            log.error("추천 생성 LLM 응답 파싱 실패. content={}", content, e);
            throw new RecommendationGenerationException(e);
        }
        if (parsed.getRecommendations() == null) {
            return List.of();
        }
        return parsed.getRecommendations().stream()
                .map(item -> new GeneratedRecommendation(item.getText(), item.getReason(), toRuleJsonString(item.getRuleJson())))
                .collect(Collectors.toList());
    }

    // GeneratedRecommendation.ruleJson()은 DB의 JSON 컬럼에 그대로 저장될 문자열을 기대하므로,
    // 파싱된 JsonNode를 다시 압축 JSON 문자열로 직렬화한다. null/JSON null이면 null 그대로 둔다.
    private String toRuleJsonString(JsonNode ruleJson) {
        if (ruleJson == null || ruleJson.isNull()) {
            return null;
        }
        return ruleJson.toString();
    }
}
