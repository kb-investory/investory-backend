package com.investory.ledger.domain.ports;

import java.util.ArrayList;
import java.util.List;

public class FakeJournalNotePort implements JournalNotePort {

    private final List<List<Long>> deleteCalls = new ArrayList<>();

    @Override
    public void deleteNotesByTradeIds(List<Long> tradeIds) {
        deleteCalls.add(tradeIds);
    }

    public List<List<Long>> deleteCalls() {
        return deleteCalls;
    }
}
