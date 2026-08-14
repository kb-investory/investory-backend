package com.investory.journal.infra.port_impls;

import com.investory.journal.domain.constant.MarketType;
import com.investory.journal.domain.ports.MarketDataPort;
import com.investory.journal.domain.ports.dto.SecurityInfo;
import com.investory.market.domain.services.StockLookupService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

// 빈 이름 명시: ledger.infra.port_impls.MarketDataPortImpl과 클래스명이 겹쳐서
// 컴포넌트 스캔 시 기본 빈 이름(marketDataPortImpl)이 충돌한다.
@Component("journalMarketDataPortImpl")
public class MarketDataPortImpl implements MarketDataPort {

    private final StockLookupService stockLookupService;

    public MarketDataPortImpl(StockLookupService stockLookupService) {
        this.stockLookupService = stockLookupService;
    }

    @Override
    public List<SecurityInfo> findSecurities(List<Long> securityIds) {
        return stockLookupService.findByIds(securityIds).stream()
                .map(stock -> new SecurityInfo(
                        stock.getSecurityId(),
                        stock.getStockCode(),
                        stock.getStockName(),
                        MarketType.valueOf(stock.getMarketType().name())
                ))
                .collect(Collectors.toList());
    }
}
