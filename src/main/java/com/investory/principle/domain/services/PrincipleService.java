package com.investory.principle.domain.services;

import com.investory.principle.domain.constant.PrincipleOriginType;
import com.investory.principle.domain.constant.RecommendationStatus;
import com.investory.principle.domain.exception.PrincipleErrorCode;
import com.investory.principle.domain.exception.PrincipleException;
import com.investory.principle.domain.model.PrincipleRecommendation;
import com.investory.principle.domain.model.PrincipleSet;
import com.investory.principle.domain.model.PrincipleSetItem;
import com.investory.principle.domain.ports.RecommendationGenerationPort;
import com.investory.principle.domain.ports.TendencyAnalysisPort;
import com.investory.principle.domain.ports.dto.GeneratedRecommendation;
import com.investory.principle.domain.ports.dto.TendencyAnalysisInfo;
import com.investory.principle.domain.repositories.PrincipleRecommendationRepository;
import com.investory.principle.domain.repositories.PrincipleSetRepository;
import com.investory.principle.domain.services.dto.command.PrincipleItemCommand;
import com.investory.principle.domain.services.dto.command.RefreshRecommendationsCommand;
import com.investory.principle.domain.services.dto.command.SavePrincipleSetCommand;
import com.investory.principle.domain.services.dto.query.GetActivePrincipleSetQuery;
import com.investory.principle.domain.services.dto.query.GetRecommendationsQuery;
import com.investory.principle.domain.services.dto.result.AnalysisTypeResult;
import com.investory.principle.domain.services.dto.result.PrincipleItemResult;
import com.investory.principle.domain.services.dto.result.PrincipleOriginResult;
import com.investory.principle.domain.services.dto.result.PrincipleRuleItemResult;
import com.investory.principle.domain.services.dto.result.PrincipleSetResult;
import com.investory.principle.domain.services.dto.result.RecommendationListResult;
import com.investory.principle.domain.services.dto.result.RecommendationResult;
import com.investory.principle.domain.services.dto.result.SavePrincipleSetResult;
import com.investory.principle.infra.exception.RecommendationGenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
public class PrincipleService {

    private static final Logger log = LoggerFactory.getLogger(PrincipleService.class);

    private final PrincipleSetRepository principleSetRepository;
    private final PrincipleRecommendationRepository principleRecommendationRepository;
    private final TendencyAnalysisPort tendencyAnalysisPort;
    private final RecommendationGenerationPort recommendationGenerationPort;
    private final Executor recommendationGenerationExecutor;

    public PrincipleService(PrincipleSetRepository principleSetRepository,
                             PrincipleRecommendationRepository principleRecommendationRepository,
                             TendencyAnalysisPort tendencyAnalysisPort,
                             RecommendationGenerationPort recommendationGenerationPort,
                             @Qualifier("recommendationGenerationExecutor") Executor recommendationGenerationExecutor) {
        this.principleSetRepository = principleSetRepository;
        this.principleRecommendationRepository = principleRecommendationRepository;
        this.tendencyAnalysisPort = tendencyAnalysisPort;
        this.recommendationGenerationPort = recommendationGenerationPort;
        this.recommendationGenerationExecutor = recommendationGenerationExecutor;
    }

    public PrincipleSetResult getActivePrincipleSet(GetActivePrincipleSetQuery query) {
        PrincipleSet principleSet = principleSetRepository.findActiveByUserId(query.userId())
                .orElseThrow(() -> new PrincipleException(PrincipleErrorCode.PRINCIPLE_SET_NOT_FOUND));
        return toPrincipleSetResult(principleSet);
    }

    // getActivePrincipleSet()과 달리 원칙 세트가 없어도 예외 대신 빈 리스트를 반환한다 — 6번(원칙 이행
    // 성향)의 설계상 "원칙 없음"은 정상적인 판정불가형 결과이지 오류가 아니다. ruleJson도 함께 반환한다
    // (getActivePrincipleSet/PrincipleSetResult는 REST 계약이라 그대로 두고, 이 조회는 tendency 전용).
    public List<PrincipleRuleItemResult> getActivePrincipleRules(Long userId) {
        return principleSetRepository.findActiveByUserId(userId)
                .map(PrincipleSet::getItems)
                .orElse(List.of())
                .stream()
                .map(item -> new PrincipleRuleItemResult(item.getPrincipleSetItemId(), item.getPrincipleText(), item.getRuleJson()))
                .collect(Collectors.toList());
    }

    private PrincipleSetResult toPrincipleSetResult(PrincipleSet principleSet) {
        Map<Long, PrincipleRecommendation> recommendationsById = findRecommendationsForItems(principleSet.getItems());

        List<PrincipleItemResult> items = principleSet.getItems().stream()
                .map(item -> toItemResult(item, recommendationsById))
                .collect(Collectors.toList());

        return new PrincipleSetResult(principleSet.getPrincipleSetId(), principleSet.getVersionNo(), principleSet.getStatus(), items);
    }

