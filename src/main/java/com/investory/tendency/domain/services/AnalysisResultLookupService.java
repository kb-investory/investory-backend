package com.investory.tendency.domain.services;

import com.investory.tendency.domain.model.AnalysisResultDetail;
import com.investory.tendency.domain.repositories.AnalysisResultRepository;
import com.investory.tendency.domain.repositories.AnalysisRunRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

// principle.infra.port_impls.TendencyAnalysisPortImpl(cross-domain 호출 지점)이 직접 호출하는 진입점.
// AnalysisRunService와 별도 서비스로 분리한 이유: AnalysisRunService는 PrincipleAdherenceAnalysisService(6번)에
// 의존하는데, 그 서비스는 PrinciplePort(→principle.PrincipleService)에 의존한다. 그 경로가 다시 이 조회로
// 되돌아오면(TendencyAnalysisPortImpl→AnalysisRunService→...→PrinciplePortImpl→PrincipleService) 스프링 빈
// 생성 시 순환 참조가 생긴다. 이 서비스는 리포지토리만 의존해서 그 순환을 끊는다
// (broker.AccountLookupService/market.StockLookupService와 동일한 이유·패턴).
@Service
public class AnalysisResultLookupService {

    private final AnalysisRunRepository analysisRunRepository;
    private final AnalysisResultRepository analysisResultRepository;

    public AnalysisResultLookupService(AnalysisRunRepository analysisRunRepository,
                                        AnalysisResultRepository analysisResultRepository) {
        this.analysisRunRepository = analysisRunRepository;
        this.analysisResultRepository = analysisResultRepository;
    }

    // 유저의 가장 최근 실행(run)에 속한 항목별 결과. 실행 이력이 없으면 빈 리스트.
    public List<AnalysisResultDetail> findLatestResultDetails(Long userId) {
        return analysisRunRepository.findByUserId(userId).stream()
                .findFirst()   // findByUserId가 이미 created_at DESC로 정렬해서 반환
                .map(run -> analysisResultRepository.findDetailByAnalysisRunId(run.getAnalysisRunId()))
                .orElse(List.of());
    }

    public Optional<AnalysisResultDetail> findResultDetail(Long analysisResultId) {
        return analysisResultRepository.findDetailById(analysisResultId);
    }
}
