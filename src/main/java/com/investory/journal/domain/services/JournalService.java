package com.investory.journal.domain.services;

import com.investory.journal.domain.exception.JournalErrorCode;
import com.investory.journal.domain.exception.JournalException;
import com.investory.journal.domain.models.Journal;
import com.investory.journal.domain.models.JournalTradeNote;
import com.investory.journal.domain.ports.MarketDataPort;
import com.investory.journal.domain.ports.TradeLedgerPort;
import com.investory.journal.domain.ports.dto.SecurityInfo;
import com.investory.journal.domain.ports.dto.TradeCountInfo;
import com.investory.journal.domain.ports.dto.TradeInfo;
import com.investory.journal.domain.repositories.JournalRepository;
import com.investory.journal.domain.repositories.JournalTradeNoteRepository;
import com.investory.journal.domain.services.dto.query.GetJournalDetailQuery;
import com.investory.journal.domain.services.dto.query.GetJournalEntriesQuery;
import com.investory.journal.domain.services.dto.result.JournalDetailResult;
import com.investory.journal.domain.services.dto.result.JournalEntryResult;
import com.investory.journal.domain.services.dto.result.JournalInfoResult;
import com.investory.journal.domain.services.dto.result.TradeDetailResult;
import com.investory.journal.domain.services.dto.result.TradeNoteResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class JournalService {

    private final JournalRepository journalRepository;
    private final JournalTradeNoteRepository journalTradeNoteRepository;
    private final TradeLedgerPort tradeLedgerPort;
    private final MarketDataPort marketDataPort;

    public JournalService(JournalRepository journalRepository,
                           JournalTradeNoteRepository journalTradeNoteRepository,
                           TradeLedgerPort tradeLedgerPort,
                           MarketDataPort marketDataPort) {
        this.journalRepository = journalRepository;
        this.journalTradeNoteRepository = journalTradeNoteRepository;
        this.tradeLedgerPort = tradeLedgerPort;
        this.marketDataPort = marketDataPort;
    }

    public List<JournalEntryResult> getEntries(GetJournalEntriesQuery query) {
        if (query.startDate().isAfter(query.endDate())) {
            throw new JournalException(JournalErrorCode.INVALID_DATE_RANGE);
        }

        List<Journal> journals = journalRepository.findByUserAndDateRange(query.userId(), query.startDate(), query.endDate());
        Map<LocalDate, Integer> tradeCountsByDate = tradeLedgerPort
                .countTradesByDateRange(query.userId(), query.startDate(), query.endDate())
                .stream()
                .collect(Collectors.toMap(TradeCountInfo::tradeDate, TradeCountInfo::tradeCount));

        Instant now = Instant.now();
        return journals.stream()
                .map(journal -> toResult(journal, tradeCountsByDate, now))
                .collect(Collectors.toList());
    }

    public JournalDetailResult getDetail(GetJournalDetailQuery query) {
        Optional<Journal> journal = journalRepository.findByUserAndDate(query.userId(), query.date());
        List<TradeInfo> trades = tradeLedgerPort.findTradesOn(query.userId(), query.date());

        Map<Long, SecurityInfo> securitiesBySecurityId;
        Map<Long, JournalTradeNote> notesByTradeId;
        if (trades.isEmpty()) {
            securitiesBySecurityId = Map.of();
            notesByTradeId = Map.of();
        } else {
            List<Long> securityIds = trades.stream()
                    .map(TradeInfo::securityId)
                    .distinct()
                    .collect(Collectors.toList());
            securitiesBySecurityId = marketDataPort.findSecurities(securityIds).stream()
                    .collect(Collectors.toMap(SecurityInfo::securityId, info -> info));

            List<Long> tradeIds = trades.stream()
                    .map(TradeInfo::tradeId)
                    .collect(Collectors.toList());
            notesByTradeId = journalTradeNoteRepository.findByTradeIds(tradeIds).stream()
                    .collect(Collectors.toMap(JournalTradeNote::getTradeId, note -> note));
        }

        List<TradeDetailResult> tradeResults = trades.stream()
                .map(trade -> toTradeDetailResult(trade, securitiesBySecurityId, notesByTradeId))
                .collect(Collectors.toList());

        Instant now = Instant.now();
        boolean canCreate = journal.isEmpty() && !query.date().isAfter(LocalDate.ofInstant(now, ZoneOffset.UTC));
        JournalInfoResult journalInfo = journal.map(j -> toJournalInfoResult(j, now)).orElse(null);

        return new JournalDetailResult(query.date(), canCreate, journalInfo, tradeResults);
    }

    private TradeDetailResult toTradeDetailResult(TradeInfo trade,
                                                    Map<Long, SecurityInfo> securitiesBySecurityId,
                                                    Map<Long, JournalTradeNote> notesByTradeId) {
        SecurityInfo security = Optional.ofNullable(securitiesBySecurityId.get(trade.securityId()))
                .orElseThrow(() -> new JournalException(JournalErrorCode.SECURITY_NOT_FOUND));
        TradeNoteResult note = Optional.ofNullable(notesByTradeId.get(trade.tradeId()))
                .map(this::toTradeNoteResult)
                .orElse(null);

        return new TradeDetailResult(
                trade.tradeId(),
                trade.securityId(),
                security.securityCode(),
                security.securityName(),
                trade.tradeSide(),
                trade.quantity(),
                trade.unitPrice(),
                trade.tradedAt(),
                note
        );
    }

    private TradeNoteResult toTradeNoteResult(JournalTradeNote note) {
        return new TradeNoteResult(
                note.getJournalTradeNoteId(),
                note.getRationaleText(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }

    private JournalInfoResult toJournalInfoResult(Journal journal, Instant now) {
        boolean isBackfilled = LocalDate.ofInstant(journal.getCreatedAt(), ZoneOffset.UTC).isAfter(journal.getJournalDate());
        boolean isEditable = now.isBefore(journal.getEditableUntilAt());

        return new JournalInfoResult(
                journal.getJournalId(),
                journal.getMarketThought(),
                journal.getMarketMood(),
                journal.getCreatedAt(),
                journal.getUpdatedAt(),
                journal.getEditableUntilAt(),
                isBackfilled,
                isEditable
        );
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
