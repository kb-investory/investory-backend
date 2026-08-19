package com.investory.ledger.domain.ports;

import java.util.List;

// journal.domain.services.JournalService.deleteNotesByTradeIds(List<Long>)로 위임 예정.
// 거래가 삭제될 때(증권사 연동 해지) 그 거래에 달린 매매 근거(journal_trade_notes)를 함께 지운다 —
// CLAUDE.md §8-1 참고. ledger는 journal_trade_notes의 존재를 모르고, "삭제할 tradeId 목록"만 안다.
public interface JournalNotePort {
    void deleteNotesByTradeIds(List<Long> tradeIds);
}
