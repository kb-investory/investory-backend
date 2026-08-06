package com.investory.market.infra.repository_impls;

import com.investory.market.domain.constant.MarketType;
import com.investory.market.domain.model.Stock;
import com.investory.market.domain.repositories.StockRepository;
import com.investory.market.infra.entities.StockRow;
import com.investory.market.infra.exception.MarketInfraErrorCode;
import com.investory.market.infra.exception.MarketInfraException;
import com.investory.market.infra.mappers.StockMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class StockRepositoryImpl implements StockRepository {

    private final StockMapper stockMapper;

    public StockRepositoryImpl(StockMapper stockMapper) {
        this.stockMapper = stockMapper;
    }

    @Override
    public Optional<Stock> findByStockCode(String stockCode) {
        try {
            StockRow row = stockMapper.findByStockCode(stockCode);
            return Optional.ofNullable(row).map(StockRow::toDomain);
        } catch (DataAccessException e) {
            throw new MarketInfraException(MarketInfraErrorCode.STOCK_QUERY_FAILED, e);
        }
    }

    @Override
    public Optional<Stock> findBySecurityId(Long securityId) {
        try {
            StockRow row = stockMapper.findBySecurityId(securityId);
            return Optional.ofNullable(row).map(StockRow::toDomain);
        } catch (DataAccessException e) {
            throw new MarketInfraException(MarketInfraErrorCode.STOCK_QUERY_FAILED, e);
        }
    }

    @Override
    public List<Stock> findBySecurityIds(List<Long> securityIds) {
        if (securityIds == null || securityIds.isEmpty()) {
            return List.of();
        }
        try {
            List<StockRow> rows = stockMapper.findBySecurityIds(securityIds);
            return rows.stream().map(StockRow::toDomain).collect(Collectors.toList());
        } catch (DataAccessException e) {
            throw new MarketInfraException(MarketInfraErrorCode.STOCK_QUERY_FAILED, e);
        }
    }

    @Override
    public List<String> findAllStockCodes() {
        try {
            return stockMapper.findAllStockCodes();
        } catch (DataAccessException e) {
            throw new MarketInfraException(MarketInfraErrorCode.STOCK_QUERY_FAILED, e);
        }
    }

    @Override
    public List<Stock> search(String keyword, MarketType marketType, int offset, int limit) {
        try {
            String marketTypeName = marketType != null ? marketType.name() : null;
            List<StockRow> rows = stockMapper.search(keyword, marketTypeName, offset, limit);
            return rows.stream().map(StockRow::toDomain).collect(Collectors.toList());
        } catch (DataAccessException e) {
            throw new MarketInfraException(MarketInfraErrorCode.STOCK_QUERY_FAILED, e);
        }
    }

    @Override
    public long countSearch(String keyword, MarketType marketType) {
        try {
            String marketTypeName = marketType != null ? marketType.name() : null;
            return stockMapper.countSearch(keyword, marketTypeName);
        } catch (DataAccessException e) {
            throw new MarketInfraException(MarketInfraErrorCode.STOCK_QUERY_FAILED, e);
        }
    }

    // security_code 존재 여부로 insert/update를 분기하는 upsert
    @Override
    public Stock save(Stock stock) {
        try {
            StockRow row = StockRow.from(stock);
            boolean exists = (stockMapper.findByStockCode(row.getStockCode()) != null);
            if (exists) {
                stockMapper.update(row);
            } else {
                stockMapper.insert(row);
            }
            return row.toDomain();
        } catch (DataAccessException e) {
            throw new MarketInfraException(MarketInfraErrorCode.STOCK_SAVE_FAILED, e);
        }
    }
}
