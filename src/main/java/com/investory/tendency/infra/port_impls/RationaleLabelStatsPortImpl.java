package com.investory.tendency.infra.port_impls;

import com.investory.tendency.infra.exception.TendencyInfraException;
import com.investory.tendency.domain.constant.RationaleLabelType;
import com.investory.tendency.domain.ports.RationaleLabelStatsPort;
import com.investory.tendency.infra.entities.RationaleLabelCountRow;
import com.investory.tendency.infra.mappers.RationaleLabelStatsMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// journal 도메인의 journal_trade_notes/investment_journals 테이블을 직접 읽는 유일한 지점.
// journal.domain.JournalTradeNote가 rationale_label을 노출하지 않아 부득이하게 journal의 스키마를
// 직접 알아야 한다 — journal 도메인 코드는 수정하지 않고, 이 어댑터 안에만 그 지식을 가둔다.
@Component
public class RationaleLabelStatsPortImpl implements RationaleLabelStatsPort {

    private static final Logger log = LoggerFactory.getLogger(RationaleLabelStatsPortImpl.class);

    private final RationaleLabelStatsMapper rationaleLabelStatsMapper;

    public RationaleLabelStatsPortImpl(RationaleLabelStatsMapper rationaleLabelStatsMapper) {
        this.rationaleLabelStatsMapper = rationaleLabelStatsMapper;
    }

    @Override
    public Map<RationaleLabelType, Long> countByUserAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        try {
            List<RationaleLabelCountRow> rows = rationaleLabelStatsMapper.countByUserAndDateRange(userId, startDate, endDate);
            // DB에 'unclassified'처럼 소문자로 저장된 값도 있어 대문자로 정규화한 뒤 enum으로 변환한다.
            // 정규화 후 같은 enum으로 모이는 행이 여러 개일 수 있어 toMap 대신 groupingBy+summingLong으로 합산한다.
            return rows.stream()
                    .collect(Collectors.groupingBy(
                            row -> parseLabel(row.getRationaleLabel()),
                            Collectors.summingLong(RationaleLabelCountRow::getCount)));
        } catch (DataAccessException e) {
            throw new TendencyInfraException("판단 근거 라벨 통계를 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    // enum에 없는 값(NULL, 오타, 대소문자 변형 등)이 들어와도 전체 조회가 죽지 않도록 UNCLASSIFIED로 안전하게 취급한다.
    private RationaleLabelType parseLabel(String rawLabel) {
        if (rawLabel == null) {
            return RationaleLabelType.UNCLASSIFIED;
        }
        try {
            return RationaleLabelType.valueOf(rawLabel.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("알 수 없는 rationale_label_type 값 '{}' — UNCLASSIFIED로 집계합니다.", rawLabel);
            return RationaleLabelType.UNCLASSIFIED;
        }
    }
}
