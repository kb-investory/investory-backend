package com.investory.tendency.domain.ports;

import java.time.LocalDate;

public class FakeJournalRationalePort implements JournalRationalePort {

    private int count = 0;

    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public int countJournalsInRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return count;
    }
}
