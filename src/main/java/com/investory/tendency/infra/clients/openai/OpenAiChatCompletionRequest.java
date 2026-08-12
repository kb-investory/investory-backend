package com.investory.tendency.infra.clients.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

// temperature는 null이면 직렬화에서 아예 빠지게 한다 — gpt-5-nano 등 일부 모델은 이 파라미터 자체를
// 지원하지 않아서, 지원 안 하는 모델용으로는 애초에 요청 바디에 넣지 않는 편이 안전하다.
@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenAiChatCompletionRequest {
    private String model;
    private List<Message> messages;
    private Double temperature;
    private ResponseFormat response_format;

    @Data
    @AllArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }

    @Data
    @AllArgsConstructor
    public static class ResponseFormat {
        private String type;
    }
}
