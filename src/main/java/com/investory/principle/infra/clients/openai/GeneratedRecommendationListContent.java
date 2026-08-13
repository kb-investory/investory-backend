package com.investory.principle.infra.clients.openai;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;

// LLM 응답 message.content(JSON 문자열): {"recommendations":[{"text":"...","reason":"...","ruleJson":null}, ...]}
// ruleJson은 null이거나 {"type":...,"value":...,"unit":...} 형태의 JSON 객체로 내려온다(문자열이 아님) —
// String으로 받으면 객체가 왔을 때 역직렬화가 깨지므로 원시 노드(JsonNode)로 받는다.
@Data
public class GeneratedRecommendationListContent {
    private List<Item> recommendations;

    @Data
    public static class Item {
        private String text;
        private String reason;
        private JsonNode ruleJson;   // nullable — 정성적 추천이면 없음
    }
}
