package com.investory.market.infra.repository_impls;

import com.investory.market.domain.constant.MarketType;
import com.investory.market.domain.model.Security;
import com.investory.market.domain.repositories.SecurityRepository;
import com.investory.market.infra.entities.SecurityRow;
import com.investory.market.infra.exception.MarketInfraErrorCode;
import com.investory.market.infra.exception.MarketInfraException;
import com.investory.market.infra.mappers.SecurityMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class SecurityRepositoryImpl implements SecurityRepository {

    private final SecurityMapper securityMapper;

    public SecurityRepositoryImpl(SecurityMapper securityMapper) {
        this.securityMapper = securityMapper;
    }

    @Override
    public Optional<Security> findBySecurityCode(String securityCode) {
        try {
            SecurityRow row = securityMapper.findBySecurityCode(securityCode);
            return Optional.ofNullable(row).map(SecurityRow::toDomain);
        } catch (DataAccessException e) {
            throw new MarketInfraException(MarketInfraErrorCode.STOCK_QUERY_FAILED, e);
        }
    }

    @Override
    public Optional<Security> findBySecurityId(Long securityId) {
        try {
            SecurityRow row = securityMapper.findBySecurityId(securityId);
            return Optional.ofNullable(row).map(SecurityRow::toDomain);
        } catch (DataAccessException e) {
            throw new MarketInfraException(MarketInfraErrorCode.STOCK_QUERY_FAILED, e);
        }
    }

    @Override
    public List<Security> findBySecurityIds(List<Long> securityIds) {
        if (securityIds == null || securityIds.isEmpty()) {
            return List.of();
        }
        try {
            List<SecurityRow> rows = securityMapper.findBySecurityIds(securityIds);
            return rows.stream().map(SecurityRow::toDomain).collect(Collectors.toList());
        } catch (DataAccessException e) {
            throw new MarketInfraException(MarketInfraErrorCode.STOCK_QUERY_FAILED, e);
        }
    }

    @Override
    public List<Security> findBySecurityCodes(List<String> securityCodes) {
        if (securityCodes == null || securityCodes.isEmpty()) {
            return List.of();
        }
        try {
            List<SecurityRow> rows = securityMapper.findBySecurityCodes(securityCodes);
            return rows.stream().map(SecurityRow::toDomain).collect(Collectors.toList());
        } catch (DataAccessException e) {
            throw new MarketInfraException(MarketInfraErrorCode.STOCK_QUERY_FAILED, e);
        }
    }

    @Override
    public List<String> findAllSecurityCodes() {
        try {
            return securityMapper.findAllSecurityCodes();
        } catch (DataAccessException e) {
            throw new MarketInfraException(MarketInfraErrorCode.STOCK_QUERY_FAILED, e);
        }
    }

    @Override
    public List<Security> search(String keyword, MarketType marketType, int offset, int limit) {
        try {
            String marketTypeName = marketType != null ? marketType.name() : null;
            List<SecurityRow> rows = securityMapper.search(keyword, marketTypeName, offset, limit);
            return rows.stream().map(SecurityRow::toDomain).collect(Collectors.toList());
        } catch (DataAccessException e) {
            throw new MarketInfraException(MarketInfraErrorCode.STOCK_QUERY_FAILED, e);
        }
    }

    @Override
    public long countSearch(String keyword, MarketType marketType) {
        try {
            String marketTypeName = marketType != null ? marketType.name() : null;
            return securityMapper.countSearch(keyword, marketTypeName);
        } catch (DataAccessException e) {
            throw new MarketInfraException(MarketInfraErrorCode.STOCK_QUERY_FAILED, e);
        }
    }

    // security_code 존재 여부로 insert/update를 분기하는 upsert
    @Override
    public Security save(Security security) {
        try {
            SecurityRow row = SecurityRow.from(security);
            boolean exists = (securityMapper.findBySecurityCode(row.getSecurityCode()) != null);
            if (exists) {
                securityMapper.update(row);
            } else {
                securityMapper.insert(row);
            }
            return row.toDomain();
        } catch (DataAccessException e) {
            throw new MarketInfraException(MarketInfraErrorCode.STOCK_SAVE_FAILED, e);
        }
    }
}
