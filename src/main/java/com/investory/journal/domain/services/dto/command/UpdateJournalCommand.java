package com.investory.journal.domain.services.dto.command;

import com.investory.journal.domain.constant.MarketMood;

import java.util.List;

public record UpdateJournalCommand(
    Long userId,
    Long journalId,
    String marketThought,
    MarketMood marketMood,
    List<TradeNoteCommand> tradeNotes
) {
}
