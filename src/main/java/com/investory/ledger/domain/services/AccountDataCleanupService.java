package com.investory.ledger.domain.services;

import com.investory.ledger.domain.ports.JournalNotePort;
import com.investory.ledger.domain.repositories.HoldingSnapshotRepository;
import com.investory.ledger.domain.repositories.TradeMatchRepository;
import com.investory.ledger.domain.repositories.TradeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 증권사 연동 해지 시 broker.AccountDataCleanupPort를 통해 호출된다. 계좌 하나가 소유한 ledger 데이터를
// FK 참조 순서(journal_trade_notes/trade_matches → trades → holding_snapshots)에 맞춰 전부 지운다.
// 기본 propagation(REQUIRED)이라 호출자(BrokerConnectionService.disconnectConnection)의 트랜잭션에
// 합류한다 — 계좌 삭제 전체가 하나의 물리 트랜잭션으로 원자적으로 처리된다.
@Service
public class AccountDataCleanupService {

    private final TradeRepository tradeRepository;
    private final TradeMatchRepository tradeMatchRepository;
    private final HoldingSnapshotRepository holdingSnapshotRepository;
    private final JournalNotePort journalNotePort;

    public AccountDataCleanupService(TradeRepository tradeRepository,
                                      TradeMatchRepository tradeMatchRepository,
                                      HoldingSnapshotRepository holdingSnapshotRepository,
                                      JournalNotePort journalNotePort) {
        this.tradeRepository = tradeRepository;
        this.tradeMatchRepository = tradeMatchRepository;
        this.holdingSnapshotRepository = holdingSnapshotRepository;
        this.journalNotePort = journalNotePort;
    }

    @Transactional
    public void deleteAccountData(Long accountId) {
        List<Long> tradeIds = tradeRepository.findTradeIdsByAccountId(accountId);

        journalNotePort.deleteNotesByTradeIds(tradeIds);
        tradeMatchRepository.deleteByAccountId(accountId);
        tradeRepository.deleteByAccountId(accountId);
        holdingSnapshotRepository.deleteByAccountId(accountId);
    }
}
