package com.investory.broker.infra.repository_impls;

import com.investory.broker.domain.model.AccountSyncBatch;
import com.investory.broker.domain.repositories.AccountSyncBatchRepository;
import com.investory.broker.infra.entities.AccountSyncBatchRow;
import com.investory.broker.infra.exception.BrokerInfraException;
import com.investory.broker.infra.mappers.AccountSyncBatchMapper;
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
            throw new BrokerInfraException(e);
        }
        return row.getSyncBatchId();
    }

    @Override
    public void markSuccess(Long syncBatchId) {
        try {
            accountSyncBatchMapper.markSuccess(syncBatchId);
        } catch (DataAccessException e) {
            throw new BrokerInfraException(e);
        }
    }

    @Override
    public void markFailed(Long syncBatchId, String errorMessage) {
        try {
            accountSyncBatchMapper.markFailed(syncBatchId, errorMessage);
        } catch (DataAccessException e) {
            throw new BrokerInfraException(e);
        }
    }

    @Override
    public Optional<AccountSyncBatch> findLatestByConnectionId(Long connectionId) {
        try {
            return accountSyncBatchMapper.findLatestByConnectionId(connectionId).stream()
                    .map(AccountSyncBatchRow::toDomain)
                    .findFirst();
        } catch (DataAccessException e) {
            throw new BrokerInfraException(e);
        }
    }
}
