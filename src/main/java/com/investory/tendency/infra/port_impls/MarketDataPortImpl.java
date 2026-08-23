package com.investory.tendency.infra.port_impls;

import com.investory.market.domain.model.SecurityPrice;
import com.investory.market.domain.services.MarketDataQueryService;
import com.investory.tendency.domain.ports.MarketDataPort;
import com.investory.tendency.domain.ports.dto.DailyPriceInfo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
                .map(this::toDailyPriceInfo)
                .collect(Collectors.toList());
    }

    @Override
    public Map<Long, List<DailyPriceInfo>> findDailyPrices(List<Long> securityIds, LocalDate from, LocalDate to) {
        return marketDataQueryService.getStockPrices(securityIds, from, to).stream()
                .collect(Collectors.groupingBy(
                        SecurityPrice::getSecurityId,
                        Collectors.mapping(this::toDailyPriceInfo, Collectors.toList())));
    }

    private DailyPriceInfo toDailyPriceInfo(SecurityPrice p) {
        return new DailyPriceInfo(p.getPriceDate(), BigDecimal.valueOf(p.getClosePrice()), p.getDailyReturnRate());
    }
}
