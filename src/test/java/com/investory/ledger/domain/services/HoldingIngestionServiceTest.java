package com.investory.ledger.domain.services;

import com.investory.ledger.domain.model.Holding;
import com.investory.ledger.domain.ports.FakeMarketDataPort;
import com.investory.ledger.domain.ports.dto.SecurityInfo;
import com.investory.ledger.domain.repositories.FakeHoldingSnapshotRepository;
import com.investory.ledger.domain.services.dto.command.IngestRawHoldingsCommand;
import com.investory.ledger.domain.services.dto.command.RawHoldingRecord;
import com.investory.ledger.domain.services.dto.result.IngestResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HoldingIngestionServiceTest {

    private static final Long USER_ID = 100L;
    private static final Long ACCOUNT_ID = 11L;
    private static final Long SECURITY_ID = 101L;
    private static final LocalDate BASE_DATE = LocalDate.of(2026, 7, 29);

    private FakeHoldingSnapshotRepository holdingSnapshotRepository;
    private FakeMarketDataPort marketDataPort;
    private HoldingIngestionService holdingIngestionService;

    @BeforeEach
    void setUp() {
        holdingSnapshotRepository = new FakeHoldingSnapshotRepository();
        marketDataPort = new FakeMarketDataPort();
        holdingIngestionService = new HoldingIngestionService(holdingSnapshotRepository, marketDataPort);

        marketDataPort.add(new SecurityInfo(SECURITY_ID, "005930", "삼성전자", "KOSPI", "반도체"));
    }

    @Test
    void broker가_계산한_보유현황을_그대로_저장한다() {
        RawHoldingRecord raw = new RawHoldingRecord("005930", BigDecimal.TEN, BigDecimal.valueOf(70000), BigDecimal.valueOf(75000));

        IngestResult result = holdingIngestionService.ingestHoldings(
                new IngestRawHoldingsCommand(USER_ID, ACCOUNT_ID, BASE_DATE, List.of(raw)));

        assertEquals(1, result.successCount());
        List<Holding> saved = holdingSnapshotRepository.findLatestByAccountIds(List.of(ACCOUNT_ID), null);
        assertEquals(1, saved.size());
        assertEquals(0, saved.get(0).getAveragePurchasePrice().compareTo(BigDecimal.valueOf(70000)));
        assertEquals(0, saved.get(0).getCurrentPrice().compareTo(BigDecimal.valueOf(75000)));
        assertEquals(BASE_DATE, saved.get(0).getSnapshotDate());
    }

    @Test
    void 알수_없는_종목코드는_건너뛴다() {
        RawHoldingRecord raw = new RawHoldingRecord("999999", BigDecimal.TEN, BigDecimal.valueOf(70000), BigDecimal.valueOf(75000));

        IngestResult result = holdingIngestionService.ingestHoldings(
                new IngestRawHoldingsCommand(USER_ID, ACCOUNT_ID, BASE_DATE, List.of(raw)));

        assertEquals(0, result.successCount());
        assertEquals(1, result.skippedCount());
        assertTrue(holdingSnapshotRepository.findLatestByAccountIds(List.of(ACCOUNT_ID), null).isEmpty());
    }

    @Test
    void 같은_기준일에_다시_적재하면_덮어쓴다() {
        RawHoldingRecord first = new RawHoldingRecord("005930", BigDecimal.TEN, BigDecimal.valueOf(70000), BigDecimal.valueOf(75000));
        RawHoldingRecord updated = new RawHoldingRecord("005930", BigDecimal.valueOf(20), BigDecimal.valueOf(72000), BigDecimal.valueOf(75000));

        holdingIngestionService.ingestHoldings(new IngestRawHoldingsCommand(USER_ID, ACCOUNT_ID, BASE_DATE, List.of(first)));
        holdingIngestionService.ingestHoldings(new IngestRawHoldingsCommand(USER_ID, ACCOUNT_ID, BASE_DATE, List.of(updated)));

        List<Holding> saved = holdingSnapshotRepository.findLatestByAccountIds(List.of(ACCOUNT_ID), null);
        assertEquals(1, saved.size());
        assertEquals(0, saved.get(0).getQuantity().compareTo(BigDecimal.valueOf(20)));
    }
}
