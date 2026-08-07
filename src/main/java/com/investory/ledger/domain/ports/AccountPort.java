package com.investory.ledger.domain.ports;

import com.investory.ledger.domain.ports.dto.AccountInfo;

import java.util.List;
import java.util.Optional;

public interface AccountPort {

    // 계좌 필터 없이 조회할 때 — 사용자가 가진 모든 계좌
    List<AccountInfo> findAccountsByUserId(Long userId);

    // 조회 응답 조립용 — accountId로 표시 정보 일괄 조회
    List<AccountInfo> findAccounts(List<Long> accountIds);

    // 특정 accountId가 해당 사용자 소유인지 확인하면서 조회 (없거나 다른 사용자 소유면 empty)
    Optional<AccountInfo> findAccount(Long accountId, Long userId);
}
