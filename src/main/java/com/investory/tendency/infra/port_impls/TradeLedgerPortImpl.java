package com.investory.tendency.infra.port_impls;

import com.investory.ledger.domain.services.TradeQueryService;
import com.investory.ledger.domain.services.dto.query.GetTradesQuery;
import com.investory.ledger.domain.services.dto.result.TradeListResult;
import com.investory.ledger.domain.services.dto.result.TradeResult;
import com.investory.tendency.domain.ports.TradeLedgerPort;
import com.investory.tendency.domain.ports.dto.TradeInfo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

// 빈 이름 명시: journal.infra.port_impls.TradeLedgerPortImpl과 클래스명이 겹쳐서
// 컴포넌트 스캔 시 기본 빈 이름(tradeLedgerPortImpl)이 충돌한다.
@Component("tendencyTradeLedgerPortImpl")
public class TradeLedgerPortImpl implements TradeLedgerPort {

    // ledger.TradeQueryService의 페이지 크기 상한과 동일 (전체 이력을 다 받을 때까지 이 크기로 반복 조회)
    private static final int PAGE_SIZE = 100;

    private final TradeQueryService tradeQueryService;

    public TradeLedgerPortImpl(TradeQueryService tradeQueryService) {
        this.tradeQueryService = tradeQueryService;
    }

    @Override
    public List<TradeInfo> findTrades(Long userId, Long securityId) {
        List<TradeResult> allTrades = new ArrayList<>();
        int page = 0;
        while (true) {
            GetTradesQuery query = new GetTradesQuery(userId, null, securityId, null, null, null, page, PAGE_SIZE);
            TradeListResult result = tradeQueryService.getTrades(query);
            allTrades.addAll(result.content());
            if (!result.hasNext()) {
                break;
            }
            page++;
        }

        return allTrades.stream()
                .sorted(Comparator.comparing(TradeResult::tradedAt))
                .map(t -> new TradeInfo(t.securityId(), t.tradeSide().name(), t.quantity(), t.unitPrice(), t.tradedAt()))
                .collect(Collectors.toList());
    }
}