    private Map<Long, PrincipleRecommendation> findRecommendationsForItems(List<PrincipleSetItem> items) {
        List<Long> recommendationIds = items.stream()
                .map(PrincipleSetItem::getPrincipleRecommendationId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (recommendationIds.isEmpty()) {
            return Map.of();
        }
        return principleRecommendationRepository.findByIds(recommendationIds).stream()
                .collect(Collectors.toMap(PrincipleRecommendation::getPrincipleRecommendationId, r -> r));
    }

    // AI 추천 기반 원칙이면 원본 추천이 참조하는 분석 결과로부터 analysisTypeName을 복원하고,
    // 직접 추가한 원칙이면 origin을 DIRECT로 둔다 (principleRecommendationId 존재 여부로 판단 — §ERD 주석).
    private PrincipleItemResult toItemResult(PrincipleSetItem item, Map<Long, PrincipleRecommendation> recommendationsById) {
        if (!item.isAiRecommendation()) {
            PrincipleOriginResult origin = new PrincipleOriginResult(PrincipleOriginType.DIRECT, null);
            return new PrincipleItemResult(item.getPrincipleSetItemId(), null, item.getPrincipleText(), origin, item.getSortOrder());
        }

        PrincipleRecommendation recommendation = recommendationsById.get(item.getPrincipleRecommendationId());
        String analysisTypeName = recommendation == null ? null
                : tendencyAnalysisPort.findAnalysisType(recommendation.getAnalysisResultId())
                        .map(TendencyAnalysisInfo::analysisTypeName)
                        .orElse(null);
        PrincipleOriginResult origin = new PrincipleOriginResult(PrincipleOriginType.AI_RECOMMENDATION, analysisTypeName);
        return new PrincipleItemResult(
                item.getPrincipleSetItemId(), item.getPrincipleRecommendationId(), item.getPrincipleText(), origin, item.getSortOrder());
    }

    // 새 버전을 저장한다: 기존 ACTIVE 세트가 있으면 ARCHIVED로 전환하고, 새 세트를 ACTIVE로 만든다.
    // 추천 채택/해제에 따른 principle_recommendations 상태 전이도 같은 트랜잭션에서 함께 반영한다.
    @Transactional
    public SavePrincipleSetResult savePrincipleSet(SavePrincipleSetCommand command) {
        Optional<PrincipleSet> previousActive = principleSetRepository.findActiveByUserId(command.userId());
        int nextVersionNo = principleSetRepository.findMaxVersionNo(command.userId()) + 1;

        List<PrincipleSetItem> items = command.principles().stream()
                .map(this::toValidatedItem)
                .collect(Collectors.toList());

        if (previousActive.isPresent()) {
            principleSetRepository.archiveActive(command.userId());
        }

        PrincipleSet newPrincipleSet = PrincipleSet.create(command.userId(), command.analysisRunId(), nextVersionNo, items);
        Long principleSetId = principleSetRepository.save(newPrincipleSet);

        reconcileRecommendationStatuses(previousActive, command.principles());

        return new SavePrincipleSetResult(principleSetId, nextVersionNo, newPrincipleSet.getStatus(), "투자원칙이 저장되었습니다.");
    }

    private PrincipleSetItem toValidatedItem(PrincipleItemCommand command) {
        if (command.recommendationId() != null) {
            PrincipleRecommendation recommendation = principleRecommendationRepository.findByIds(List.of(command.recommendationId()))
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new PrincipleException(PrincipleErrorCode.RECOMMENDATION_NOT_FOUND));
            recommendation.validateAdoptable();
        }
        return PrincipleSetItem.create(command.recommendationId(), command.principleText(), command.ruleJson(), command.sortOrder());
    }

