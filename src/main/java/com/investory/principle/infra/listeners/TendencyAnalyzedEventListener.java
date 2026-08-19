package com.investory.principle.infra.listeners;

import com.investory.principle.domain.services.PrincipleService;
import com.investory.principle.domain.services.dto.command.RefreshRecommendationsCommand;
import com.investory.tendency.domain.events.TendencyAnalyzedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

// tendency.domain.events를 참조하는 유일한 지점 — 받는 즉시 principle 자신의 Command로 변환해
// domain/services에 넘긴다(§5 변환 규칙). PrincipleService는 TendencyAnalyzedEvent의 존재를 모른다.
//
// @Async: Spring 이벤트 발행은 기본이 동기라, 이 리스너 그대로면 POST /tendency/analyses가 이
// 리스너의 LLM 호출(추천 생성)이 끝날 때까지 응답을 못 내보낸다 — 분석 자체는 이미 커밋됐는데도.
// 추천은 분석 응답 바디에 안 실리고 GET /principle/recommendations로 별도 조회되므로,
// 비동기로 떼어내도 잃는 기능이 없다.
@Component
public class TendencyAnalyzedEventListener {

    private static final Logger log = LoggerFactory.getLogger(TendencyAnalyzedEventListener.class);

    private final PrincipleService principleService;

    public TendencyAnalyzedEventListener(PrincipleService principleService) {
        this.principleService = principleService;
    }

    // 실행(run) 1건에 항목별 결과가 여러 개 들어있으므로, 항목마다 각각 추천을 갱신한다 —
    // 성향(analysis type)마다 추천이 생겨야 하므로 특정 항목만 골라내지 않는다.
    // 항목 하나에서 예상 못한 예외가 나도(추천 생성 자체의 LLM 실패는 refreshRecommendations가 이미
    // 내부에서 처리) 나머지 항목 갱신은 계속 진행하도록 여기서 잡아 로그만 남긴다 — 비동기라 여기서
    // 안 잡으면 호출자에게 전파되지도 않고 그냥 남은 항목들 처리가 중단된다.
    @Async("principleRecommendationExecutor")
    @EventListener
    public void handle(TendencyAnalyzedEvent event) {
        for (TendencyAnalyzedEvent.AnalysisResult result : event.results()) {
            try {
                principleService.refreshRecommendations(
                        new RefreshRecommendationsCommand(result.analysisResultId(), result.analysisTypeCode(), result.analysisTypeName()));
            } catch (RuntimeException e) {
                log.error("추천 갱신 실패. analysisResultId={}", result.analysisResultId(), e);
            }
        }
    }
}
