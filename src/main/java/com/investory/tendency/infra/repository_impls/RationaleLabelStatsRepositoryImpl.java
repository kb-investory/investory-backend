package com.investory.tendency.infra.repository_impls;

import com.investory.tendency.domain.constant.RationaleLabelType;
import com.investory.tendency.domain.repositories.RationaleLabelStatsRepository;
import com.investory.tendency.infra.entities.RationaleLabelCountRow;
import com.investory.tendency.infra.mappers.RationaleLabelStatsMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class RationaleLabelStatsRepositoryImpl implements RationaleLabelStatsRepository {

    private final RationaleLabelStatsMapper rationaleLabelStatsMapper;

    public RationaleLabelStatsRepositoryImpl(RationaleLabelStatsMapper rationaleLabelStatsMapper) {
        this.rationaleLabelStatsMapper = rationaleLabelStatsMapper;
    }

    @Override
    public Map<RationaleLabelType, Long> countByUserAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        try {
            List<RationaleLabelCountRow> rows = rationaleLabelStatsMapper.countByUserAndDateRange(userId, startDate, endDate);
            return rows.stream()
                    // rationale_label이 NULL인 레거시 행이 있더라도 집계가 죽지 않도록 방어.
                    .filter(row -> row.getRationaleLabel() != null)
                    .collect(Collectors.toMap(RationaleLabelCountRow::getRationaleLabel, RationaleLabelCountRow::getCount));
        } catch (DataAccessException e) {
            throw new TendencyInfraException("판단 근거 라벨 통계를 조회하는 중 오류가 발생했습니다.", e);
        }
    }
}
