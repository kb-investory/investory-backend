package com.investory.journal.presentation.dto.request;

import com.investory.journal.domain.services.dto.command.TradeNoteCommand;

public record TradeNoteRequest(Long tradeId, String rationaleText) {
    public TradeNoteCommand toCommand() {
        return new TradeNoteCommand(tradeId, rationaleText);
    }
}
