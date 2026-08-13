package com.investory.journal.domain.services;

import com.investory.journal.domain.constant.MarketMood;
import com.investory.journal.domain.constant.TradeSide;
import com.investory.journal.domain.exception.JournalErrorCode;
import com.investory.journal.domain.exception.JournalException;
import com.investory.journal.domain.models.JournalFixture;
import com.investory.journal.domain.models.JournalTradeNote;
import com.investory.journal.domain.models.JournalTradeNoteFixture;
import com.investory.journal.domain.ports.FakeMarketDataPort;
import com.investory.journal.domain.ports.FakeRationaleLabelingPort;
import com.investory.journal.domain.ports.FakeTradeLedgerPort;
import com.investory.journal.domain.ports.dto.SecurityInfoFixture;
import com.investory.journal.domain.ports.dto.TradeCountInfo;
import com.investory.journal.domain.ports.dto.TradeInfoFixture;
import com.investory.journal.domain.ports.dto.TradeTimelineInfoFixture;
import com.investory.journal.domain.repositories.FakeJournalRepository;
import com.investory.journal.domain.repositories.FakeJournalTradeNoteRepository;
import com.investory.journal.domain.services.dto.command.CreateJournalCommand;
import com.investory.journal.domain.services.dto.command.TradeNoteCommand;
import com.investory.journal.domain.services.dto.command.UpdateJournalCommand;
import com.investory.journal.domain.services.dto.query.GetJournalByIdQuery;
import com.investory.journal.domain.services.dto.query.GetJournalDetailQuery;
import com.investory.journal.domain.services.dto.query.GetJournalEntriesQuery;
import com.investory.journal.domain.services.dto.query.GetTradeTimelineQuery;
import com.investory.journal.domain.services.dto.result.CreateJournalResult;
import com.investory.journal.domain.services.dto.result.JournalDetailResult;
import com.investory.journal.domain.services.dto.result.JournalEntryResult;
import com.investory.journal.domain.services.dto.result.TradeTimelineResult;
import com.investory.journal.domain.services.dto.result.UpdateJournalResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JournalServiceTest {

    private static final Long USER_ID = 100L;

    private FakeJournalRepository journalRepository;
    private FakeJournalTradeNoteRepository journalTradeNoteRepository;
    private FakeTradeLedgerPort tradeLedgerPort;
    private FakeMarketDataPort marketDataPort;
    private FakeRationaleLabelingPort rationaleLabelingPort;
    private JournalService journalService;

    @BeforeEach
    void setUp() {
        journalRepository = new FakeJournalRepository();
        journalTradeNoteRepository = new FakeJournalTradeNoteRepository();
        tradeLedgerPort = new FakeTradeLedgerPort();
        marketDataPort = new FakeMarketDataPort();
        rationaleLabelingPort = new FakeRationaleLabelingPort();
        journalService = new JournalService(journalRepository, journalTradeNoteRepository, tradeLedgerPort,
                marketDataPort, rationaleLabelingPort, new FakeTransactionManager());
    }

    @Test
    void ledger의_tradeCount와_journal의_tradeNoteCount를_병합해서_반환한다() {
        LocalDate journalDate = LocalDate.of(2026, 7, 10);
        journalRepository.add(JournalFixture.journal(journalDate, utc(journalDate, 20, 0), inFuture(3600)));
        tradeLedgerPort.add(new TradeCountInfo(journalDate, 5));

        List<JournalEntryResult> results = getEntries(journalDate, journalDate);

        assertEquals(1, results.size());
        assertEquals(5, results.get(0).tradeCount());
        assertEquals(2, results.get(0).tradeNoteCount());
    }

    @Test
    void ledger에_해당_날짜_거래_기록이_없으면_tradeCount는_0이다() {
        LocalDate journalDate = LocalDate.of(2026, 7, 10);
        journalRepository.add(JournalFixture.journal(journalDate, utc(journalDate, 20, 0), inFuture(3600)));

        List<JournalEntryResult> results = getEntries(journalDate, journalDate);

        assertEquals(0, results.get(0).tradeCount());
    }

    @Test
    void 일지_대상_날짜와_같은_날_작성했으면_isBackfilled는_false다() {
        LocalDate journalDate = LocalDate.of(2026, 7, 10);
        journalRepository.add(JournalFixture.journal(journalDate, utc(journalDate, 23, 59), inFuture(3600)));

        List<JournalEntryResult> results = getEntries(journalDate, journalDate);

        assertFalse(results.get(0).isBackfilled());
    }

    @Test
    void 일지_대상_날짜보다_늦게_작성했으면_isBackfilled는_true다() {
        LocalDate journalDate = LocalDate.of(2026, 7, 10);
        journalRepository.add(JournalFixture.journal(journalDate, utc(journalDate.plusDays(3), 0, 0), inFuture(3600)));

        List<JournalEntryResult> results = getEntries(journalDate, journalDate);

        assertTrue(results.get(0).isBackfilled());
    }

    @Test
    void 현재_시각이_editableUntilAt_이전이면_isEditable은_true다() {
        LocalDate journalDate = LocalDate.of(2026, 7, 10);
        journalRepository.add(JournalFixture.journal(journalDate, utc(journalDate, 10, 0), inFuture(3600)));

        List<JournalEntryResult> results = getEntries(journalDate, journalDate);

        assertTrue(results.get(0).isEditable());
    }

    @Test
    void 현재_시각이_editableUntilAt_이후면_isEditable은_false다() {
        LocalDate journalDate = LocalDate.of(2026, 7, 10);
        journalRepository.add(JournalFixture.journal(journalDate, utc(journalDate, 10, 0), inFuture(-3600)));

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
        journalRepository.add(JournalFixture.journal(journalDate, utc(journalDate, 18, 20), inFuture(3600)));

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
        Instant tradedAt1 = utc(date, 10, 15);
        Instant tradedAt2 = utc(date, 14, 10);
        tradeLedgerPort.add(
                TradeInfoFixture.trade(501L, 101L, TradeSide.BUY, tradedAt1),
                TradeInfoFixture.trade(502L, 102L, TradeSide.SELL, tradedAt2)
        );
        marketDataPort.add(
                SecurityInfoFixture.samsungElectronics(101L),
                SecurityInfoFixture.skHynix(102L)
        );
        journalTradeNoteRepository.add(JournalTradeNoteFixture.note(
                501L, "HBM 시장의 장기 성장 가능성이 높다고 판단했다.", tradedAt1));

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
        tradeLedgerPort.add(TradeInfoFixture.trade(501L, 999L, TradeSide.BUY, utc(date, 10, 15)));
        // marketDataPort에 999L 종목 정보를 등록하지 않음

        JournalException exception = assertThrows(JournalException.class,
                () -> journalService.getDetail(new GetJournalDetailQuery(USER_ID, date)));

        assertEquals(JournalErrorCode.SECURITY_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void journalId로_본인_소유_일지를_조회하면_canCreate는_항상_false다() {
        LocalDate journalDate = LocalDate.of(2026, 7, 10);
        journalRepository.add(JournalFixture.journal(305L, USER_ID, journalDate, utc(journalDate, 18, 20), inFuture(3600)));

        JournalDetailResult result = journalService.getByJournalId(new GetJournalByIdQuery(USER_ID, 305L));

        assertFalse(result.canCreate());
        assertEquals(305L, result.journal().journalId());
        assertEquals("시장에 대한 생각", result.journal().marketThought());
    }

    @Test
    void 존재하지_않는_journalId면_JOURNAL_NOT_FOUND_예외를_던진다() {
        JournalException exception = assertThrows(JournalException.class,
                () -> journalService.getByJournalId(new GetJournalByIdQuery(USER_ID, 999L)));

        assertEquals(JournalErrorCode.JOURNAL_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 타인_소유_journalId면_JOURNAL_NOT_FOUND_예외를_던진다() {
        LocalDate journalDate = LocalDate.of(2026, 7, 10);
        Long otherUserId = 200L;
        journalRepository.add(JournalFixture.journal(305L, otherUserId, journalDate, utc(journalDate, 18, 20), inFuture(3600)));

        JournalException exception = assertThrows(JournalException.class,
                () -> journalService.getByJournalId(new GetJournalByIdQuery(USER_ID, 305L)));

        assertEquals(JournalErrorCode.JOURNAL_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 정상_저장하면_journalId와_createdAt을_반환한다() {
        LocalDate journalDate = LocalDate.of(2020, 1, 1);

        CreateJournalResult result = journalService.save(new CreateJournalCommand(
                USER_ID, journalDate, "시장에 대한 생각", MarketMood.CAUTIOUS, List.of()));

        assertNotNull(result.journalId());
        assertNotNull(result.createdAt());
    }

    @Test
    void tradeNotes와_함께_저장하면_journal_trade_note도_함께_저장된다() {
        LocalDate journalDate = LocalDate.of(2020, 1, 1);
        tradeLedgerPort.add(TradeInfoFixture.trade(501L, 101L, TradeSide.BUY, utc(journalDate, 10, 0)));

        CreateJournalResult result = journalService.save(new CreateJournalCommand(
                USER_ID, journalDate, "시장에 대한 생각", MarketMood.CAUTIOUS,
                List.of(new TradeNoteCommand(501L, "판단 근거"))));

        assertEquals(1, journalTradeNoteRepository.getSaved().size());
        assertEquals(result.journalId(), journalTradeNoteRepository.getSaved().get(0).getJournalId());
        assertEquals(501L, journalTradeNoteRepository.getSaved().get(0).getTradeId());
    }

    @Test
    void 미래_날짜면_예외를_던진다() {
        LocalDate futureDate = LocalDate.of(2099, 1, 1);

        JournalException exception = assertThrows(JournalException.class,
                () -> journalService.save(new CreateJournalCommand(USER_ID, futureDate, "생각", null, List.of())));

        assertEquals(JournalErrorCode.FUTURE_DATE_NOT_ALLOWED, exception.getErrorCode());
    }

    @Test
    void 동일_날짜에_이미_일지가_있으면_예외를_던진다() {
        LocalDate journalDate = LocalDate.of(2020, 1, 1);
        journalRepository.add(JournalFixture.journal(journalDate, utc(journalDate, 10, 0), inFuture(3600)));

        JournalException exception = assertThrows(JournalException.class,
                () -> journalService.save(new CreateJournalCommand(USER_ID, journalDate, "생각", null, List.of())));

        assertEquals(JournalErrorCode.JOURNAL_ALREADY_EXISTS, exception.getErrorCode());
    }

    @Test
    void 요청_내_tradeId가_중복되면_예외를_던진다() {
        LocalDate journalDate = LocalDate.of(2020, 1, 1);

        JournalException exception = assertThrows(JournalException.class,
                () -> journalService.save(new CreateJournalCommand(USER_ID, journalDate, "생각", null,
                        List.of(new TradeNoteCommand(501L, "근거1"), new TradeNoteCommand(501L, "근거2")))));

        assertEquals(JournalErrorCode.DUPLICATE_TRADE_ID, exception.getErrorCode());
    }

    @Test
    void tradeId가_사용자_거래_목록에_없으면_예외를_던진다() {
        LocalDate journalDate = LocalDate.of(2020, 1, 1);
        // tradeLedgerPort에 아무 거래도 등록하지 않음 — 소유권/날짜 검증 실패로 취급

        JournalException exception = assertThrows(JournalException.class,
                () -> journalService.save(new CreateJournalCommand(USER_ID, journalDate, "생각", null,
                        List.of(new TradeNoteCommand(999L, "근거")))));

        assertEquals(JournalErrorCode.TRADE_DATE_MISMATCH, exception.getErrorCode());
    }

    @Test
    void 정상_수정하면_journal_본문과_거래_근거가_생성_유지_삭제로_반영된다() {
        LocalDate journalDate = LocalDate.of(2026, 7, 10);
        journalRepository.add(JournalFixture.journal(305L, USER_ID, journalDate, utc(journalDate, 10, 0), inFuture(3600)));
        journalTradeNoteRepository.add(
                JournalTradeNoteFixture.note(501L, "기존 근거", utc(journalDate, 10, 15)),
                JournalTradeNoteFixture.note(502L, "삭제될 근거", utc(journalDate, 10, 20)));
        tradeLedgerPort.add(
                TradeInfoFixture.trade(501L, 101L, TradeSide.BUY, utc(journalDate, 10, 15)),
                TradeInfoFixture.trade(503L, 102L, TradeSide.SELL, utc(journalDate, 14, 0)));

        UpdateJournalResult result = journalService.update(new UpdateJournalCommand(
                USER_ID, 305L, "바뀐 생각", MarketMood.CONFIDENT,
                List.of(new TradeNoteCommand(501L, "수정된 근거"), new TradeNoteCommand(503L, "새 근거"))));

        assertEquals(305L, result.journalId());
        assertNotNull(result.updatedAt());

        List<JournalTradeNote> saved = journalTradeNoteRepository.getSaved();
        assertEquals(2, saved.size());
        assertTrue(saved.stream().anyMatch(note -> note.getTradeId().equals(501L) && note.getRationaleText().equals("수정된 근거")));
        assertTrue(saved.stream().anyMatch(note -> note.getTradeId().equals(503L)));
        assertFalse(saved.stream().anyMatch(note -> note.getTradeId().equals(502L)));
    }

    @Test
    void 존재하지_않는_journalId를_수정하려하면_JOURNAL_NOT_FOUND_예외를_던진다() {
        JournalException exception = assertThrows(JournalException.class,
                () -> journalService.update(new UpdateJournalCommand(USER_ID, 999L, "생각", MarketMood.CALM, List.of())));

        assertEquals(JournalErrorCode.JOURNAL_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 타인_소유_journalId를_수정하려하면_JOURNAL_NOT_FOUND_예외를_던진다() {
        LocalDate journalDate = LocalDate.of(2026, 7, 10);
        Long otherUserId = 200L;
        journalRepository.add(JournalFixture.journal(305L, otherUserId, journalDate, utc(journalDate, 10, 0), inFuture(3600)));

        JournalException exception = assertThrows(JournalException.class,
                () -> journalService.update(new UpdateJournalCommand(USER_ID, 305L, "생각", MarketMood.CALM, List.of())));

        assertEquals(JournalErrorCode.JOURNAL_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 수정_가능_시간이_지났으면_JOURNAL_NOT_EDITABLE_예외를_던진다() {
        LocalDate journalDate = LocalDate.of(2026, 7, 10);
        journalRepository.add(JournalFixture.journal(305L, USER_ID, journalDate, utc(journalDate, 10, 0), inFuture(-3600)));

        JournalException exception = assertThrows(JournalException.class,
                () -> journalService.update(new UpdateJournalCommand(USER_ID, 305L, "생각", MarketMood.CALM, List.of())));

        assertEquals(JournalErrorCode.JOURNAL_NOT_EDITABLE, exception.getErrorCode());
    }

    @Test
    void 수정_요청_내_tradeId가_중복되면_예외를_던진다() {
        LocalDate journalDate = LocalDate.of(2026, 7, 10);
        journalRepository.add(JournalFixture.journal(305L, USER_ID, journalDate, utc(journalDate, 10, 0), inFuture(3600)));

        JournalException exception = assertThrows(JournalException.class,
                () -> journalService.update(new UpdateJournalCommand(USER_ID, 305L, "생각", MarketMood.CALM,
                        List.of(new TradeNoteCommand(501L, "근거1"), new TradeNoteCommand(501L, "근거2")))));

        assertEquals(JournalErrorCode.DUPLICATE_TRADE_ID, exception.getErrorCode());
    }

    @Test
    void 수정_요청의_tradeId가_사용자_거래_목록에_없으면_예외를_던진다() {
        LocalDate journalDate = LocalDate.of(2026, 7, 10);
        journalRepository.add(JournalFixture.journal(305L, USER_ID, journalDate, utc(journalDate, 10, 0), inFuture(3600)));
        // tradeLedgerPort에 아무 거래도 등록하지 않음 — 소유권/날짜 검증 실패로 취급

        JournalException exception = assertThrows(JournalException.class,
                () -> journalService.update(new UpdateJournalCommand(USER_ID, 305L, "생각", MarketMood.CALM,
                        List.of(new TradeNoteCommand(999L, "근거")))));

        assertEquals(JournalErrorCode.TRADE_DATE_MISMATCH, exception.getErrorCode());
    }

    @Test
    void 종목별_거래_타임라인을_조회하면_종목정보와_근거를_포함해_반환한다() {
        LocalDate journalDate = LocalDate.of(2026, 7, 10);
        Instant tradedAt1 = utc(journalDate, 10, 15);
        Instant tradedAt2 = utc(journalDate, 14, 10);
        marketDataPort.add(SecurityInfoFixture.samsungElectronics(101L));
        tradeLedgerPort.add(101L,
                TradeTimelineInfoFixture.trade(501L, TradeSide.BUY, tradedAt1),
                TradeTimelineInfoFixture.trade(502L, TradeSide.SELL, tradedAt2));
        journalRepository.add(JournalFixture.journal(305L, USER_ID, journalDate, utc(journalDate, 9, 0), inFuture(3600)));
        journalTradeNoteRepository.add(JournalTradeNoteFixture.note(501L, "HBM 시장의 장기 성장 가능성이 높다고 판단했다.", tradedAt1));

        TradeTimelineResult result = journalService.getTradeTimeline(
                new GetTradeTimelineQuery(USER_ID, 101L, null, null, 0, 20));

        assertEquals("005930", result.security().securityCode());
        assertEquals("삼성전자", result.security().securityName());
        assertEquals(2, result.trades().size());
        assertEquals(2, result.totalElements());
        assertEquals(1, result.totalPages());

        var withNote = result.trades().stream().filter(t -> t.tradeId().equals(501L)).findFirst().orElseThrow();
        assertEquals(305L, withNote.note().journalId());
        assertEquals(journalDate, withNote.note().journalDate());
        assertEquals("HBM 시장의 장기 성장 가능성이 높다고 판단했다.", withNote.note().rationaleText());

        var withoutNote = result.trades().stream().filter(t -> t.tradeId().equals(502L)).findFirst().orElseThrow();
        assertNull(withoutNote.note());
    }

    @Test
    void 존재하지_않는_종목을_조회하면_SECURITY_NOT_FOUND_예외를_던진다() {
        JournalException exception = assertThrows(JournalException.class,
                () -> journalService.getTradeTimeline(new GetTradeTimelineQuery(USER_ID, 999L, null, null, 0, 20)));

        assertEquals(JournalErrorCode.SECURITY_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void startDate가_endDate보다_늦으면_예외를_던진다_타임라인() {
        JournalException exception = assertThrows(JournalException.class,
                () -> journalService.getTradeTimeline(new GetTradeTimelineQuery(
                        USER_ID, 101L, LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 10), 0, 20)));

        assertEquals(JournalErrorCode.INVALID_DATE_RANGE, exception.getErrorCode());
    }

    @Test
    void page가_음수이면_예외를_던진다() {
        JournalException exception = assertThrows(JournalException.class,
                () -> journalService.getTradeTimeline(new GetTradeTimelineQuery(USER_ID, 101L, null, null, -1, 20)));

        assertEquals(JournalErrorCode.INVALID_PAGE_PARAMS, exception.getErrorCode());
    }

    @Test
    void size가_1보다_작으면_예외를_던진다() {
        JournalException exception = assertThrows(JournalException.class,
                () -> journalService.getTradeTimeline(new GetTradeTimelineQuery(USER_ID, 101L, null, null, 0, 0)));

        assertEquals(JournalErrorCode.INVALID_PAGE_PARAMS, exception.getErrorCode());
    }

    @Test
    void totalElements와_size로_totalPages를_올림_계산한다() {
        LocalDate journalDate = LocalDate.of(2026, 7, 10);
        marketDataPort.add(SecurityInfoFixture.samsungElectronics(101L));
        tradeLedgerPort.add(101L,
                TradeTimelineInfoFixture.trade(501L, TradeSide.BUY, utc(journalDate, 9, 0)),
                TradeTimelineInfoFixture.trade(502L, TradeSide.BUY, utc(journalDate, 10, 0)),
                TradeTimelineInfoFixture.trade(503L, TradeSide.BUY, utc(journalDate, 11, 0)));

        TradeTimelineResult result = journalService.getTradeTimeline(
                new GetTradeTimelineQuery(USER_ID, 101L, null, null, 0, 2));

        assertEquals(2, result.trades().size());
        assertEquals(3, result.totalElements());
        assertEquals(2, result.totalPages());
    }

    private List<JournalEntryResult> getEntries(LocalDate startDate, LocalDate endDate) {
        return journalService.getEntries(new GetJournalEntriesQuery(USER_ID, startDate, endDate));
    }

    private static Instant utc(LocalDate date, int hour, int minute) {
        return date.atTime(hour, minute).toInstant(ZoneOffset.UTC);
    }

    private static Instant inFuture(long offsetSeconds) {
        return Instant.now().plusSeconds(offsetSeconds);
    }
}
