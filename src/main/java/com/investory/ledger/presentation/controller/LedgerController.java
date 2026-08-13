package com.investory.ledger.presentation.controller;

import com.investory.ledger.domain.services.HoldingQueryService;
import com.investory.ledger.domain.services.TradeQueryService;
import com.investory.ledger.domain.services.dto.query.GetHoldingsQuery;
import com.investory.ledger.domain.services.dto.query.GetTradeDetailQuery;
import com.investory.ledger.domain.services.dto.query.GetTradesQuery;
import com.investory.ledger.domain.services.dto.result.HoldingListResult;
import com.investory.ledger.domain.services.dto.result.TradeDetailResult;
import com.investory.ledger.domain.services.dto.result.TradeListResult;
import com.investory.ledger.presentation.dto.response.HoldingListResponse;
import com.investory.ledger.presentation.dto.response.TradeDetailResponse;
import com.investory.ledger.presentation.dto.response.TradeListResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/ledger")
public class LedgerController {

    private final TradeQueryService tradeQueryService;
    private final HoldingQueryService holdingQueryService;

    public LedgerController(TradeQueryService tradeQueryService, HoldingQueryService holdingQueryService) {
        this.tradeQueryService = tradeQueryService;
        this.holdingQueryService = holdingQueryService;
    }

    @GetMapping("/trades")
    public TradeListResponse getTrades(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) Long securityId,
            @RequestParam(required = false) String tradeSide,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        GetTradesQuery query = new GetTradesQuery(userId, accountId, securityId, tradeSide, from, to, page, size);
        TradeListResult result = tradeQueryService.getTrades(query);
        return TradeListResponse.from(result);
    }

    @GetMapping("/trades/{tradeId}")
    public TradeDetailResponse getTradeDetail(@AuthenticationPrincipal Long userId, @PathVariable Long tradeId) {
        TradeDetailResult result = tradeQueryService.getTradeDetail(new GetTradeDetailQuery(userId, tradeId));
        return TradeDetailResponse.from(result);
    }

    @GetMapping("/holdings")
    public HoldingListResponse getHoldings(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) Long securityId) {
        GetHoldingsQuery query = new GetHoldingsQuery(userId, accountId, securityId);
        HoldingListResult result = holdingQueryService.getHoldings(query);
        return HoldingListResponse.from(result);
    }
}
