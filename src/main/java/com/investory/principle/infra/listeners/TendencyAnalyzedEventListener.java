package com.investory.principle.infra.listeners;

import com.investory.principle.domain.services.PrincipleService;
import com.investory.principle.domain.services.dto.command.RefreshRecommendationsCommand;
import com.investory.tendency.domain.events.TendencyAnalyzedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;

// tendency.domain.events를 참조하는 유일한 지점 — 받는 즉시 principle 자신의 Command로 변환해
// domain/services에 넘긴다(§5 변환 규칙). PrincipleService는 TendencyAnalyzedEvent의 존재를 모른다.
//
// 실행기에 직접 제출: Spring 이벤트 발행은 기본이 동기라, 이 리스너를 그대로 두면 POST
// /tendency/analyses가 이 리스너의 LLM 호출(추천 생성)이 끝날 때까지 응답을 못 내보낸다 — 분석
// 자체는 이미 커밋됐는데도. 추천은 분석 응답 바디에 안 실리고 GET /principle/recommendations로
// 별도 조회되므로, 비동기로 떼어내도 잃는 기능이 없다.
//
// 예전엔 @Async("principleRecommendationExecutor")로 선언적으로 떼어냈는데, 1000 VU 부하테스트에서
// 이 executor 큐가 가득 차자 "제출 자체"가 거부되는 TaskRejectedException이 handle() 본문(과 그
// 안의 try/catch)을 거치지 않고 Spring AOP 프록시 단계에서 곧장 던져졌다. 이게 동기 발행 경로인
// publishEvent() -> AnalysisRunService.runAnalysis()까지 그대로 전파돼, 이미 저장까지 끝난 분석
// 결과를 클라이언트에게 500으로 되돌려주는 결과를 냈다(에러 151/1000건이 전부 이 경로). 그래서
// handle() 자체는 동기(@EventListener만)로 두고, 실행기 제출을 여기서 직접 try/catch로 감싼다 —
// journalLabelingExecutor/brokerSyncExecutor/tendencyLlmExecutor 호출부와 같은 패턴.
//
// 빈 이름을 명시한다 — notification도 같은 이벤트를 구독하는 동명(TendencyAnalyzedEventListener) 클래스를
// 갖고 있어(xxxEventListener 명명 규칙상 자연스러운 충돌), 둘 다 default bean name을 쓰면
// ConflictingBeanDefinitionException으로 컨텍스트 초기화가 실패한다.
@Component("principleTendencyAnalyzedEventListener")
public class TendencyAnalyzedEventListener {

    private static final Logger log = LoggerFactory.getLogger(TendencyAnalyzedEventListener.class);

    private final PrincipleService principleService;
    private final Executor principleRecommendationExecutor;

    public TendencyAnalyzedEventListener(PrincipleService principleService,
                                          @Qualifier("principleRecommendationExecutor") Executor principleRecommendationExecutor) {
        this.principleService = principleService;
        this.principleRecommendationExecutor = principleRecommendationExecutor;
    }

    @EventListener
    public void handle(TendencyAnalyzedEvent event) {
        try {
            CompletableFuture.runAsync(() -> refreshRecommendations(event), principleRecommendationExecutor);
        } catch (RejectedExecutionException e) {
            log.warn("추천 갱신 작업 제출 실패(큐 포화) — 이번 분석 실행의 추천 갱신은 건너뜁니다. analysisRunId={}",
                    event.analysisRunId(), e);
        }
    }

    // 실행(run) 1건에 속한 항목별 결과 전체를 한 번에 넘긴다 — refreshRecommendationsForRun이 항목별
    // 생성을 병렬로 돌리고 한 번에 저장하므로, 같은 실행의 추천들이 항상 같은 시점에 한꺼번에
    // 나타난다(항목마다 따로 호출하면 늦게 끝난 항목의 추천만 뒤늦게 나타나는 시간차가 생겼었다).
    private void refreshRecommendations(TendencyAnalyzedEvent event) {
        List<RefreshRecommendationsCommand> commands = event.results().stream()
                .map(result -> new RefreshRecommendationsCommand(result.analysisResultId(), result.analysisTypeCode(), result.analysisTypeName()))
                .collect(Collectors.toList());
        // refreshRecommendationsForRun 내부에서 REQUESTED/SUCCESS/FAILED 상태를 스스로 기록하므로
        // 여기서는 이벤트 매핑 자체가 깨지는 것 같은 그 이전 단계의 예외만 최후 방어선으로 로깅한다.
        try {
            principleService.refreshRecommendationsForRun(event.userId(), event.analysisRunId(), commands);
        } catch (RuntimeException e) {
            log.error("추천 갱신 실패. analysisRunId={}", event.analysisRunId(), e);
        }
    }
}
