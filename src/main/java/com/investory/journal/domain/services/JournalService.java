package com.investory.journal.domain.services;

import com.investory.journal.domain.exception.JournalErrorCode;
import com.investory.journal.domain.exception.JournalException;
import com.investory.journal.domain.models.Journal;
import com.investory.journal.domain.ports.TradeLedgerPort;
import com.investory.journal.domain.ports.dto.TradeCountInfo;
import com.investory.journal.domain.repositories.JournalRepository;
import com.investory.journal.domain.services.dto.query.GetJournalEntriesQuery;
import com.investory.journal.domain.services.dto.result.JournalEntryResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class JournalService {

    private final JournalRepository journalRepository;
    private final TradeLedgerPort tradeLedgerPort;

    public JournalService(JournalRepository journalRepository, TradeLedgerPort tradeLedgerPort) {
        this.journalRepository = journalRepository;
        this.tradeLedgerPort = tradeLedgerPort;
    }

    public List<JournalEntryResult> getEntries(GetJournalEntriesQuery query) {
        if (query.startDate().isAfter(query.endDate())) {
            throw new JournalException(JournalErrorCode.INVALID_DATE_RANGE);
        }

        List<Journal> journals = journalRepository.findByUserAndDateRange(query);
        Map<LocalDate, Integer> tradeCountsByDate = tradeLedgerPort
                .countTradesByDateRange(query.userId(), query.startDate(), query.endDate())
                .stream()
                .collect(Collectors.toMap(TradeCountInfo::tradeDate, TradeCountInfo::tradeCount));

        Instant now = Instant.now();
        return journals.stream()
                .map(journal -> toResult(journal, tradeCountsByDate, now))
                .collect(Collectors.toList());
    }

    private JournalEntryResult toResult(Journal journal, Map<LocalDate, Integer> tradeCountsByDate, Instant now) {
        int tradeCount = tradeCountsByDate.getOrDefault(journal.getJournalDate(), 0);
        boolean isBackfilled = LocalDate.ofInstant(journal.getCreatedAt(), ZoneOffset.UTC).isAfter(journal.getJournalDate());
        boolean isEditable = now.isBefore(journal.getEditableUntilAt());

        return new JournalEntryResult(
                journal.getJournalId(),
                journal.getJournalDate(),
                journal.getMarketMood(),
                tradeCount,
                journal.getTradeNoteCount(),
                journal.getCreatedAt(),
                journal.getEditableUntilAt(),
                isBackfilled,
                isEditable
        );
    }
}
