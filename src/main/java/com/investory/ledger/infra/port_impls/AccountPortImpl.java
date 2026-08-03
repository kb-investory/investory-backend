package com.investory.ledger.infra.port_impls;

import com.investory.ledger.domain.ports.AccountPort;
import com.investory.ledger.domain.ports.dto.AccountInfo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class AccountPortImpl implements AccountPort {

    // TODO: broker가 계좌 조회 서비스를 노출하면 실제 호출로 교체. broker가 아직 없어 항상 빈 값을 반환한다.
    @Override
    public List<AccountInfo> findAccountsByUserId(Long userId) {
        return List.of();
    }

    // TODO: broker가 계좌 조회 서비스를 노출하면 실제 호출로 교체. broker가 아직 없어 항상 빈 값을 반환한다.
    @Override
    public List<AccountInfo> findAccounts(List<Long> accountIds) {
        return List.of();
    }

    // TODO: broker가 계좌 조회 서비스를 노출하면 실제 호출로 교체. broker가 아직 없어 항상 빈 값을 반환한다.
    @Override
    public Optional<AccountInfo> findAccount(Long accountId, Long userId) {
        return Optional.empty();
    }
}
