package com.investory.tendency.infra.port_impls;

import com.investory.market.domain.services.MarketDataQueryService;
import com.investory.tendency.domain.ports.MarketDataPort;
import com.investory.tendency.domain.ports.dto.DailyPriceInfo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component("tendencyMarketDataPortImpl")
public class MarketDataPortImpl implements MarketDataPort {

    private final MarketDataQueryService marketDataQueryService;

    public MarketDataPortImpl(MarketDataQueryService marketDataQueryService) {
        this.marketDataQueryService = marketDataQueryService;
    }

    @Override
    public List<DailyPriceInfo> findDailyPrices(Long securityId, LocalDate from, LocalDate to) {
        return marketDataQueryService.getStockPrices(securityId, from, to).stream()
                .map(p -> new DailyPriceInfo(p.getPriceDate(), BigDecimal.valueOf(p.getClosePrice())))
                .collect(Collectors.toList());
    }
}
