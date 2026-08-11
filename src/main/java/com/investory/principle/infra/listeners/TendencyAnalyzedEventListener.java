package com.investory.principle.infra.listeners;

import com.investory.principle.domain.services.PrincipleService;
import com.investory.principle.domain.services.dto.command.RefreshRecommendationsCommand;
import com.investory.tendency.domain.events.TendencyAnalyzedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// tendency.domain.events를 참조하는 유일한 지점 — 받는 즉시 principle 자신의 Command로 변환해
// domain/services에 넘긴다(§5 변환 규칙). PrincipleService는 TendencyAnalyzedEvent의 존재를 모른다.
@Component
public class TendencyAnalyzedEventListener {

    private final PrincipleService principleService;

    public TendencyAnalyzedEventListener(PrincipleService principleService) {
        this.principleService = principleService;
    }

    // 실행(run) 1건에 항목별 결과가 여러 개 들어있으므로, 항목마다 각각 추천을 갱신한다 —
    // 성향(analysis type)마다 추천이 생겨야 하므로 특정 항목만 골라내지 않는다.
    @EventListener
    public void handle(TendencyAnalyzedEvent event) {
        for (TendencyAnalyzedEvent.AnalysisResult result : event.results()) {
            principleService.refreshRecommendations(
                    new RefreshRecommendationsCommand(result.analysisResultId(), result.analysisTypeCode()));
        }
    }
}
