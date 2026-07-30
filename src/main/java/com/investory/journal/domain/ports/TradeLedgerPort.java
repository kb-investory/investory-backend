package com.investory.journal.domain.ports;

import com.investory.journal.domain.ports.dto.TradeCountInfo;
import com.investory.journal.domain.ports.dto.TradeInfo;

import java.time.LocalDate;
import java.util.List;

public interface TradeLedgerPort {

    // REST 명세에 없는 조회 — ledger가 분리될 때 대응 REST 엔드포인트 추가 필요
    List<TradeCountInfo> countTradesByDateRange(Long userId, LocalDate startDate, LocalDate endDate);

    // ↔ GET /api/ledger/trades/on/{date}
    List<TradeInfo> findTradesOn(Long userId, LocalDate date);
}
