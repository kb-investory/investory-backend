package com.investory.principle.infra.clients.openai;

import lombok.Data;

import java.util.List;

// LLM 응답 message.content(JSON 문자열): {"recommendations":[{"text":"...","reason":"...","ruleJson":null}, ...]}
@Data
public class GeneratedRecommendationListContent {
    private List<Item> recommendations;

    @Data
    public static class Item {
        private String text;
        private String reason;
        private String ruleJson;   // nullable — 정성적 추천이면 없음
    }
}
