package com.investory.ledger.domain.services;

import com.investory.ledger.domain.constant.TradeSide;
import com.investory.ledger.domain.model.HoldingFixture;
import com.investory.ledger.domain.model.Trade;
import com.investory.ledger.domain.model.TradeFixture;
import com.investory.ledger.domain.ports.FakeJournalNotePort;
import com.investory.ledger.domain.repositories.FakeHoldingSnapshotRepository;
import com.investory.ledger.domain.repositories.FakeTradeMatchRepository;
import com.investory.ledger.domain.repositories.FakeTradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountDataCleanupServiceTest {

    private static final Long ACCOUNT_ID = 1000L;

    private FakeTradeRepository tradeRepository;
    private FakeTradeMatchRepository tradeMatchRepository;
    private FakeHoldingSnapshotRepository holdingSnapshotRepository;
    private FakeJournalNotePort journalNotePort;
    private AccountDataCleanupService accountDataCleanupService;

    @BeforeEach
    void setUp() {
        tradeRepository = new FakeTradeRepository();
        tradeMatchRepository = new FakeTradeMatchRepository();
        holdingSnapshotRepository = new FakeHoldingSnapshotRepository();
        journalNotePort = new FakeJournalNotePort();
        accountDataCleanupService = new AccountDataCleanupService(
                tradeRepository, tradeMatchRepository, holdingSnapshotRepository, journalNotePort);
    }

    @Test
    void 계좌의_거래를_전부_지운다() {
        tradeRepository.add(TradeFixture.trade(ACCOUNT_ID, 1L, TradeSide.BUY, "ext-1", Instant.now()));
        tradeRepository.add(TradeFixture.trade(ACCOUNT_ID, 1L, TradeSide.SELL, "ext-2", Instant.now()));

        accountDataCleanupService.deleteAccountData(ACCOUNT_ID);

        assertTrue(tradeRepository.findTradeIdsByAccountId(ACCOUNT_ID).isEmpty());
    }

    @Test
    void 거래를_지우기_전에_그_거래ID_목록으로_journal_근거_삭제를_요청한다() {
        Trade trade = tradeRepository.save(TradeFixture.trade(ACCOUNT_ID, 1L, TradeSide.BUY, "ext-1", Instant.now()));

        accountDataCleanupService.deleteAccountData(ACCOUNT_ID);

        assertEquals(1, journalNotePort.deleteCalls().size());
        assertEquals(List.of(trade.getTradeId()), journalNotePort.deleteCalls().get(0));
    }

    @Test
    void 거래가_없는_계좌도_안전하게_처리된다() {
        accountDataCleanupService.deleteAccountData(ACCOUNT_ID);

        assertEquals(1, journalNotePort.deleteCalls().size());
        assertTrue(journalNotePort.deleteCalls().get(0).isEmpty());
    }

    @Test
    void 계좌의_매칭과_보유스냅샷도_함께_지운다() {
        holdingSnapshotRepository.add(HoldingFixture.holding(
                ACCOUNT_ID, 1L, BigDecimal.TEN, BigDecimal.valueOf(10000), BigDecimal.valueOf(11000), LocalDate.now()));

        accountDataCleanupService.deleteAccountData(ACCOUNT_ID);

        assertEquals(1, tradeMatchRepository.deletedByAccountIdCalls().size());
        assertTrue(holdingSnapshotRepository.findLatestByAccountIds(List.of(ACCOUNT_ID), null).isEmpty());
    }
}