    // 새로 채택된 추천은 ADOPTED로, 이전엔 채택돼 있었지만 이번엔 빠진 추천은 다시 SUGGESTED로 되돌린다
    // (ERD 상태 전이 규칙 — 채택 해제 시 추천 목록에 다시 노출).
    private void reconcileRecommendationStatuses(Optional<PrincipleSet> previousActive, List<PrincipleItemCommand> newItems) {
        Set<Long> newRecommendationIds = newItems.stream()
                .map(PrincipleItemCommand::recommendationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (Long recommendationId : newRecommendationIds) {
            principleRecommendationRepository.updateStatus(recommendationId, RecommendationStatus.ADOPTED);
        }

        previousActive.ifPresent(principleSet -> {
            Set<Long> previouslyAdoptedIds = principleSet.getItems().stream()
                    .map(PrincipleSetItem::getPrincipleRecommendationId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            for (Long recommendationId : previouslyAdoptedIds) {
                if (!newRecommendationIds.contains(recommendationId)) {
                    principleRecommendationRepository.updateStatus(recommendationId, RecommendationStatus.SUGGESTED);
                }
            }
        });
    }

    // 순수 조회다 — 추천 생성은 더 이상 이 조회 시점에 일어나지 않는다. tendency가 성향 분석을
    // 완료(요청)하는 시점에 refreshRecommendations()가 미리 생성해둔 것을 읽기만 한다.
    // 실행(run) 1건에 항목(analysis type)별 결과가 여러 개이므로, 모든 항목의 추천을 모아서 반환한다.
    public RecommendationListResult getRecommendations(GetRecommendationsQuery query) {
        List<TendencyAnalysisInfo> analysisResults = tendencyAnalysisPort.findLatestCompletedAnalysisResults(query.userId());

        List<RecommendationResult> results = analysisResults.stream()
                .flatMap(info -> toRecommendationResults(info).stream())
                .collect(Collectors.toList());
        return new RecommendationListResult(results);
    }

    private List<RecommendationResult> toRecommendationResults(TendencyAnalysisInfo info) {
        AnalysisTypeResult analysisType = new AnalysisTypeResult(info.analysisTypeCode(), info.analysisTypeName());
        return principleRecommendationRepository.findByAnalysisResultId(info.analysisResultId()).stream()
                .filter(r -> r.getStatus() == RecommendationStatus.SUGGESTED)
                .map(r -> toRecommendationResult(r, analysisType))
                .collect(Collectors.toList());
    }

    // tendency가 성향 분석을 완료(요청)한 시점에 그 실행(run)에 속한 분석 결과 전체에 대응하는 추천
    // 후보를 LLM으로 새로 생성해 "갈아끼운다". 항목별로 순서대로 호출하면 항목 수만큼 LLM 호출이
    // 누적돼 늦게 끝난 항목의 추천이 먼저 끝난 항목보다 한참 뒤에 나타나는 문제가 있었다 — 그래서
    // 항목별 생성은 병렬로 실행하고, 결과를 전부 모은 뒤 saveAll() 한 번으로 저장해서 한 실행(run)의
    // 추천이 항상 같은 시점에 한꺼번에 나타나게 한다.
    // 이미 추천이 생성된 analysisResultId는 멱등적으로 건너뛴다. 항목 하나의 LLM 호출이 실패해도
    // (RecommendationGenerationException) 그 항목만 추천 0건으로 남기고 나머지는 그대로 진행한다 —
    // 원인과 무관한 캔 텍스트를 진짜 추천인 것처럼 저장하지 않는다. 다음에 다시 트리거되면 재시도된다.
    @Transactional
    public void refreshRecommendationsForRun(List<RefreshRecommendationsCommand> commands) {
        List<RefreshRecommendationsCommand> pending = commands.stream()
                .filter(command -> principleRecommendationRepository.findByAnalysisResultId(command.analysisResultId()).isEmpty())
                .collect(Collectors.toList());
        if (pending.isEmpty()) {
            return;
        }

        List<CompletableFuture<List<PrincipleRecommendation>>> futures = pending.stream()
                .map(command -> CompletableFuture.supplyAsync(() -> generateCandidates(command), recommendationGenerationExecutor))
                .collect(Collectors.toList());

        List<PrincipleRecommendation> allCandidates = futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        principleRecommendationRepository.saveAll(allCandidates);
    }

    private List<PrincipleRecommendation> generateCandidates(RefreshRecommendationsCommand command) {
        try {
            List<GeneratedRecommendation> generated = recommendationGenerationPort.generate(command.analysisTypeCode(), command.analysisTypeName());
            return generated.stream()
                    .map(g -> PrincipleRecommendation.create(command.analysisResultId(), g.text(), g.reason(), g.ruleJson()))
                    .collect(Collectors.toList());
        } catch (RecommendationGenerationException e) {
            log.warn("추천 생성 실패 — 이번 실행은 추천 0건으로 남깁니다. analysisResultId={}", command.analysisResultId(), e);
            return List.of();
        }
    }

    // auth.domain.ports.PrincipleCleanupPort 구현체(PrincipleCleanupPortImpl)에서만 호출된다 — 계정
    // 탈퇴 시 사용자의 투자원칙(모든 버전)을 전부 지운다. 원본 추천(principle_recommendations)은
    // analysisResultId로만 연결돼 있어 여기서 건드리지 않는다 — tendency의 분석 결과가 살아있는 한
    // 남아 있어도 무해하다.
    @Transactional
    public void deleteAllPrincipleSets(Long userId) {
        principleSetRepository.deleteByUserId(userId);
    }

    private RecommendationResult toRecommendationResult(PrincipleRecommendation recommendation, AnalysisTypeResult analysisType) {
        return new RecommendationResult(recommendation.getPrincipleRecommendationId(), recommendation.getRecommendationText(),
                recommendation.getRecommendationReason(), analysisType, recommendation.getStatus());
    }
}
