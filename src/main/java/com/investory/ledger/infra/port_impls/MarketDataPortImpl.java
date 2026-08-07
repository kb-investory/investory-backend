package com.investory.ledger.infra.port_impls;

import com.investory.ledger.domain.ports.MarketDataPort;
import com.investory.ledger.domain.ports.dto.SecurityInfo;
import com.investory.market.domain.model.Stock;
import com.investory.market.domain.services.StockLookupService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// 빈 이름 명시: journal.infra.port_impls.MarketDataPortImpl과 클래스명이 겹쳐서
// 컴포넌트 스캔 시 기본 빈 이름(marketDataPortImpl)이 충돌한다.
@Component("ledgerMarketDataPortImpl")
public class MarketDataPortImpl implements MarketDataPort {

    private final StockLookupService stockLookupService;

    public MarketDataPortImpl(StockLookupService stockLookupService) {
        this.stockLookupService = stockLookupService;
    }

    @Override
    public List<SecurityInfo> findSecurities(List<Long> securityIds) {
        return stockLookupService.findByIds(securityIds).stream()
                .map(this::toSecurityInfo)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<SecurityInfo> resolveByCode(String securityCode) {
        return stockLookupService.findByCode(securityCode).map(this::toSecurityInfo);
    }

    // SecurityInfo.sectorName 필드명은 그대로 두고 industry name 값을 담는다.
    // securities ERD가 sector_name -> industry_code/industry_name으로 바뀌었지만
    // DB 마이그레이션 전이라 필드 rename은 보류 (ledger 담당자 복귀 후 처리 예정).
    private SecurityInfo toSecurityInfo(Stock stock) {
        return new SecurityInfo(
                stock.getSecurityId(),
                stock.getStockCode(),
                stock.getStockName(),
                stock.getMarketType().name(),
                stock.getStdIdstClsfName()
        );
    }
}
