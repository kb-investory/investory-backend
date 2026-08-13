package com.investory.journal.infra.clients.openai;

import lombok.Data;

// LLM 응답 message.content(JSON 문자열) 안에 담기는 값 — {"label": "FUNDAMENTAL_ANALYSIS"} 형태
@Data
public class RationaleLabelContent {
    private String label;
}
