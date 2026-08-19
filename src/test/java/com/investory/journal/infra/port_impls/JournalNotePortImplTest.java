package com.investory.journal.infra.port_impls;

import com.investory.journal.domain.models.JournalTradeNoteFixture;
import com.investory.journal.domain.ports.FakeMarketDataPort;
import com.investory.journal.domain.ports.FakeRationaleLabelingPort;
import com.investory.journal.domain.ports.FakeTradeLedgerPort;
import com.investory.journal.domain.repositories.FakeJournalRepository;
import com.investory.journal.domain.repositories.FakeJournalTradeNoteRepository;
import com.investory.journal.domain.services.FakeTransactionManager;
import com.investory.journal.domain.services.JournalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JournalNotePortImplTest {

    private FakeJournalTradeNoteRepository journalTradeNoteRepository;
    private JournalNotePortImpl journalNotePort;

    @BeforeEach
    void setUp() {
        FakeJournalRepository journalRepository = new FakeJournalRepository();
        journalTradeNoteRepository = new FakeJournalTradeNoteRepository();
        JournalService journalService = new JournalService(journalRepository, journalTradeNoteRepository,
                new FakeTradeLedgerPort(), new FakeMarketDataPort(), new FakeRationaleLabelingPort(),
                new FakeTransactionManager());
        journalNotePort = new JournalNotePortImpl(journalService);
    }

    @Test
    void tradeId_목록을_받으면_해당_매매근거를_지운다() {
        journalTradeNoteRepository.add(JournalTradeNoteFixture.note(501L, "근거1", Instant.now()));

        journalNotePort.deleteNotesByTradeIds(List.of(501L));

        assertTrue(journalTradeNoteRepository.findByTradeIds(List.of(501L)).isEmpty());
    }
}
