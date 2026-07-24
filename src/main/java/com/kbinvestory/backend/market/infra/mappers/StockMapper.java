package com.kbinvestory.backend.market.infra.mappers;

import com.kbinvestory.backend.market.domain.services.dto.query.StockSearchQuery;
import com.kbinvestory.backend.market.infra.entities.StockRow;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface StockMapper {
    List<StockRow> search(StockSearchQuery query);
    long count(StockSearchQuery query);
}