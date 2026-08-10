package com.investory.journal.infra.clients.openai;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
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
