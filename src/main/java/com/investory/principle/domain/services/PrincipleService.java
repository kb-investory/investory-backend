package com.investory.principle.domain.services;

import com.investory.principle.domain.constant.PrincipleOriginType;
import com.investory.principle.domain.constant.RecommendationStatus;
import com.investory.principle.domain.exception.PrincipleErrorCode;
import com.investory.principle.domain.exception.PrincipleException;
import com.investory.principle.domain.model.PrincipleRecommendation;
import com.investory.principle.domain.model.PrincipleSet;
import com.investory.principle.domain.model.PrincipleSetItem;
import com.investory.principle.domain.ports.TendencyAnalysisPort;
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
import com.investory.principle.domain.services.dto.result.PrincipleSetResult;
import com.investory.principle.domain.services.dto.result.RecommendationListResult;
import com.investory.principle.domain.services.dto.result.RecommendationResult;
import com.investory.principle.domain.services.dto.result.SavePrincipleSetResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PrincipleService {

    private final PrincipleSetRepository principleSetRepository;
    private final PrincipleRecommendationRepository principleRecommendationRepository;
    private final TendencyAnalysisPort tendencyAnalysisPort;

    public PrincipleService(PrincipleSetRepository principleSetRepository,
                             PrincipleRecommendationRepository principleRecommendationRepository,
                             TendencyAnalysisPort tendencyAnalysisPort) {
        this.principleSetRepository = principleSetRepository;
        this.principleRecommendationRepository = principleRecommendationRepository;
        this.tendencyAnalysisPort = tendencyAnalysisPort;
    }

    public PrincipleSetResult getActivePrincipleSet(GetActivePrincipleSetQuery query) {
        PrincipleSet principleSet = principleSetRepository.findActiveByUserId(query.userId())
                .orElseThrow(() -> new PrincipleException(PrincipleErrorCode.PRINCIPLE_SET_NOT_FOUND));
        return toPrincipleSetResult(principleSet);
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
            return new PrincipleItemResult(item.getPrincipleSetItemId(), item.getPrincipleText(), origin, item.getSortOrder());
        }

        PrincipleRecommendation recommendation = recommendationsById.get(item.getPrincipleRecommendationId());
        String analysisTypeName = recommendation == null ? null
                : tendencyAnalysisPort.findAnalysisType(recommendation.getAnalysisResultId())
                        .map(TendencyAnalysisInfo::analysisTypeName)
                        .orElse(null);
        PrincipleOriginResult origin = new PrincipleOriginResult(PrincipleOriginType.AI_RECOMMENDATION, analysisTypeName);
        return new PrincipleItemResult(item.getPrincipleSetItemId(), item.getPrincipleText(), origin, item.getSortOrder());
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

    // tendency가 성향 분석을 완료(요청)한 시점에 그 분석에 대응하는 추천 후보를 새로 생성해 "갈아끼운다".
    // 현재는 tendency 쪽에 분석 영속성/이벤트 발행이 없어 이 메서드를 호출할 리스너가 아직 없다 —
    // tendency가 analysis_runs/analysis_results를 영속화하고 완료 이벤트를 발행하기 시작하면,
    // principle/infra/listeners에 그 이벤트를 구독해 이 메서드를 호출하는 리스너를 추가한다.
    // 같은 analysisResultId로 이미 생성된 적이 있으면 멱등적으로 아무 것도 하지 않는다.
    @Transactional
    public void refreshRecommendations(RefreshRecommendationsCommand command) {
        List<PrincipleRecommendation> existing = principleRecommendationRepository.findByAnalysisResultId(command.analysisResultId());
        if (!existing.isEmpty()) {
            return;
        }
        List<PrincipleRecommendation> candidates = PrincipleRecommendationTemplates.forAnalysisTypeCode(command.analysisTypeCode()).stream()
                .map(template -> PrincipleRecommendation.create(command.analysisResultId(), template.text(), template.reason(), template.ruleJson()))
                .collect(Collectors.toList());
        principleRecommendationRepository.saveAll(candidates);
    }

    private RecommendationResult toRecommendationResult(PrincipleRecommendation recommendation, AnalysisTypeResult analysisType) {
        return new RecommendationResult(recommendation.getPrincipleRecommendationId(), recommendation.getRecommendationText(),
                recommendation.getRecommendationReason(), analysisType, recommendation.getStatus());
    }
}
