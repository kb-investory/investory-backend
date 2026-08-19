package com.investory.ledger.infra.mappers;

import com.investory.ledger.infra.entities.HoldingSnapshotRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface HoldingSnapshotMapper {

    List<HoldingSnapshotRow> findLatestByAccountIds(@Param("accountIds") List<Long> accountIds,
                                                       @Param("securityId") Long securityId);

    void upsert(HoldingSnapshotRow row);

    void deleteByAccountId(@Param("accountId") Long accountId);
}
