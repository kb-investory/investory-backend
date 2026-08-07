package com.investory.journal.presentation.dto.request;

import com.investory.journal.domain.constant.MarketMood;
import com.investory.journal.domain.services.dto.command.CreateJournalCommand;
import com.investory.journal.domain.services.dto.command.TradeNoteCommand;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public record CreateJournalRequest(
        LocalDate journalDate,
        String marketThought,
        MarketMood marketMood,
        List<TradeNoteRequest> tradeNotes
) {
    public CreateJournalCommand toCommand(Long userId) {
        List<TradeNoteCommand> notes = tradeNotes == null ? null : tradeNotes.stream()
                .map(TradeNoteRequest::toCommand)
                .collect(Collectors.toList());
        return new CreateJournalCommand(userId, journalDate, marketThought, marketMood, notes);
    }
}
