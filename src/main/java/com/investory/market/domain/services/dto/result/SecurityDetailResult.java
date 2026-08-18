package com.investory.market.domain.services.dto.result;


import com.investory.market.domain.model.Security;
import com.investory.market.domain.model.SecurityPrice;

// GET /market/securities/{securityId} 응답을 만들기 위한 조합 결과.
// latestPrice는 아직 시세가 한 번도 저장되지 않은 종목이면 null일 수 있다.
public record SecurityDetailResult(Security security, SecurityPrice latestPrice) {
}
