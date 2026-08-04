package com.investory.broker.domain.services;

import com.investory.broker.domain.model.BrokerConnection;
import com.investory.broker.domain.model.InvestmentAccount;
import com.investory.broker.domain.repositories.BrokerConnectionRepository;
import com.investory.broker.domain.repositories.InvestmentAccountRepository;
import com.investory.broker.domain.services.dto.result.InvestmentAccountResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// ledger.infra.port_impls.AccountPortImpl(cross-domain 호출 지점)이 직접 호출하는 진입점.
// InvestmentAccountService와 별도 서비스로 분리한 이유: InvestmentAccountService는
// HoldingSummaryPort(→ledger.HoldingQueryService→AccountPort→AccountPortImpl)에 의존하는데,
// 그 경로가 다시 이 조회로 되돌아오면 스프링 빈 생성 시 순환 참조가 생긴다. 이 서비스는
// 리포지토리만 의존해서 그 순환을 끊는다.
@Service
public class AccountLookupService {

    private final InvestmentAccountRepository investmentAccountRepository;
    private final BrokerConnectionRepository brokerConnectionRepository;

    public AccountLookupService(
            InvestmentAccountRepository investmentAccountRepository,
            BrokerConnectionRepository brokerConnectionRepository) {
        this.investmentAccountRepository = investmentAccountRepository;
        this.brokerConnectionRepository = brokerConnectionRepository;
    }

    public List<InvestmentAccountResult> findByUserId(Long userId) {
        return assembleResults(investmentAccountRepository.findByUserId(userId));
    }

    public List<InvestmentAccountResult> findByIds(List<Long> accountIds) {
        return assembleResults(investmentAccountRepository.findByIds(accountIds));
    }

    public Optional<InvestmentAccountResult> findByIdAndUserId(Long accountId, Long userId) {
        return investmentAccountRepository.findByIdAndUserId(accountId, userId)
                .map(account -> toResult(account, resolveBrokerName(account.getConnectionId())));
    }

    private List<InvestmentAccountResult> assembleResults(List<InvestmentAccount> accounts) {
        if (accounts.isEmpty()) {
            return List.of();
        }
        List<Long> connectionIds = accounts.stream()
                .map(InvestmentAccount::getConnectionId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> brokerNameByConnectionId = brokerConnectionRepository.findByIds(connectionIds).stream()
                .collect(Collectors.toMap(BrokerConnection::getConnectionId, BrokerConnection::getBrokerName));

        return accounts.stream()
                .map(account -> toResult(account, brokerNameByConnectionId.get(account.getConnectionId())))
                .collect(Collectors.toList());
    }

    private String resolveBrokerName(Long connectionId) {
        return brokerConnectionRepository.findByIds(List.of(connectionId)).stream()
                .findFirst()
                .map(BrokerConnection::getBrokerName)
                .orElse(null);
    }

    private InvestmentAccountResult toResult(InvestmentAccount account, String brokerName) {
        return new InvestmentAccountResult(
                account.getAccountId(),
                account.getConnectionId(),
                account.getAccountNoMasked(),
                account.getAccountName(),
                account.getAccountType(),
                account.getCurrencyCode(),
                brokerName
        );
    }
}
