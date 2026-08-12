package com.investory.principle.infra.port_impls;

import com.investory.principle.domain.ports.dto.TendencyAnalysisInfo;
import com.investory.tendency.domain.model.AnalysisResult;
import com.investory.tendency.domain.model.AnalysisRun;
import com.investory.tendency.domain.repositories.FakeAnalysisResultRepository;
import com.investory.tendency.domain.repositories.FakeAnalysisRunRepository;
import com.investory.tendency.domain.services.AnalysisResultLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TendencyAnalysisPortImplTest {

    private static final Long USER_ID = 1L;

    private FakeAnalysisRunRepository analysisRunRepository;
    private FakeAnalysisResultRepository analysisResultRepository;
    private TendencyAnalysisPortImpl port;

    @BeforeEach
    void setUp() {
        analysisRunRepository = new FakeAnalysisRunRepository();
        analysisResultRepository = new FakeAnalysisResultRepository();
        port = new TendencyAnalysisPortImpl(new AnalysisResultLookupService(analysisRunRepository, analysisResultRepository));
    }

    @Test
    void 최신_실행_결과를_TendencyAnalysisInfo로_매핑한다() {
        AnalysisRun run = analysisRunRepository.save(AnalysisRun.create(USER_ID, LocalDate.now(), LocalDate.now(), 1, 0, "1.0"));
        analysisResultRepository.saveAll(List.of(
                AnalysisResult.create(run.getAnalysisRunId(), "PORTFOLIO_RISK_ALLOCATION", "RISK_LOW_VOLATILITY_DIVERSIFIED", "{}")));

        List<TendencyAnalysisInfo> results = port.findLatestCompletedAnalysisResults(USER_ID);

        assertEquals(1, results.size());
        assertEquals("RISK_LOW_VOLATILITY_DIVERSIFIED", results.get(0).analysisTypeCode());
        assertEquals("RISK_LOW_VOLATILITY_DIVERSIFIED", results.get(0).analysisTypeName()); // Fake라 코드=이름
    }

    @Test
    void 실행_이력이_없으면_빈_리스트() {
        List<TendencyAnalysisInfo> results = port.findLatestCompletedAnalysisResults(USER_ID);

        assertTrue(results.isEmpty());
    }

    @Test
    void analysisResultId로_단건_조회() {
        AnalysisRun run = analysisRunRepository.save(AnalysisRun.create(USER_ID, LocalDate.now(), LocalDate.now(), 1, 0, "1.0"));
        analysisResultRepository.saveAll(List.of(AnalysisResult.create(run.getAnalysisRunId(), "PORTFOLIO_RISK_ALLOCATION", "CONCENTRATED", "{}")));
        Long analysisResultId = analysisResultRepository.findDetailByAnalysisRunId(run.getAnalysisRunId()).get(0).analysisResultId();

        Optional<TendencyAnalysisInfo> result = port.findAnalysisType(analysisResultId);

        assertTrue(result.isPresent());
        assertEquals("CONCENTRATED", result.get().analysisTypeCode());
    }

    @Test
    void 존재하지_않는_결과ID면_빈_Optional() {
        Optional<TendencyAnalysisInfo> result = port.findAnalysisType(999L);

        assertTrue(result.isEmpty());
    }
}
