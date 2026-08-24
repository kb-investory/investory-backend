package com.investory.ledger.domain.services;

import com.investory.ledger.domain.constant.TradeSide;
import com.investory.ledger.domain.model.Trade;
import com.investory.ledger.domain.model.TradeMatch;
import com.investory.ledger.domain.repositories.TradeMatchRepository;
import com.investory.ledger.domain.repositories.TradeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

// 계좌·종목 단위로 매수-매도를 FIFO로 매칭해 trade_matches를 재계산한다.
// Mock Broker(또는 향후 실 증권사)가 매칭 결과를 주지 않으므로, ledger가 직접 계산하는
// 유일한 파생 데이터다. 과거 거래가 뒤늦게 들어와도 정확하도록 매번 전체 재계산한다
// (부분 upsert가 아니라 delete 후 rebuild).
@Service
public class TradeMatchingService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final TradeRepository tradeRepository;
    private final TradeMatchRepository tradeMatchRepository;

    public TradeMatchingService(TradeRepository tradeRepository, TradeMatchRepository tradeMatchRepository) {
        this.tradeRepository = tradeRepository;
        this.tradeMatchRepository = tradeMatchRepository;
    }

    @Transactional
    public void rematch(Long accountId, Long securityId) {
        List<Trade> tradesInOrder = tradeRepository.findAllByAccountIdAndSecurityId(accountId, securityId);
        List<TradeMatch> matches = match(accountId, securityId, tradesInOrder);

        tradeMatchRepository.deleteByAccountIdAndSecurityId(accountId, securityId);
        if (!matches.isEmpty()) {
            tradeMatchRepository.saveAll(matches);
        }
    }

    // FIFO 매칭 본체.
    // openLots는 "아직 안 팔린 매수 잔량"을 매수일 오름차순으로 들고 있는 큐(가장 오래된 매수가 맨 앞)다.
    // 매수를 만나면 새 lot을 큐 맨 뒤에 쌓고, 매도를 만나면 큐 맨 앞(=가장 오래된 매수)부터 순서대로
    // 소진시킨다 — 이게 FIFO(선입선출)의 의미다. 매도 하나가 여러 lot에 걸쳐 소진되면 그만큼
    // TradeMatch가 여러 개 생기고, 반대로 매수 하나가 여러 매도에 나눠 소진될 수도 있다
    // (lot.remainingQuantity가 0이 될 때까지 큐에 남아 다음 매도를 기다림).
    private List<TradeMatch> match(Long accountId, Long securityId, List<Trade> tradesInOrder) {
        List<TradeMatch> matches = new ArrayList<>();
        Deque<Lot> openLots = new ArrayDeque<>();

        for (Trade trade : tradesInOrder) {
            if (trade.getTradeSide() == TradeSide.BUY) {
                openLots.addLast(new Lot(trade, trade.getQuantity()));
                continue;
            }

            // 매도 수량을 큐 맨 앞 lot부터 채워나간다. 한 번의 while 반복 = lot 하나와의 매칭 하나.
            BigDecimal remaining = trade.getQuantity();
            while (remaining.signum() > 0 && !openLots.isEmpty()) {
                Lot lot = openLots.peekFirst();
                // 이번에 소진할 수량은 "이 lot에 남은 양"과 "이 매도가 아직 못 채운 양" 중 작은 쪽
                BigDecimal matchedQuantity = lot.remainingQuantity.min(remaining);

                matches.add(buildMatch(accountId, securityId, lot.trade, trade, matchedQuantity));

                lot.remainingQuantity = lot.remainingQuantity.subtract(matchedQuantity);
                remaining = remaining.subtract(matchedQuantity);
                if (lot.remainingQuantity.signum() == 0) {
                    openLots.pollFirst(); // 이 lot은 완전히 소진됐으니 큐에서 제거
                }
                // lot이 남아있으면(matchedQuantity < lot.remainingQuantity였던 경우) 큐 맨 앞에 그대로 둬서
                // 다음 while 반복(또는 다음 매도)이 이어서 소진하게 한다.
            }
            // remaining > 0인 채로 while이 끝났다는 건 openLots가 먼저 바닥났다는 뜻(보유 이력 누락 등
            // 데이터 정합성이 깨진 경우) — 이 매도의 남은 수량은 매칭 없이 그냥 버려진다.
        }

        return matches;
    }

    // 매칭 한 건(=lot 하나와 매도 하나가 matchedQuantity만큼 만난 것)의 손익을 계산한다.
    // 비용(수수료 등)은 거래 전체에 걸려있는 금액이라, 이번에 매칭된 수량 비율만큼만 나눠서
    // 반영한다 — 그래서 거래당 "1주당 비용(perUnit)"을 먼저 구한 뒤 matchedQuantity를 곱한다.
    private TradeMatch buildMatch(Long accountId, Long securityId, Trade buy, Trade sell, BigDecimal matchedQuantity) {
        BigDecimal buyCostPerUnit = perUnit(buy.getTransactionCostAmount(), buy.getQuantity());
        BigDecimal sellCostPerUnit = perUnit(sell.getTransactionCostAmount(), sell.getQuantity());

        // 실현손익 = (매도가-매수가)*수량 - 이번 매칭분에 배분된 매수/매도 비용
        BigDecimal grossPnl = sell.getUnitPrice().subtract(buy.getUnitPrice()).multiply(matchedQuantity);
        BigDecimal costs = buyCostPerUnit.add(sellCostPerUnit).multiply(matchedQuantity);
        BigDecimal realizedPnl = grossPnl.subtract(costs);

        // 수익률은 매수 원가(costBasis = 매수가*수량) 대비 실현손익의 비율
        BigDecimal costBasis = buy.getUnitPrice().multiply(matchedQuantity);
        BigDecimal returnRate = costBasis.signum() == 0
                ? BigDecimal.ZERO
                : realizedPnl.divide(costBasis, MathContext.DECIMAL64).multiply(HUNDRED);

        // 보유기간 = 이 lot을 매수한 날부터 이번에 매도된 날까지(일 단위, UTC 기준 날짜로 절삭)
        long holdingDays = ChronoUnit.DAYS.between(
                buy.getTradedAt().atZone(ZoneOffset.UTC).toLocalDate(),
                sell.getTradedAt().atZone(ZoneOffset.UTC).toLocalDate());

        return TradeMatch.of(accountId, buy.getTradeId(), sell.getTradeId(), securityId, matchedQuantity,
                buy.getUnitPrice(), sell.getUnitPrice(), realizedPnl, returnRate, holdingDays);
    }

    // 거래 1주(1단위)당 비용 — 매칭된 수량만큼만 비용을 배분하기 위한 기준값
    private BigDecimal perUnit(BigDecimal amount, BigDecimal quantity) {
        return quantity.signum() == 0 ? BigDecimal.ZERO : amount.divide(quantity, MathContext.DECIMAL64);
    }

    // "아직 다 팔리지 않은 매수 한 건"을 표현하는 내부 작업용 단위.
    // remainingQuantity는 이 매수 중 아직 매도로 소진되지 않고 남은 수량이며,
    // match() 안에서 매도가 들어올 때마다 줄어들다가 0이 되면 큐에서 빠진다.
    private static class Lot {
        private final Trade trade;
        private BigDecimal remainingQuantity;

        private Lot(Trade trade, BigDecimal remainingQuantity) {
            this.trade = trade;
            this.remainingQuantity = remainingQuantity;
        }
    }
}
