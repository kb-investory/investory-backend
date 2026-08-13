package com.investory.tendency.infra.port_impls;

import com.investory.journal.domain.repositories.JournalTradeNoteRepository;
import com.investory.journal.infra.exception.JournalInfraException;
import com.investory.tendency.infra.exception.TendencyInfraException;
import com.investory.tendency.domain.constant.RationaleLabelType;
import com.investory.tendency.domain.ports.RationaleLabelStatsPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

// journal 도메인이 공개한 JournalTradeNoteRepository를 통해서만 데이터를 받는다 — journal의 테이블/SQL을
// 직접 알지 않는다. 라벨 원본 문자열의 대소문자 정규화 + enum 변환은 journal이 아닌 여기(tendency)의 책임이다.
@Component
public class RationaleLabelStatsPortImpl implements RationaleLabelStatsPort {

    private static final Logger log = LoggerFactory.getLogger(RationaleLabelStatsPortImpl.class);

    private final JournalTradeNoteRepository journalTradeNoteRepository;

    public RationaleLabelStatsPortImpl(JournalTradeNoteRepository journalTradeNoteRepository) {
        this.journalTradeNoteRepository = journalTradeNoteRepository;
    }

    @Override
    public Map<RationaleLabelType, Long> countByUserAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        Map<String, Long> rawCounts;
        try {
            rawCounts = journalTradeNoteRepository.countRationaleLabelsByUserAndDateRange(userId, startDate, endDate);
        } catch (JournalInfraException e) {
            throw new TendencyInfraException("판단 근거 라벨 통계를 조회하는 중 오류가 발생했습니다.", e);
        }

        // DB에 'unclassified'처럼 소문자로 저장된 값도 있어 대문자로 정규화한 뒤 enum으로 변환한다.
        // 정규화 후 같은 enum으로 모이는 항목이 여러 개일 수 있어 toMap 대신 groupingBy+summingLong으로 합산한다.
        return rawCounts.entrySet().stream()
                .collect(Collectors.groupingBy(
                        entry -> parseLabel(entry.getKey()),
                        Collectors.summingLong(Map.Entry::getValue)));
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
