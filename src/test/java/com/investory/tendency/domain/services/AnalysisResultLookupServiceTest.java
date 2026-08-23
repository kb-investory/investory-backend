package com.investory.tendency.domain.services;

import com.investory.tendency.domain.model.AnalysisResult;
import com.investory.tendency.domain.model.AnalysisResultDetail;
import com.investory.tendency.domain.model.AnalysisRun;
import com.investory.tendency.domain.repositories.FakeAnalysisResultRepository;
import com.investory.tendency.domain.repositories.FakeAnalysisRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisResultLookupServiceTest {

    private static final Long USER_ID = 1L;

    private FakeAnalysisRunRepository analysisRunRepository;
    private FakeAnalysisResultRepository analysisResultRepository;
    private AnalysisResultLookupService lookupService;

    @BeforeEach
    void setUp() {
        analysisRunRepository = new FakeAnalysisRunRepository();
        analysisResultRepository = new FakeAnalysisResultRepository();
        lookupService = new AnalysisResultLookupService(analysisRunRepository, analysisResultRepository);
    }

    @Test
    void 실행_이력이_없으면_최신_결과는_빈_리스트다() {
        List<AnalysisResultDetail> details = lookupService.findLatestResultDetails(USER_ID);

        assertTrue(details.isEmpty());
    }

    @Test
    void 가장_최근_실행의_결과만_반환한다() {
        AnalysisRun older = analysisRunRepository.save(AnalysisRun.create(USER_ID, LocalDate.now(), LocalDate.now(), "1.0"));
        analysisResultRepository.saveAll(List.of(AnalysisResult.create(older.getAnalysisRunId(), "PORTFOLIO_RISK_ALLOCATION", "OLD_TYPE", "{}")));

        AnalysisRun newer = analysisRunRepository.save(AnalysisRun.create(USER_ID, LocalDate.now(), LocalDate.now(), "1.0"));
        analysisResultRepository.saveAll(List.of(AnalysisResult.create(newer.getAnalysisRunId(), "PORTFOLIO_RISK_ALLOCATION", "NEW_TYPE", "{}")));

        List<AnalysisResultDetail> details = lookupService.findLatestResultDetails(USER_ID);

        assertEquals(1, details.size());
        assertEquals("NEW_TYPE", details.get(0).typeCode());
    }

    @Test
    void 단건_조회_성공() {
        AnalysisRun run = analysisRunRepository.save(AnalysisRun.create(USER_ID, LocalDate.now(), LocalDate.now(), "1.0"));
        analysisResultRepository.saveAll(List.of(AnalysisResult.create(run.getAnalysisRunId(), "PORTFOLIO_RISK_ALLOCATION", "CONCENTRATED", "{}")));
        Long analysisResultId = analysisResultRepository.findDetailByAnalysisRunId(run.getAnalysisRunId()).get(0).analysisResultId();

        Optional<AnalysisResultDetail> result = lookupService.findResultDetail(analysisResultId);

        assertTrue(result.isPresent());
        assertEquals("CONCENTRATED", result.get().typeCode());
    }

    @Test
    void 존재하지_않는_결과ID면_빈_Optional() {
        Optional<AnalysisResultDetail> result = lookupService.findResultDetail(999L);

        assertTrue(result.isEmpty());
    }
}
