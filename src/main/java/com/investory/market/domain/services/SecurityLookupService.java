package com.investory.market.domain.services;

import com.investory.market.domain.model.Security;
import com.investory.market.domain.repositories.SecurityRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 다른 도메인(ledger 등)이 market 정보를 조회할 때 쓰는 좁고 가벼운 서비스.
 *
 * MarketDataQueryService는 프론트 요청 기준으로 설계돼 있어서(못 찾으면 MarketException throw,
 * SecurityPriceRepository까지 딸려있음) cross-domain 조회 용도로는 안 맞는다. 이 서비스는:
 *  - 못 찾아도 예외를 던지지 않고 Optional/빈 리스트로 반환한다 (호출 측에서 스킵 등으로 처리하도록)
 *  - SecurityRepository에만 의존한다 (SecurityPriceRepository 등 불필요한 의존성 없음)
 *
 * broker의 AccountLookupService와 동일한 역할/구조.
 */
@Service
public class SecurityLookupService {

    private final SecurityRepository securityRepository;

    public SecurityLookupService(SecurityRepository securityRepository) {
        this.securityRepository = securityRepository;
    }

    public Optional<Security> findByCode(String securityCode) {
        return securityRepository.findBySecurityCode(securityCode);
    }

    public List<Security> findByIds(List<Long> securityIds) {
        return securityRepository.findBySecurityIds(securityIds);
    }
}
