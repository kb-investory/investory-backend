package com.investory.tendency.infra.port_impls;

import com.investory.journal.domain.repositories.JournalRepository;
import com.investory.journal.infra.exception.JournalInfraException;
import com.investory.tendency.domain.ports.JournalRationalePort;
import com.investory.tendency.infra.exception.TendencyInfraException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

// journal 도메인이 공개한 JournalRepository를 통해서만 데이터를 받는다 — journal의 테이블/SQL을 직접 알지 않는다.
@Component
public class JournalRationalePortImpl implements JournalRationalePort {

    private final JournalRepository journalRepository;

    public JournalRationalePortImpl(JournalRepository journalRepository) {
        this.journalRepository = journalRepository;
    }

    @Override
    public int countJournalsInRange(Long userId, LocalDate startDate, LocalDate endDate) {
        try {
            return journalRepository.countByUserAndDateRange(userId, startDate, endDate);
        } catch (JournalInfraException e) {
            throw new TendencyInfraException("투자일지 건수를 조회하는 중 오류가 발생했습니다.", e);
        }
    }
}
