package com.investory.broker.infra.mappers;

import com.investory.broker.infra.entities.InvestmentAccountRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InvestmentAccountMapper {
    void upsert(InvestmentAccountRow row);

    List<InvestmentAccountRow> findByConnectionId(@Param("connectionId") Long connectionId);

    List<InvestmentAccountRow> findByUserId(@Param("userId") Long userId);

    List<InvestmentAccountRow> findByIds(@Param("accountIds") List<Long> accountIds);

    List<InvestmentAccountRow> findByIdAndUserId(@Param("accountId") Long accountId, @Param("userId") Long userId);

    void updateAccountName(@Param("accountId") Long accountId, @Param("accountName") String accountName);

    void deleteByConnectionId(@Param("connectionId") Long connectionId);
}
