package com.investory.journal.infra.port_impls;

import com.investory.auth.domain.ports.JournalCleanupPort;
import com.investory.journal.domain.services.JournalService;
import org.springframework.stereotype.Component;

// auth.domain.ports를 참조하는 유일한 지점 — 받는 즉시 journal 자신의 서비스 호출로 위임한다(§5).
@Component
public class JournalCleanupPortImpl implements JournalCleanupPort {

    private final JournalService journalService;

    public JournalCleanupPortImpl(JournalService journalService) {
        this.journalService = journalService;
    }

    @Override
    public void deleteAllJournals(Long userId) {
        journalService.deleteAllJournals(userId);
    }
}
