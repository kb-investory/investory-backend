package com.investory.broker.infra.mappers;

import com.investory.broker.infra.entities.AccountSyncBatchRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AccountSyncBatchMapper {
    void insert(AccountSyncBatchRow row);

    void markSuccess(@Param("syncBatchId") Long syncBatchId);

    void markFailed(@Param("syncBatchId") Long syncBatchId, @Param("errorMessage") String errorMessage);

    List<AccountSyncBatchRow> findLatestByConnectionId(@Param("connectionId") Long connectionId);
}
