package com.investory.principle.infra.port_impls;

import com.investory.principle.domain.ports.TendencyAnalysisPort;
import com.investory.principle.domain.ports.dto.TendencyAnalysisInfo;
import com.investory.tendency.domain.model.AnalysisResultDetail;
import com.investory.tendency.domain.services.AnalysisResultLookupService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// tendency/infra/port_impls의 TradeLedgerPortImpl 등이 상대 도메인 서비스를 직접 호출하는 방식과 동일 —
// AnalysisResultLookupService(tendency, repository 2개에만 의존하는 얇은 조회 서비스)를 생성자로 주입받는다.
// AnalysisRunService를 직접 주입받지 않는 이유: AnalysisRunService는 PrincipleAdherenceAnalysisService를
// 거쳐 PrinciplePort로 다시 이 패키지(principle)를 호출하므로, 여기서 그걸 주입받으면 스프링 빈 순환
// 참조가 생긴다.
@Component
public class TendencyAnalysisPortImpl implements TendencyAnalysisPort {

    private final AnalysisResultLookupService analysisResultLookupService;

    public TendencyAnalysisPortImpl(AnalysisResultLookupService analysisResultLookupService) {
        this.analysisResultLookupService = analysisResultLookupService;
    }

    @Override
    public List<TendencyAnalysisInfo> findLatestCompletedAnalysisResults(Long userId) {
        return analysisResultLookupService.findLatestResultDetails(userId).stream()
                .map(this::toInfo)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<TendencyAnalysisInfo> findAnalysisType(Long analysisResultId) {
        return analysisResultLookupService.findResultDetail(analysisResultId).map(this::toInfo);
    }

    // analysisRunId는 AnalysisResultDetail이 들고 있지 않고 principle 쪽에서도 쓰이지 않아 null로 둔다.
    private TendencyAnalysisInfo toInfo(AnalysisResultDetail detail) {
        return new TendencyAnalysisInfo(detail.analysisResultId(), null, detail.typeCode(), detail.typeName());
    }
}
