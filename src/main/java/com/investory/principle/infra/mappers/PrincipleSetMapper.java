package com.investory.principle.infra.mappers;

import com.investory.principle.infra.entities.PrincipleSetRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PrincipleSetMapper {
    List<PrincipleSetRow> findActiveByUserId(@Param("userId") Long userId);
    int findMaxVersionNo(@Param("userId") Long userId);
    void archiveActive(@Param("userId") Long userId);
    void insert(PrincipleSetRow row);
}
