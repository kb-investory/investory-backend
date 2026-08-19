package com.investory.principle.infra.listeners;

import com.investory.principle.domain.services.PrincipleService;
import com.investory.principle.domain.services.dto.command.RefreshRecommendationsCommand;
import com.investory.tendency.domain.events.TendencyAnalyzedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

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

    // 실행(run) 1건에 속한 항목별 결과 전체를 한 번에 넘긴다 — refreshRecommendationsForRun이 항목별
    // 생성을 병렬로 돌리고 한 번에 저장하므로, 같은 실행의 추천들이 항상 같은 시점에 한꺼번에
    // 나타난다(항목마다 따로 호출하면 늦게 끝난 항목의 추천만 뒤늦게 나타나는 시간차가 생겼었다).
    // 비동기 리스너라 예외가 호출자에게 전파되지 않으므로 여기서 잡아 로그만 남긴다.
    @Async("principleRecommendationExecutor")
    @EventListener
    public void handle(TendencyAnalyzedEvent event) {
        List<RefreshRecommendationsCommand> commands = event.results().stream()
                .map(result -> new RefreshRecommendationsCommand(result.analysisResultId(), result.analysisTypeCode(), result.analysisTypeName()))
                .collect(Collectors.toList());
        try {
            principleService.refreshRecommendationsForRun(commands);
        } catch (RuntimeException e) {
            log.error("추천 갱신 실패. analysisRunId={}", event.analysisRunId(), e);
        }
    }
}
