package com.investory.journal.presentation.dto.response;

import com.investory.journal.domain.services.dto.result.JournalEntryResult;

import java.util.List;
import java.util.stream.Collectors;

public record JournalEntryListResponse(List<JournalEntryResponse> entries) {
    public static JournalEntryListResponse from(List<JournalEntryResult> results) {
        return new JournalEntryListResponse(
                results.stream().map(JournalEntryResponse::from).collect(Collectors.toList()));
    }
}
