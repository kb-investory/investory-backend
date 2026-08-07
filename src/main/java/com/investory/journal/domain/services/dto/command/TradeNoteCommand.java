package com.investory.journal.domain.services.dto.command;

public record TradeNoteCommand(
    Long tradeId,
    String rationaleText
) {
}
