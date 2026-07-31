package com.investory.journal.domain.services.dto.command;

import com.investory.journal.domain.constant.MarketMood;

import java.time.LocalDate;
import java.util.List;

public record CreateJournalCommand(
    Long userId,
    LocalDate journalDate,
    String marketThought,
    MarketMood marketMood,
    List<TradeNoteCommand> tradeNotes
) {
}
