package com.investory.market.infra.repository_impls;

import com.investory.market.domain.model.SecurityPrice;
import com.investory.market.domain.repositories.SecurityPriceRepository;
import com.investory.market.infra.entities.SecurityPriceRow;
import com.investory.market.infra.exception.MarketInfraErrorCode;
import com.investory.market.infra.exception.MarketInfraException;
import com.investory.market.infra.mappers.SecurityPriceMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class SecurityPriceRepositoryImpl implements SecurityPriceRepository {

    private final SecurityPriceMapper securityPriceMapper;

    public SecurityPriceRepositoryImpl(SecurityPriceMapper securityPriceMapper) {
        this.securityPriceMapper = securityPriceMapper;
    }

    @Override
    public Optional<SecurityPrice> findBySecurityIdAndPriceDate(Long securityId, LocalDate priceDate) {
        try {
            SecurityPriceRow row = securityPriceMapper.findBySecurityIdAndPriceDate(securityId, priceDate);
            return Optional.ofNullable(row).map(SecurityPriceRow::toDomain);
        } catch (DataAccessException e) {
            throw new MarketInfraException(MarketInfraErrorCode.STOCK_PRICE_QUERY_FAILED, e);
        }
    }

    @Override
    public Optional<SecurityPrice> findLatestBySecurityId(Long securityId) {
        try {
            SecurityPriceRow row = securityPriceMapper.findLatestBySecurityId(securityId);
            return Optional.ofNullable(row).map(SecurityPriceRow::toDomain);
        } catch (DataAccessException e) {
            throw new MarketInfraException(MarketInfraErrorCode.STOCK_PRICE_QUERY_FAILED, e);
        }
    }

    @Override
    public List<SecurityPrice> findBySecurityIdAndDateRange(Long securityId, LocalDate from, LocalDate to) {
        try {
            List<SecurityPriceRow> rows = securityPriceMapper.findBySecurityIdAndDateRange(securityId, from, to);
            return rows.stream().map(SecurityPriceRow::toDomain).collect(Collectors.toList());
        } catch (DataAccessException e) {
            throw new MarketInfraException(MarketInfraErrorCode.STOCK_PRICE_QUERY_FAILED, e);
        }
    }

    @Override
    public List<SecurityPrice> findBySecurityIdsAndDateRange(List<Long> securityIds, LocalDate from, LocalDate to) {
        if (securityIds.isEmpty()) {
            return List.of();
        }
        try {
            List<SecurityPriceRow> rows = securityPriceMapper.findBySecurityIdsAndDateRange(securityIds, from, to);
            return rows.stream().map(SecurityPriceRow::toDomain).collect(Collectors.toList());
        } catch (DataAccessException e) {
            throw new MarketInfraException(MarketInfraErrorCode.STOCK_PRICE_QUERY_FAILED, e);
        }
    }

    // PK가 (security_id, price_date) 복합키라 그 두 값 존재 여부로 insert/update를 분기한다 (하루 1회 갱신 기준).
    @Override
    public SecurityPrice save(SecurityPrice securityPrice) {
        try {
            SecurityPriceRow row = SecurityPriceRow.from(securityPrice);
            boolean exists = securityPriceMapper.findBySecurityIdAndPriceDate(row.getSecurityId(), row.getPriceDate()) != null;
            if (exists) {
                securityPriceMapper.update(row);
            } else {
                securityPriceMapper.insert(row);
            }
            return row.toDomain();
        } catch (DataAccessException e) {
            throw new MarketInfraException(MarketInfraErrorCode.STOCK_PRICE_SAVE_FAILED, e);
        }
    }
}
