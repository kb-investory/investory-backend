package com.investory.tendency.infra.clients.openai;

import lombok.Data;

// LLM 응답 message.content(JSON 문자열): {"grade":"FOLLOWED"}
@Data
public class PrincipleComplianceGradeContent {
    private String grade;
}
