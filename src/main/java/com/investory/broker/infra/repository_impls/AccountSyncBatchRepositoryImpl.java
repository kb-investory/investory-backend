package com.investory.broker.infra.repository_impls;

import com.investory.broker.domain.model.AccountSyncBatch;
import com.investory.broker.domain.repositories.AccountSyncBatchRepository;
import com.investory.broker.infra.entities.AccountSyncBatchRow;
import com.investory.broker.infra.exception.BrokerInfraException;
import com.investory.broker.infra.mappers.AccountSyncBatchMapper;
import com.investory.core.exception.ErrorType;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AccountSyncBatchRepositoryImpl implements AccountSyncBatchRepository {

    private final AccountSyncBatchMapper accountSyncBatchMapper;

    public AccountSyncBatchRepositoryImpl(AccountSyncBatchMapper accountSyncBatchMapper) {
        this.accountSyncBatchMapper = accountSyncBatchMapper;
    }

    @Override
    public Long create(Long connectionId) {
        AccountSyncBatchRow row = new AccountSyncBatchRow();
        row.setConnectionId(connectionId);
        try {
            accountSyncBatchMapper.insert(row);
        } catch (DataAccessException e) {
            throw new BrokerInfraException(ErrorType.INTERNAL_ERROR, "동기화 배치를 생성하는 중 오류가 발생했습니다.", e);
        }
        return row.getSyncBatchId();
    }

    @Override
    public void markSuccess(Long syncBatchId) {
        try {
            accountSyncBatchMapper.markSuccess(syncBatchId);
        } catch (DataAccessException e) {
            throw new BrokerInfraException(ErrorType.INTERNAL_ERROR, "동기화 배치를 성공 처리하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public void markFailed(Long syncBatchId, String errorMessage) {
        try {
            accountSyncBatchMapper.markFailed(syncBatchId, errorMessage);
        } catch (DataAccessException e) {
            throw new BrokerInfraException(ErrorType.INTERNAL_ERROR, "동기화 배치를 실패 처리하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public Optional<AccountSyncBatch> findLatestByConnectionId(Long connectionId) {
        try {
            return accountSyncBatchMapper.findLatestByConnectionId(connectionId).stream()
                    .map(AccountSyncBatchRow::toDomain)
                    .findFirst();
        } catch (DataAccessException e) {
            throw new BrokerInfraException(ErrorType.INTERNAL_ERROR, "최근 동기화 이력을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public boolean existsInProgress(Long connectionId) {
        try {
            return accountSyncBatchMapper.countInProgress(connectionId) > 0;
        } catch (DataAccessException e) {
            throw new BrokerInfraException(ErrorType.INTERNAL_ERROR, "동기화 진행 여부를 확인하는 중 오류가 발생했습니다.", e);
        }
    }
}
