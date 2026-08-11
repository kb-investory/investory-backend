package com.investory.principle.infra.port_impls;

import com.investory.principle.domain.ports.TendencyAnalysisPort;
import com.investory.principle.domain.ports.dto.TendencyAnalysisInfo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

// tendency 패키지는 아직 analysis_runs/analysis_results/analysis_types에 대한 영속성이 없어
// (domain/repositories, infra/entities, infra/mappers가 .gitkeep만 존재) 실제로 조회할 서비스가
// 없다. tendency가 분석 결과를 영속화하기 시작하면, tendency.domain.services의 조회 서비스를
// 생성자로 주입받아 이 구현체를 실제 조회 로직으로 교체한다 (tendency/infra/port_impls의
// TradeLedgerPortImpl 등이 상대 도메인 서비스를 직접 호출하는 방식과 동일).
//
// 그때까지는 고정된 실행 1건의 항목별 결과 목록을 반환하는 임시 어댑터로 동작한다. 이 fixture는
// principle_recommendations의 analysis_result_id가 참조하는 실제 tendency 쪽 레코드가 아직 없다는
// 뜻이기도 하다 — 개발 DB에 analysis_results에 대한 FK 제약이 걸려 있다면 팀 논의가 필요하다
// (plan 문서의 "확인이 필요한 사항" 참고).
@Component
public class TendencyAnalysisPortImpl implements TendencyAnalysisPort {

    private static final List<TendencyAnalysisInfo> FIXED_RESULTS = List.of(
            new TendencyAnalysisInfo(1L, 1L, "CONCENTRATED", "집중투자형"),
            new TendencyAnalysisInfo(2L, 1L, "ADDITIONAL_BUY", "추가매수형")
    );

    @Override
    public List<TendencyAnalysisInfo> findLatestCompletedAnalysisResults(Long userId) {
        return FIXED_RESULTS;
    }

    @Override
    public Optional<TendencyAnalysisInfo> findAnalysisType(Long analysisResultId) {
        return FIXED_RESULTS.stream()
                .filter(info -> info.analysisResultId().equals(analysisResultId))
                .findFirst();
    }
}
