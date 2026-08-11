package com.investory.journal.infra.repository_impls;

import com.investory.journal.domain.models.JournalTradeNote;
import com.investory.journal.domain.repositories.JournalTradeNoteRepository;
import com.investory.journal.infra.entities.JournalTradeNoteRow;
import com.investory.journal.infra.entities.RationaleLabelCountRow;
import com.investory.journal.infra.exception.JournalInfraException;
import com.investory.journal.infra.mappers.JournalTradeNoteMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class JournalTradeNoteRepositoryImpl implements JournalTradeNoteRepository {

    private final JournalTradeNoteMapper journalTradeNoteMapper;

    public JournalTradeNoteRepositoryImpl(JournalTradeNoteMapper journalTradeNoteMapper) {
        this.journalTradeNoteMapper = journalTradeNoteMapper;
    }

    @Override
    public List<JournalTradeNote> findByTradeIds(List<Long> tradeIds) {
        // MyBatis <foreach>로 만든 IN (...) 절에 빈 컬렉션을 넘기면 예외가 나므로 매퍼 호출 전에 막는다.
        if (tradeIds.isEmpty()) {
            return List.of();
        }
        try {
            return journalTradeNoteMapper.findByTradeIds(tradeIds).stream()
                    .map(JournalTradeNoteRow::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            throw new JournalInfraException("거래 판단 근거를 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public List<JournalTradeNote> findByJournalId(Long journalId) {
        try {
            return journalTradeNoteMapper.findByJournalId(journalId).stream()
                    .map(JournalTradeNoteRow::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            throw new JournalInfraException("거래 판단 근거를 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public void saveAll(List<JournalTradeNote> notes) {
        // 빈 리스트로 INSERT ... VALUES <foreach>를 만들면 SQL 문법 오류가 나므로 매퍼 호출 전에 막는다.
        if (notes.isEmpty()) {
            return;
        }
        List<JournalTradeNoteRow> rows = notes.stream()
                .map(JournalTradeNoteRow::from)
                .collect(Collectors.toList());
        try {
            journalTradeNoteMapper.insertAll(rows);
        } catch (DataAccessException e) {
            throw new JournalInfraException("거래 판단 근거를 저장하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public void deleteByTradeIds(List<Long> tradeIds) {
        // 빈 리스트로 IN (...) 절을 만들면 SQL 문법 오류가 나므로 매퍼 호출 전에 막는다.
        if (tradeIds.isEmpty()) {
            return;
        }
        try {
            journalTradeNoteMapper.deleteByTradeIds(tradeIds);
        } catch (DataAccessException e) {
            throw new JournalInfraException("거래 판단 근거를 삭제하는 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public Map<String, Long> countRationaleLabelsByUserAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        try {
            List<RationaleLabelCountRow> rows = journalTradeNoteMapper.countRationaleLabelsByUserAndDateRange(userId, startDate, endDate);
            // 같은 원본 문자열이 여러 행으로 나뉘어 올 가능성은 없지만, 방어적으로 summingLong으로 합산한다.
            return rows.stream()
                    .collect(Collectors.groupingBy(
                            RationaleLabelCountRow::getRationaleLabel,
                            Collectors.summingLong(RationaleLabelCountRow::getCount)));
        } catch (DataAccessException e) {
            throw new JournalInfraException("판단 근거 라벨 통계를 조회하는 중 오류가 발생했습니다.", e);
        }
    }
}
