package com.investory.journal.domain.services;

import com.investory.journal.domain.constant.MarketMood;
import com.investory.journal.domain.constant.TradeSide;
import com.investory.journal.domain.exception.JournalErrorCode;
import com.investory.journal.domain.exception.JournalException;
import com.investory.journal.domain.models.Journal;
import com.investory.journal.domain.models.JournalTradeNote;
import com.investory.journal.domain.ports.FakeMarketDataPort;
import com.investory.journal.domain.ports.FakeTradeLedgerPort;
import com.investory.journal.domain.ports.dto.SecurityInfo;
import com.investory.journal.domain.ports.dto.TradeCountInfo;
import com.investory.journal.domain.ports.dto.TradeInfo;
import com.investory.journal.domain.repositories.FakeJournalRepository;
import com.investory.journal.domain.repositories.FakeJournalTradeNoteRepository;
import com.investory.journal.domain.services.dto.query.GetJournalDetailQuery;
import com.investory.journal.domain.services.dto.query.GetJournalEntriesQuery;
import com.investory.journal.domain.services.dto.result.JournalDetailResult;
import com.investory.journal.domain.services.dto.result.JournalEntryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JournalServiceTest {

    private static final Long USER_ID = 100L;

    private FakeJournalRepository journalRepository;
    private FakeJournalTradeNoteRepository journalTradeNoteRepository;
    private FakeTradeLedgerPort tradeLedgerPort;
    private FakeMarketDataPort marketDataPort;
    private JournalService journalService;

    @BeforeEach
    void setUp() {
        journalRepository = new FakeJournalRepository();
        journalTradeNoteRepository = new FakeJournalTradeNoteRepository();
        tradeLedgerPort = new FakeTradeLedgerPort();
        marketDataPort = new FakeMarketDataPort();
        journalService = new JournalService(journalRepository, journalTradeNoteRepository, tradeLedgerPort, marketDataPort);
    }

    @Test
    void ledger의_tradeCount와_journal의_tradeNoteCount를_병합해서_반환한다() {
        LocalDate journalDate = LocalDate.of(2026, 7, 10);
        journalRepository.add(journal(journalDate, journalDate.atTime(20, 0), 3600));
        tradeLedgerPort.add(new TradeCountInfo(journalDate, 5));

        List<JournalEntryResult> results = getEntries(journalDate, journalDate);

        assertEquals(1, results.size());
        assertEquals(5, results.get(0).tradeCount());
        assertEquals(2, results.get(0).tradeNoteCount());
    }

    @Test
    void ledger에_해당_날짜_거래_기록이_없으면_tradeCount는_0이다() {
        LocalDate journalDate = LocalDate.of(2026, 7, 10);
        journalRepository.add(journal(journalDate, journalDate.atTime(20, 0), 3600));

        List<JournalEntryResult> results = getEntries(journalDate, journalDate);

        assertEquals(0, results.get(0).tradeCount());
    }

    @Test
    void 일지_대상_날짜와_같은_날_작성했으면_isBackfilled는_false다() {
        LocalDate journalDate = LocalDate.of(2026, 7, 10);
        journalRepository.add(journal(journalDate, journalDate.atTime(23, 59), 3600));

        List<JournalEntryResult> results = getEntries(journalDate, journalDate);

        assertFalse(results.get(0).isBackfilled());
    }

    @Test
    void 일지_대상_날짜보다_늦게_작성했으면_isBackfilled는_true다() {
        LocalDate journalDate = LocalDate.of(2026, 7, 10);
        journalRepository.add(journal(journalDate, journalDate.plusDays(3).atStartOfDay(), 3600));

        List<JournalEntryResult> results = getEntries(journalDate, journalDate);

        assertTrue(results.get(0).isBackfilled());
    }

    @Test
    void 현재_시각이_editableUntilAt_이전이면_isEditable은_true다() {
        LocalDate journalDate = LocalDate.of(2026, 7, 10);
        journalRepository.add(journal(journalDate, journalDate.atTime(10, 0), 3600));

        List<JournalEntryResult> results = getEntries(journalDate, journalDate);

        assertTrue(results.get(0).isEditable());
    }

    @Test
    void 현재_시각이_editableUntilAt_이후면_isEditable은_false다() {
        LocalDate journalDate = LocalDate.of(2026, 7, 10);
        journalRepository.add(journal(journalDate, journalDate.atTime(10, 0), -3600));

        List<JournalEntryResult> results = getEntries(journalDate, journalDate);

        assertFalse(results.get(0).isEditable());
    }

    @Test
    void startDate가_endDate보다_늦으면_예외를_던진다() {
        LocalDate startDate = LocalDate.of(2026, 7, 20);
        LocalDate endDate = LocalDate.of(2026, 7, 10);

        JournalException exception = assertThrows(JournalException.class,
                () -> journalService.getEntries(new GetJournalEntriesQuery(USER_ID, startDate, endDate)));

        assertEquals(JournalErrorCode.INVALID_DATE_RANGE, exception.getErrorCode());
    }

    @Test
    void 일지가_있으면_journal_필드를_채워서_반환한다() {
        LocalDate journalDate = LocalDate.of(2026, 7, 10);
        journalRepository.add(journal(journalDate, journalDate.atTime(18, 20), 3600));

        JournalDetailResult result = journalService.getDetail(new GetJournalDetailQuery(USER_ID, journalDate));

        assertFalse(result.canCreate());
        assertEquals(1L, result.journal().journalId());
        assertEquals("시장에 대한 생각", result.journal().marketThought());
        assertTrue(result.trades().isEmpty());
    }

    @Test
    void 일지가_없고_과거_날짜면_canCreate는_true다() {
        LocalDate pastDate = LocalDate.of(2020, 1, 1);

        JournalDetailResult result = journalService.getDetail(new GetJournalDetailQuery(USER_ID, pastDate));

        assertTrue(result.canCreate());
        assertNull(result.journal());
    }

    @Test
    void 일지가_없고_미래_날짜면_canCreate는_false다() {
        LocalDate futureDate = LocalDate.of(2099, 1, 1);

        JournalDetailResult result = journalService.getDetail(new GetJournalDetailQuery(USER_ID, futureDate));

        assertFalse(result.canCreate());
        assertNull(result.journal());
    }

    @Test
    void 거래에_종목정보와_근거를_병합해서_반환한다() {
        LocalDate date = LocalDate.of(2026, 7, 10);
        Instant tradedAt1 = date.atTime(10, 15).toInstant(ZoneOffset.UTC);
        Instant tradedAt2 = date.atTime(14, 10).toInstant(ZoneOffset.UTC);
        tradeLedgerPort.add(
                new TradeInfo(501L, 101L, TradeSide.BUY, 10, new BigDecimal("72000"), tradedAt1),
                new TradeInfo(502L, 102L, TradeSide.SELL, 3, new BigDecimal("195000"), tradedAt2)
        );
        marketDataPort.add(
                new SecurityInfo(101L, "005930", "삼성전자"),
                new SecurityInfo(102L, "000660", "SK하이닉스")
        );
        journalTradeNoteRepository.add(JournalTradeNote.of(
                701L, 305L, 501L, "HBM 시장의 장기 성장 가능성이 높다고 판단했다.", tradedAt1, tradedAt1));

        JournalDetailResult result = journalService.getDetail(new GetJournalDetailQuery(USER_ID, date));

        assertEquals(2, result.trades().size());
        var withNote = result.trades().stream().filter(t -> t.tradeId().equals(501L)).findFirst().orElseThrow();
        assertEquals("005930", withNote.securityCode());
        assertEquals("삼성전자", withNote.securityName());
        assertEquals("HBM 시장의 장기 성장 가능성이 높다고 판단했다.", withNote.note().rationaleText());

        var withoutNote = result.trades().stream().filter(t -> t.tradeId().equals(502L)).findFirst().orElseThrow();
        assertEquals("000660", withoutNote.securityCode());
        assertNull(withoutNote.note());
    }

    @Test
    void 거래의_종목_정보를_찾을_수_없으면_예외를_던진다() {
        LocalDate date = LocalDate.of(2026, 7, 10);
        Instant tradedAt = date.atTime(10, 15).toInstant(ZoneOffset.UTC);
        tradeLedgerPort.add(new TradeInfo(501L, 999L, TradeSide.BUY, 10, new BigDecimal("72000"), tradedAt));
        // marketDataPort에 999L 종목 정보를 등록하지 않음

        JournalException exception = assertThrows(JournalException.class,
                () -> journalService.getDetail(new GetJournalDetailQuery(USER_ID, date)));

        assertEquals(JournalErrorCode.SECURITY_NOT_FOUND, exception.getErrorCode());
    }

    private List<JournalEntryResult> getEntries(LocalDate startDate, LocalDate endDate) {
        return journalService.getEntries(new GetJournalEntriesQuery(USER_ID, startDate, endDate));
    }

    // editableUntilAtOffsetSeconds: editableUntilAt = now + offset (음수면 이미 지난 시각)
    private Journal journal(LocalDate journalDate, LocalDateTime createdAtLocal, long editableUntilAtOffsetSeconds) {
        Instant createdAt = createdAtLocal.toInstant(ZoneOffset.UTC);
        return Journal.of(
                1L,
                journalDate,
                "시장에 대한 생각",
                MarketMood.CALM,
                2,
                createdAt,
                createdAt,
                Instant.now().plusSeconds(editableUntilAtOffsetSeconds)
        );
    }
}
