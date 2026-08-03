package com.investory.broker.infra.mappers;

import com.investory.broker.infra.entities.InvestmentAccountRow;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InvestmentAccountMapper {
    void insert(InvestmentAccountRow row);
}
