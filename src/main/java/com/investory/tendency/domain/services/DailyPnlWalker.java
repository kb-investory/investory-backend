package com.investory.tendency.domain.services;

import com.investory.tendency.domain.ports.dto.TradeInfo;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 첫 매수일부터 오늘까지 PositionDailyWalk로 전체 거래를 누적하되, "장전 보유량 > 0"이고
// 분석창(windowStart~today) 안이며 그날 손익 판정이 가능한(시세 또는 완전매각 체결가가 있는) 날만
// DailyOutcome으로 만들어 반환한다. 손익 부호에 따른 필터링·라벨링은 호출측(3번/4번 서비스) 책임 —
// 이 클래스는 손실/수익을 구분하지 않는다.
// 3번(손실 대응)·4번(수익 대응) 공유.
public class DailyPnlWalker {

    private DailyPnlWalker() {
    }

    public record DailyOutcome(LocalDate date, BigDecimal pnl, int netTradeSign) {
    }

    // trades는 매매일시 오름차순 정렬되어 있어야 한다(TradeLedgerPort의 계약과 동일). 비어있으면 빈 리스트 반환.
    public static List<DailyOutcome> walk(
            List<TradeInfo> trades,
            Map<LocalDate, BigDecimal> closePriceByDay,
            LocalDate windowStart,
            LocalDate today) {
        if (trades.isEmpty()) {
            return List.of();
        }

        LocalDate firstTradeDate = toDate(trades.get(0).tradedAt());
        Map<LocalDate, List<TradeInfo>> tradesByDay = trades.stream()
                .collect(Collectors.groupingBy(t -> toDate(t.tradedAt())));

        PositionDailyWalk walk = new PositionDailyWalk();
        List<DailyOutcome> outcomes = new ArrayList<>();

        for (LocalDate date = firstTradeDate; !date.isAfter(today); date = date.plusDays(1)) {
            BigDecimal preMarketQty = walk.quantity();
            PositionDailyWalk.DayResult day = walk.apply(tradesByDay.getOrDefault(date, List.of()));

            // 장 시작 시점에 보유량이 없던 날(첫 매수일 등)은 손익 판정 대상이 아니다
            if (preMarketQty.signum() <= 0) {
                continue;
            }
            // 평단 계산에는 분석창 밖의 과거 거래도 필요하지만, 손익 판정·집계는 분석창 안으로만 자른다
            if (date.isBefore(windowStart)) {
                continue;
            }

            BigDecimal pnl;
            if (day.fullyExited()) {
                BigDecimal avgExitPrice = day.exitAmount().divide(day.exitQuantity(), MathContext.DECIMAL64);
                pnl = avgExitPrice.subtract(day.avgCostForPnl());
            } else {
                BigDecimal closePrice = closePriceByDay.get(date);
                if (closePrice == null) {
                    continue; // 그날 시세가 없으면(비거래일 등) 판정 불가 — 건너뜀
                }
                pnl = closePrice.subtract(day.avgCostForPnl());
            }

            outcomes.add(new DailyOutcome(date, pnl, day.netQuantity().signum()));
        }

        return outcomes;
    }

    private static LocalDate toDate(Instant instant) {
        return instant.atZone(ZoneOffset.UTC).toLocalDate();
    }
}
