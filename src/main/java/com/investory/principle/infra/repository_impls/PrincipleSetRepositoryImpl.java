package com.investory.principle.infra.repository_impls;

import com.investory.principle.domain.model.PrincipleSet;
import com.investory.principle.domain.model.PrincipleSetItem;
import com.investory.principle.domain.repositories.PrincipleSetRepository;
import com.investory.principle.infra.entities.PrincipleSetItemRow;
import com.investory.principle.infra.entities.PrincipleSetRow;
import com.investory.principle.infra.exception.PrincipleInfraException;
import com.investory.principle.infra.mappers.PrincipleSetItemMapper;
import com.investory.principle.infra.mappers.PrincipleSetMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class PrincipleSetRepositoryImpl implements PrincipleSetRepository {

    private final PrincipleSetMapper principleSetMapper;
    private final PrincipleSetItemMapper principleSetItemMapper;

    public PrincipleSetRepositoryImpl(PrincipleSetMapper principleSetMapper, PrincipleSetItemMapper principleSetItemMapper) {
        this.principleSetMapper = principleSetMapper;
        this.principleSetItemMapper = principleSetItemMapper;
    }

    @Override
    public Optional<PrincipleSet> findActiveByUserId(Long userId) {
        try {
            Optional<PrincipleSetRow> row = principleSetMapper.findActiveByUserId(userId).stream().findFirst();
            if (row.isEmpty()) {
                return Optional.empty();
            }
            List<PrincipleSetItem> items = principleSetItemMapper.findBySetId(row.get().getPrincipleSetId()).stream()
                    .map(PrincipleSetItemRow::toDomain)
                    .collect(Collectors.toList());
            return Optional.of(row.get().toDomain(items));
        } catch (DataAccessException e) {
            throw new PrincipleInfraException("활성 투자원칙을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public int findMaxVersionNo(Long userId) {
        try {
            return principleSetMapper.findMaxVersionNo(userId);
        } catch (DataAccessException e) {
            throw new PrincipleInfraException("투자원칙 버전을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public void archiveActive(Long userId) {
        try {
            principleSetMapper.archiveActive(userId);
        } catch (DataAccessException e) {
            throw new PrincipleInfraException("기존 투자원칙을 보관 처리하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public Long save(PrincipleSet principleSet) {
        PrincipleSetRow row = PrincipleSetRow.from(principleSet);
        try {
            principleSetMapper.insert(row);
            if (!principleSet.getItems().isEmpty()) {
                List<PrincipleSetItemRow> itemRows = principleSet.getItems().stream()
                        .map(item -> PrincipleSetItemRow.from(item, row.getPrincipleSetId()))
                        .collect(Collectors.toList());
                principleSetItemMapper.insertAll(itemRows);
            }
        } catch (DataAccessException e) {
            throw new PrincipleInfraException("투자원칙을 저장하는 중 오류가 발생했습니다.", e);
        }
        return row.getPrincipleSetId();
    }
}
