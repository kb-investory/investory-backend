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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    @Override
    public List<StockPrice> findBySecurityIdAndDateRange(Long securityId, LocalDate from, LocalDate to) {
        try {
            List<StockPriceRow> rows = stockPriceMapper.findBySecurityIdAndDateRange(securityId, from, to);
            return rows.stream().map(StockPriceRow::toDomain).collect(Collectors.toList());
        } catch (DataAccessException e) {
            throw new MarketInfraException(MarketInfraErrorCode.STOCK_PRICE_QUERY_FAILED, e);
        }
    }

    // PK가 (security_id, price_date) 복합키라 그 두 값 존재 여부로 insert/update를 분기한다 (하루 1회 갱신 기준).
    @Override
    public StockPrice save(StockPrice stockPrice) {
        try {
            StockPriceRow row = StockPriceRow.from(stockPrice);
            boolean exists = stockPriceMapper.findBySecurityIdAndPriceDate(row.getSecurityId(), row.getPriceDate()) != null;
            if (exists) {
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
