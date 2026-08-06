package com.investory.market.infra.repository_impls;

import com.investory.market.domain.model.StockPrice;
import com.investory.market.domain.repositories.StockPriceRepository;
import com.investory.market.infra.entities.StockPriceRow;
import com.investory.market.infra.exception.MarketInfraErrorCode;
import com.investory.market.infra.exception.MarketInfraException;
import com.investory.market.infra.mappers.StockPriceMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public class StockPriceRepositoryImpl implements StockPriceRepository {

    private final StockPriceMapper stockPriceMapper;

    public StockPriceRepositoryImpl(StockPriceMapper stockPriceMapper) {
        this.stockPriceMapper = stockPriceMapper;
    }

    @Override
    public Optional<StockPrice> findBySecurityIdAndPriceDate(Long securityId, LocalDate priceDate) {
        try {
            StockPriceRow row = stockPriceMapper.findBySecurityIdAndPriceDate(securityId, priceDate);
            return Optional.ofNullable(row).map(StockPriceRow::toDomain);
        } catch (DataAccessException e) {
            throw new MarketInfraException(MarketInfraErrorCode.STOCK_PRICE_QUERY_FAILED, e);
        }
    }

    @Override
    public Optional<StockPrice> findLatestBySecurityId(Long securityId) {
        try {
            StockPriceRow row = stockPriceMapper.findLatestBySecurityId(securityId);
            return Optional.ofNullable(row).map(StockPriceRow::toDomain);
        } catch (DataAccessException e) {
            throw new MarketInfraException(MarketInfraErrorCode.STOCK_PRICE_QUERY_FAILED, e);
        }
    }

    // 같은 날 같은 종목으로 이미 저장된 시세가 있으면 갱신, 없으면 신규 저장 (하루 1회 갱신 기준)
    @Override
    public StockPrice save(StockPrice stockPrice) {
        try {
            StockPriceRow row = StockPriceRow.from(stockPrice);
            StockPriceRow existing = stockPriceMapper.findBySecurityIdAndPriceDate(row.getSecurityId(), row.getPriceDate());
            if (existing != null) {
                row.setPriceId(existing.getPriceId());
                stockPriceMapper.update(row);
            } else {
                stockPriceMapper.insert(row);
            }
            return row.toDomain();
        } catch (DataAccessException e) {
            throw new MarketInfraException(MarketInfraErrorCode.STOCK_PRICE_SAVE_FAILED, e);
        }
    }
}
