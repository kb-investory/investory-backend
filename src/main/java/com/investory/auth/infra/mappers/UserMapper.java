package com.investory.auth.infra.mappers;

import com.investory.auth.infra.entities.UserRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    UserRow findBySocialProviderAndSocialSubject(@Param("socialProvider") String socialProvider, @Param("socialSubject") String socialSubject);
    UserRow findByUserId(@Param("userId") Long userId);
    void insert(UserRow row);
    void update(UserRow row);
}
