package com.investory.tendency.domain.services;

import com.investory.tendency.domain.ports.dto.TradeInfo;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

// 하루치 거래를 순서대로 반영하며 누적 보유량/평단(평균법)을 이어가는 상태 보유 워커.
// 매도는 평단을 바꾸지 않는다 — 그래서 완전매각으로 보유량이 0이 되어도 avgCost는 그대로 남겨,
// 그 매각의 손익 계산에 쓸 수 있게 한다 (다음 매수가 들어오면 그때 새로 계산됨).
// 3번(손실 대응)·4번(수익 대응)이 공유. 호출측이 날짜 오름차순으로 매일 apply()를 정확히 한 번씩 호출해야 한다.
public class PositionDailyWalk {

    private BigDecimal quantity = BigDecimal.ZERO;
    private BigDecimal costBasis = BigDecimal.ZERO;
    private BigDecimal avgCost = BigDecimal.ZERO;

    public BigDecimal quantity() {
        return quantity;
    }

    public DayResult apply(List<TradeInfo> dayTrades) {
        BigDecimal netQuantity = BigDecimal.ZERO;
        BigDecimal exitQuantity = BigDecimal.ZERO;
        BigDecimal exitAmount = BigDecimal.ZERO;

        for (TradeInfo trade : dayTrades) {
            if ("BUY".equals(trade.tradeSide())) {
                costBasis = costBasis.add(trade.quantity().multiply(trade.unitPrice()));
                quantity = quantity.add(trade.quantity());
                avgCost = costBasis.divide(quantity, MathContext.DECIMAL64);
                netQuantity = netQuantity.add(trade.quantity());
            } else {
                costBasis = costBasis.subtract(avgCost.multiply(trade.quantity()));
                quantity = quantity.subtract(trade.quantity());
                exitQuantity = exitQuantity.add(trade.quantity());
                exitAmount = exitAmount.add(trade.quantity().multiply(trade.unitPrice()));
                netQuantity = netQuantity.subtract(trade.quantity());
            }
        }

        boolean fullyExited = quantity.signum() == 0 && exitQuantity.signum() > 0;
        return new DayResult(netQuantity, fullyExited, exitQuantity, exitAmount, avgCost);
    }

    public record DayResult(
            BigDecimal netQuantity, boolean fullyExited,
            BigDecimal exitQuantity, BigDecimal exitAmount, BigDecimal avgCostForPnl) {
    }
}
