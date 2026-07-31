package com.investory.journal.presentation.dto.request;

import com.investory.journal.domain.constant.MarketMood;
import com.investory.journal.domain.services.dto.command.TradeNoteCommand;
import com.investory.journal.domain.services.dto.command.UpdateJournalCommand;

import java.util.List;
import java.util.stream.Collectors;

public record UpdateJournalRequest(
        String marketThought,
        MarketMood marketMood,
        List<TradeNoteRequest> tradeNotes
) {
    public UpdateJournalCommand toCommand(Long userId, Long journalId) {
        List<TradeNoteCommand> notes = tradeNotes == null ? null : tradeNotes.stream()
                .map(TradeNoteRequest::toCommand)
                .collect(Collectors.toList());
        return new UpdateJournalCommand(userId, journalId, marketThought, marketMood, notes);
    }
}
