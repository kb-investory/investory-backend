package com.investory.broker.domain.ports;

import com.investory.broker.domain.ports.dto.BrokerLoginResult;
import com.investory.broker.domain.ports.dto.RawAccountRecord;
import com.investory.broker.domain.ports.dto.RawHoldingBatch;
import com.investory.broker.domain.ports.dto.RawOrganizationRecord;
import com.investory.broker.domain.ports.dto.RawTradeRecord;
import com.investory.broker.infra.exception.BrokerFeedAuthFailedException;
import com.investory.broker.infra.exception.BrokerInfraException;
import com.investory.core.exception.ErrorType;

import java.time.LocalDate;
import java.util.List;

public class FakeBrokerFeedPort implements BrokerFeedPort {

    private BrokerLoginResult loginResult = new BrokerLoginResult("access-token", "S9990001A", "미래에셋증권(모의)");
    private boolean loginAuthFails = false;
    private boolean loginServerFails = false;

    private List<RawAccountRecord> accounts = List.of();
    private List<RawTradeRecord> trades = List.of();
    private RawHoldingBatch holdingBatch = new RawHoldingBatch(LocalDate.now(), List.of());
    private RuntimeException syncFailure;
    private List<RawOrganizationRecord> organizations = List.of();

    public void willFailLoginWithUnauthorized() {
        this.loginAuthFails = true;
    }

    public void willFailLoginWithServerError() {
        this.loginServerFails = true;
    }

    public void willReturnAccounts(List<RawAccountRecord> accounts) {
        this.accounts = accounts;
    }

    public void willReturnTrades(List<RawTradeRecord> trades) {
        this.trades = trades;
    }

    public void willReturnHoldings(RawHoldingBatch holdingBatch) {
        this.holdingBatch = holdingBatch;
    }

    public void willFailSyncWith(RuntimeException exception) {
        this.syncFailure = exception;
    }

    public void willReturnOrganizations(List<RawOrganizationRecord> organizations) {
        this.organizations = organizations;
    }

    public void willLoginAs(String orgCode, String orgName) {
        this.loginResult = new BrokerLoginResult(loginResult.mockConnectionId(), orgCode, orgName);
    }

    @Override
    public BrokerLoginResult login(String loginId, String password) {
        if (loginAuthFails) {
            throw new BrokerFeedAuthFailedException(new RuntimeException("401"));
        }
        if (loginServerFails) {
            throw new BrokerInfraException(ErrorType.EXTERNAL_ERROR, "목 증권사 서버 인증 중 오류가 발생했습니다.", new RuntimeException("연결 실패"));
        }
        return loginResult;
    }

    @Override
    public List<RawAccountRecord> fetchAccounts(String accessToken, String orgCode) {
        if (syncFailure != null) {
            throw syncFailure;
        }
        return accounts;
    }

    @Override
    public List<RawTradeRecord> fetchTrades(String accessToken, String accountNum) {
        return trades;
    }

    @Override
    public RawHoldingBatch fetchHoldings(String accessToken, String accountNum) {
        return holdingBatch;
    }

    @Override
    public List<RawOrganizationRecord> fetchOrganizations() {
        return organizations;
    }
}
