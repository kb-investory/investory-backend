package com.investory.journal.domain.services.dto.result;

import com.investory.journal.domain.constant.TradeSide;
import com.investory.journal.domain.ports.dto.SecurityInfo;
import com.investory.journal.domain.ports.dto.TradeInfo;

import java.math.BigDecimal;
import java.time.Instant;

public record TradeDetailResult(
    Long tradeId,
    Long securityId,
    String securityCode,
    String securityName,
    TradeSide tradeSide,
    int quantity,
    BigDecimal unitPrice,
    Instant tradedAt,
    TradeNoteResult note
) {
    public static TradeDetailResult from(TradeInfo trade, SecurityInfo security, TradeNoteResult note) {
        return new TradeDetailResult(
                trade.tradeId(),
                trade.securityId(),
                security.securityCode(),
                security.securityName(),
                trade.tradeSide(),
                trade.quantity(),
                trade.unitPrice(),
                trade.tradedAt(),
                note
        );
    }
}
