package com.investory.principle.infra.mappers;

import com.investory.principle.infra.entities.PrincipleSetItemRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PrincipleSetItemMapper {
    List<PrincipleSetItemRow> findBySetId(@Param("principleSetId") Long principleSetId);
    void insertAll(@Param("items") List<PrincipleSetItemRow> items);
}
