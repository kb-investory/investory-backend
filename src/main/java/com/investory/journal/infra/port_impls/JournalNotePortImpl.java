package com.investory.journal.infra.port_impls;

import com.investory.journal.domain.services.JournalService;
import com.investory.ledger.domain.ports.JournalNotePort;
import org.springframework.stereotype.Component;

import java.util.List;

// ledger.domain.ports를 참조하는 유일한 지점 — 받는 즉시 journal 자신의 서비스 호출로 위임한다(§5).
// JournalService는 이 호출이 연동 해지에서 온 것인지 모른다.
@Component
public class JournalNotePortImpl implements JournalNotePort {

    private final JournalService journalService;

    public JournalNotePortImpl(JournalService journalService) {
        this.journalService = journalService;
    }

    @Override
    public void deleteNotesByTradeIds(List<Long> tradeIds) {
        journalService.deleteNotesByTradeIds(tradeIds);
    }
}
