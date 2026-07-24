package com.kbinvestory.backend.market.infra.repository_impls;

import com.kbinvestory.backend.market.domain.model.Stock;
import com.kbinvestory.backend.market.domain.repositories.StockRepository;
import com.kbinvestory.backend.market.domain.services.dto.query.StockSearchQuery;
import com.kbinvestory.backend.market.infra.entities.StockRow;
import com.kbinvestory.backend.market.infra.mappers.StockMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class StockRepositoryImpl implements StockRepository {

    private final StockMapper stockMapper;

    public StockRepositoryImpl(StockMapper stockMapper) {
        this.stockMapper = stockMapper;
    }

    @Override
    public List<Stock> search(StockSearchQuery query) {
        return stockMapper.search(query).stream()
                .map(StockRow::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long count(StockSearchQuery query) {
        return stockMapper.count(query);
    }
}