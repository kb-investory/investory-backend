package com.investory.ledger.infra.repository_impls;

import com.investory.ledger.domain.model.Holding;
import com.investory.ledger.domain.repositories.HoldingSnapshotRepository;
import com.investory.ledger.infra.entities.HoldingSnapshotRow;
import com.investory.ledger.infra.exception.LedgerInfraException;
import com.investory.ledger.infra.mappers.HoldingSnapshotMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class HoldingSnapshotRepositoryImpl implements HoldingSnapshotRepository {

    private final HoldingSnapshotMapper holdingSnapshotMapper;

    public HoldingSnapshotRepositoryImpl(HoldingSnapshotMapper holdingSnapshotMapper) {
        this.holdingSnapshotMapper = holdingSnapshotMapper;
    }

    @Override
    public List<Holding> findLatestByAccountIds(List<Long> accountIds, Long securityId) {
        try {
            return holdingSnapshotMapper.findLatestByAccountIds(accountIds, securityId).stream()
                    .map(HoldingSnapshotRow::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            throw new LedgerInfraException("보유현황을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public void upsert(Holding holding) {
        try {
            holdingSnapshotMapper.upsert(HoldingSnapshotRow.from(holding));
        } catch (DataAccessException e) {
            throw new LedgerInfraException("보유현황을 저장하는 중 오류가 발생했습니다.", e);
        }
    }
}
