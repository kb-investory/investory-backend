package com.kbinvestory.backend.account.infra.mappers;

import com.kbinvestory.backend.account.infra.entities.AccountConnectionRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AccountConnectionMapper {
    AccountConnectionRow findByUserIdAndProviderId(@Param("userId") Long userId, @Param("providerId") Long providerId);
    void insert(AccountConnectionRow row);
}
