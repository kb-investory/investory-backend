package com.investory.broker.infra.mappers;

import com.investory.broker.infra.entities.AccountSyncBatchRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AccountSyncBatchMapper {
    void insert(AccountSyncBatchRow row);

    void markSuccess(@Param("syncBatchId") Long syncBatchId);

    void markFailed(@Param("syncBatchId") Long syncBatchId, @Param("errorMessage") String errorMessage);
}
