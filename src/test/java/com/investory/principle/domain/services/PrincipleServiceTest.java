package com.investory.principle.domain.services;

import com.investory.principle.domain.constant.PrincipleOriginType;
import com.investory.principle.domain.constant.RecommendationStatus;
import com.investory.principle.domain.exception.PrincipleErrorCode;
import com.investory.principle.domain.exception.PrincipleException;
import com.investory.principle.domain.model.PrincipleRecommendation;
import com.investory.principle.domain.model.PrincipleSet;
import com.investory.principle.domain.model.PrincipleSetItem;
import com.investory.principle.domain.ports.FakeTendencyAnalysisPort;
import com.investory.principle.domain.ports.dto.TendencyAnalysisInfo;
import com.investory.principle.domain.repositories.FakePrincipleRecommendationRepository;
import com.investory.principle.domain.repositories.FakePrincipleSetRepository;
import com.investory.principle.domain.services.dto.command.PrincipleItemCommand;
import com.investory.principle.domain.services.dto.command.RefreshRecommendationsCommand;
import com.investory.principle.domain.services.dto.command.SavePrincipleSetCommand;
import com.investory.principle.domain.services.dto.query.GetActivePrincipleSetQuery;
import com.investory.principle.domain.services.dto.query.GetRecommendationsQuery;
import com.investory.principle.domain.services.dto.result.PrincipleItemResult;
import com.investory.principle.domain.services.dto.result.PrincipleSetResult;
import com.investory.principle.domain.services.dto.result.RecommendationListResult;
import com.investory.principle.domain.services.dto.result.SavePrincipleSetResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrincipleServiceTest {

    private static final Long USER_ID = 100L;

    private FakePrincipleSetRepository principleSetRepository;
    private FakePrincipleRecommendationRepository principleRecommendationRepository;
    private FakeTendencyAnalysisPort tendencyAnalysisPort;
    private PrincipleService principleService;

    @BeforeEach
    void setUp() {
        principleSetRepository = new FakePrincipleSetRepository();
        principleRecommendationRepository = new FakePrincipleRecommendationRepository();
        tendencyAnalysisPort = new FakeTendencyAnalysisPort();
        principleService = new PrincipleService(principleSetRepository, principleRecommendationRepository, tendencyAnalysisPort);
    }

    @Test
    void 활성_원칙이_없으면_조회시_예외가_발생한다() {
        PrincipleException e = assertThrows(PrincipleException.class,
                () -> principleService.getActivePrincipleSet(new GetActivePrincipleSetQuery(USER_ID)));
        assertEquals(PrincipleErrorCode.PRINCIPLE_SET_NOT_FOUND, e.getErrorCode());
    }

    @Test
    void 직접_추가한_원칙은_origin이_DIRECT이고_분석유형명이_없다() {
        PrincipleSetItem item = PrincipleSetItem.of(1L, null, "투자 근거를 기록한다.", null, 1);
        principleSetRepository.add(activeSet(1L, USER_ID, 1, List.of(item)));

        PrincipleSetResult result = principleService.getActivePrincipleSet(new GetActivePrincipleSetQuery(USER_ID));

        assertEquals(1, result.principles().size());
        PrincipleItemResult principle = result.principles().get(0);
        assertEquals(PrincipleOriginType.DIRECT, principle.origin().type());
        assertNull(principle.origin().analysisTypeName());
    }

    @Test
    void AI_추천으로_채택한_원칙은_origin에_분석유형명이_채워진다() {
        tendencyAnalysisPort.addLatestCompletedAnalysisResult(new TendencyAnalysisInfo(10L, 1L, "CONCENTRATED", "집중투자형"));
        principleRecommendationRepository.add(
                PrincipleRecommendation.of(5L, 10L, "한 종목의 비중은 30%를 넘지 않는다.", "집중 위험 완화", null, RecommendationStatus.ADOPTED,
                        Instant.now(), Instant.now()));
        PrincipleSetItem item = PrincipleSetItem.of(1L, 5L, "한 종목의 비중은 30%를 넘지 않는다.", null, 1);
        principleSetRepository.add(activeSet(1L, USER_ID, 1, List.of(item)));

        PrincipleSetResult result = principleService.getActivePrincipleSet(new GetActivePrincipleSetQuery(USER_ID));

        PrincipleItemResult principle = result.principles().get(0);
        assertEquals(PrincipleOriginType.AI_RECOMMENDATION, principle.origin().type());
        assertEquals("집중투자형", principle.origin().analysisTypeName());
    }

    @Test
    void 저장하면_버전이_증가하고_기존_활성_세트는_보관된다() {
        principleSetRepository.add(activeSet(1L, USER_ID, 1, List.of()));

        SavePrincipleSetResult result = principleService.savePrincipleSet(
                new SavePrincipleSetCommand(USER_ID, null, List.of(
                        new PrincipleItemCommand(null, "투자 근거를 기록한다.", null, 1))));

        assertEquals(2, result.versionNo());
        assertTrue(principleSetRepository.findActiveByUserId(USER_ID).isPresent());
        assertEquals(2, principleSetRepository.findActiveByUserId(USER_ID).get().getVersionNo());
    }

    @Test
    void 새_세트에_채택된_추천은_ADOPTED로_전환된다() {
        principleRecommendationRepository.add(suggestedRecommendation(1L));

        principleService.savePrincipleSet(new SavePrincipleSetCommand(USER_ID, null, List.of(
                new PrincipleItemCommand(1L, "한 종목의 비중은 30%를 넘지 않는다.", null, 1))));

        RecommendationStatus status = principleRecommendationRepository.findByIds(List.of(1L)).get(0).getStatus();
        assertEquals(RecommendationStatus.ADOPTED, status);
    }

    @Test
    void 이전에_채택됐던_추천이_새_세트에서_빠지면_다시_SUGGESTED로_되돌아간다() {
        PrincipleRecommendation adopted = PrincipleRecommendation.of(1L, 10L, "text", "reason", null, RecommendationStatus.ADOPTED,
                Instant.now(), Instant.now());
        principleRecommendationRepository.add(adopted);
        PrincipleSetItem previousItem = PrincipleSetItem.of(1L, 1L, "text", null, 1);
        principleSetRepository.add(activeSet(1L, USER_ID, 1, List.of(previousItem)));

        principleService.savePrincipleSet(new SavePrincipleSetCommand(USER_ID, null, List.of(
                new PrincipleItemCommand(null, "다른 원칙", null, 1))));

        RecommendationStatus status = principleRecommendationRepository.findByIds(List.of(1L)).get(0).getStatus();
        assertEquals(RecommendationStatus.SUGGESTED, status);
    }

    @Test
    void 이미_DISMISSED된_추천을_채택하려_하면_예외가_발생한다() {
        principleRecommendationRepository.add(PrincipleRecommendation.of(1L, 10L, "text", "reason", null,
                RecommendationStatus.DISMISSED, Instant.now(), Instant.now()));

        PrincipleException e = assertThrows(PrincipleException.class, () -> principleService.savePrincipleSet(
                new SavePrincipleSetCommand(USER_ID, null, List.of(new PrincipleItemCommand(1L, "text", null, 1)))));
        assertEquals(PrincipleErrorCode.PRINCIPLE_CONFLICT, e.getErrorCode());
    }

    @Test
    void 존재하지_않는_추천ID로_저장하면_예외가_발생한다() {
        PrincipleException e = assertThrows(PrincipleException.class, () -> principleService.savePrincipleSet(
                new SavePrincipleSetCommand(USER_ID, null, List.of(new PrincipleItemCommand(999L, "text", null, 1)))));
        assertEquals(PrincipleErrorCode.RECOMMENDATION_NOT_FOUND, e.getErrorCode());
    }

    @Test
    void 완료된_분석이_없으면_추천목록은_비어있다() {
        RecommendationListResult result = principleService.getRecommendations(new GetRecommendationsQuery(USER_ID));
        assertTrue(result.recommendations().isEmpty());
    }

    @Test
    void 분석은_있지만_추천이_아직_생성되지_않았으면_조회는_빈목록을_반환한다() {
        // GET은 더 이상 추천을 생성하지 않는다 — refreshRecommendations가 호출되기 전이면 항상 빈 목록이어야 한다.
        tendencyAnalysisPort.addLatestCompletedAnalysisResult(new TendencyAnalysisInfo(10L, 1L, "CONCENTRATED", "집중투자형"));

        RecommendationListResult result = principleService.getRecommendations(new GetRecommendationsQuery(USER_ID));

        assertTrue(result.recommendations().isEmpty());
        assertTrue(principleRecommendationRepository.findByAnalysisResultId(10L).isEmpty());
    }

    @Test
    void refreshRecommendations는_분석유형에_맞는_추천을_새로_생성한다() {
        principleService.refreshRecommendations(new RefreshRecommendationsCommand(10L, "CONCENTRATED"));

        List<PrincipleRecommendation> saved = principleRecommendationRepository.findByAnalysisResultId(10L);
        assertTrue(saved.size() > 0);
        assertTrue(saved.stream().allMatch(r -> r.getStatus() == RecommendationStatus.SUGGESTED));
    }

    @Test
    void refreshRecommendations로_생성된_추천을_조회에서_확인할_수_있다() {
        tendencyAnalysisPort.addLatestCompletedAnalysisResult(new TendencyAnalysisInfo(10L, 1L, "CONCENTRATED", "집중투자형"));
        principleService.refreshRecommendations(new RefreshRecommendationsCommand(10L, "CONCENTRATED"));

        RecommendationListResult result = principleService.getRecommendations(new GetRecommendationsQuery(USER_ID));

        assertTrue(result.recommendations().size() > 0);
        assertEquals("집중투자형", result.recommendations().get(0).analysisType().name());
    }

    @Test
    void refreshRecommendations는_같은_분석결과에_대해_멱등적이다() {
        principleService.refreshRecommendations(new RefreshRecommendationsCommand(10L, "CONCENTRATED"));
        int firstCount = principleRecommendationRepository.findByAnalysisResultId(10L).size();

        principleService.refreshRecommendations(new RefreshRecommendationsCommand(10L, "CONCENTRATED"));

        assertEquals(firstCount, principleRecommendationRepository.findByAnalysisResultId(10L).size());
    }

    @Test
    void 추천이_이미_있으면_조회는_그대로_반환한다() {
        tendencyAnalysisPort.addLatestCompletedAnalysisResult(new TendencyAnalysisInfo(10L, 1L, "CONCENTRATED", "집중투자형"));
        principleRecommendationRepository.add(suggestedRecommendation(10L));

        RecommendationListResult result = principleService.getRecommendations(new GetRecommendationsQuery(USER_ID));

        assertEquals(1, result.recommendations().size());
    }

    @Test
    void ADOPTED된_추천은_추천목록에_노출되지_않는다() {
        tendencyAnalysisPort.addLatestCompletedAnalysisResult(new TendencyAnalysisInfo(10L, 1L, "CONCENTRATED", "집중투자형"));
        principleRecommendationRepository.add(
                PrincipleRecommendation.of(1L, 10L, "text", "reason", null, RecommendationStatus.ADOPTED, Instant.now(), Instant.now()));

        RecommendationListResult result = principleService.getRecommendations(new GetRecommendationsQuery(USER_ID));

        assertTrue(result.recommendations().isEmpty());
    }

    @Test
    void 실행_1건에_속한_모든_항목의_추천을_모아서_반환한다() {
        // 하나의 분석 실행(run)에 항목(analysis type)별 결과가 여러 개 있으면, 그 각각의 추천을 전부 합쳐서 보여준다.
        tendencyAnalysisPort.addLatestCompletedAnalysisResult(new TendencyAnalysisInfo(10L, 1L, "CONCENTRATED", "집중투자형"));
        tendencyAnalysisPort.addLatestCompletedAnalysisResult(new TendencyAnalysisInfo(20L, 1L, "ADDITIONAL_BUY", "추가매수형"));
        principleRecommendationRepository.add(suggestedRecommendation(10L));
        principleRecommendationRepository.add(
                PrincipleRecommendation.of(2L, 20L, "손실 확정 후 24시간 이내 재매수하지 않는다.", "추가매수 경향 완화", null,
                        RecommendationStatus.SUGGESTED, Instant.now(), Instant.now()));

        RecommendationListResult result = principleService.getRecommendations(new GetRecommendationsQuery(USER_ID));

        assertEquals(2, result.recommendations().size());
        assertTrue(result.recommendations().stream().anyMatch(r -> r.analysisType().name().equals("집중투자형")));
        assertTrue(result.recommendations().stream().anyMatch(r -> r.analysisType().name().equals("추가매수형")));
    }

    private PrincipleRecommendation suggestedRecommendation(Long analysisResultId) {
        return PrincipleRecommendation.of(1L, analysisResultId, "한 종목의 비중은 30%를 넘지 않는다.", "집중 위험 완화", null,
                RecommendationStatus.SUGGESTED, Instant.now(), Instant.now());
    }

    private PrincipleSet activeSet(Long principleSetId, Long userId, int versionNo, List<PrincipleSetItem> items) {
        Instant now = Instant.now();
        return PrincipleSet.of(principleSetId, userId, null, versionNo,
                com.investory.principle.domain.constant.PrincipleSetStatus.ACTIVE, items, now, now);
    }
}
