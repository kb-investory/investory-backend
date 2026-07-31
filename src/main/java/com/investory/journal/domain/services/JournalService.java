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
import com.investory.journal.domain.services.dto.command.CreateJournalCommand;
import com.investory.journal.domain.services.dto.command.TradeNoteCommand;
import com.investory.journal.domain.services.dto.query.GetJournalByIdQuery;
import com.investory.journal.domain.services.dto.query.GetJournalDetailQuery;
import com.investory.journal.domain.services.dto.query.GetJournalEntriesQuery;
import com.investory.journal.domain.services.dto.command.UpdateJournalCommand;
import com.investory.journal.domain.services.dto.result.CreateJournalResult;
import com.investory.journal.domain.services.dto.result.JournalDetailResult;
import com.investory.journal.domain.services.dto.result.JournalEntryResult;
import com.investory.journal.domain.services.dto.result.JournalInfoResult;
import com.investory.journal.domain.services.dto.result.TradeDetailResult;
import com.investory.journal.domain.services.dto.result.TradeNoteResult;
import com.investory.journal.domain.services.dto.result.UpdateJournalResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
                .map(journal -> JournalEntryResult.from(journal, tradeCountsByDate.getOrDefault(journal.getJournalDate(), 0), now))
                .collect(Collectors.toList());
    }

    public JournalDetailResult getDetail(GetJournalDetailQuery query) {
        Optional<Journal> journal = journalRepository.findByUserAndDate(query.userId(), query.date());
        return buildDetailResult(query.userId(), query.date(), journal);
    }

    public JournalDetailResult getByJournalId(GetJournalByIdQuery query) {
        Journal journal = journalRepository.findById(query.journalId())
                .filter(j -> j.getUserId().equals(query.userId()))
                .orElseThrow(() -> new JournalException(JournalErrorCode.JOURNAL_NOT_FOUND));
        return buildDetailResult(query.userId(), journal.getJournalDate(), Optional.of(journal));
    }

    private JournalDetailResult buildDetailResult(Long userId, LocalDate journalDate, Optional<Journal> journal) {
        List<TradeInfo> trades = tradeLedgerPort.findTradesOn(userId, journalDate);

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
        boolean canCreate = journal.isEmpty() && !journalDate.isAfter(LocalDate.ofInstant(now, ZoneOffset.UTC));
        JournalInfoResult journalInfo = journal.map(j -> JournalInfoResult.from(j, now)).orElse(null);

        return new JournalDetailResult(journalDate, canCreate, journalInfo, tradeResults);
    }

    // journal 저장과 journal_trade_notes 저장을 하나의 트랜잭션으로 묶는다 — 하나라도 실패하면 전체 롤백.
    @Transactional
    public CreateJournalResult save(CreateJournalCommand command) {
        if (command.journalDate().isAfter(LocalDate.ofInstant(Instant.now(), ZoneOffset.UTC))) {
            throw new JournalException(JournalErrorCode.FUTURE_DATE_NOT_ALLOWED);
        }
        if (journalRepository.findByUserAndDate(command.userId(), command.journalDate()).isPresent()) {
            throw new JournalException(JournalErrorCode.JOURNAL_ALREADY_EXISTS);
        }

        List<TradeNoteCommand> tradeNotes = validateTradeNotes(command.userId(), command.journalDate(), command.tradeNotes());

        Journal journal = Journal.create(command.userId(), command.journalDate(), command.marketThought(), command.marketMood());
        Journal saved = journalRepository.save(journal);

        saveTradeNotes(saved.getJournalId(), tradeNotes);

        return new CreateJournalResult(saved.getJournalId(), saved.getCreatedAt());
    }

    // journal 수정과 journal_trade_notes 반영(upsert+삭제)을 하나의 트랜잭션으로 묶는다.
    @Transactional
    public UpdateJournalResult update(UpdateJournalCommand command) {
        Journal journal = journalRepository.findById(command.journalId())
                .filter(j -> j.getUserId().equals(command.userId()))
                .orElseThrow(() -> new JournalException(JournalErrorCode.JOURNAL_NOT_FOUND));

        if (!journal.isEditable(Instant.now())) {
            throw new JournalException(JournalErrorCode.JOURNAL_NOT_EDITABLE);
        }

        List<TradeNoteCommand> tradeNotes = validateTradeNotes(command.userId(), journal.getJournalDate(), command.tradeNotes());

        Journal updated = journal.update(command.marketThought(), command.marketMood());
        journalRepository.update(updated);

        deleteRemovedTradeNotes(command.journalId(), tradeNotes);
        saveTradeNotes(command.journalId(), tradeNotes);

        return new UpdateJournalResult(updated.getJournalId(), updated.getUpdatedAt());
    }

    // null-safety + 중복/소유권 검증을 한 번에 묶어, save/update 양쪽에서 공유한다.
    private List<TradeNoteCommand> validateTradeNotes(Long userId, LocalDate journalDate, List<TradeNoteCommand> tradeNotes) {
        List<TradeNoteCommand> notes = tradeNotes == null ? List.of() : tradeNotes;
        validateNoDuplicateTradeIds(notes);
        if (!notes.isEmpty()) {
            validateTradesBelongToUserAndDate(userId, journalDate, notes);
        }
        return notes;
    }

    private void saveTradeNotes(Long journalId, List<TradeNoteCommand> tradeNotes) {
        if (tradeNotes.isEmpty()) {
            return;
        }
        List<JournalTradeNote> notes = tradeNotes.stream()
                .map(tradeNote -> JournalTradeNote.create(journalId, tradeNote.tradeId(), tradeNote.rationaleText()))
                .collect(Collectors.toList());
        journalTradeNoteRepository.saveAll(notes); // upsert — 있으면 갱신, 없으면 생성
    }

    // 요청에서 빠진(=삭제 대상) 기존 근거만 골라 지운다. update에서만 필요 — save는 항상 근거가 없는 상태에서 시작한다.
    private void deleteRemovedTradeNotes(Long journalId, List<TradeNoteCommand> tradeNotes) {
        List<JournalTradeNote> currentNotes = journalTradeNoteRepository.findByJournalId(journalId);
        Set<Long> requestedTradeIds = tradeNotes.stream().map(TradeNoteCommand::tradeId).collect(Collectors.toSet());
        List<Long> tradeIdsToDelete = currentNotes.stream()
                .map(JournalTradeNote::getTradeId)
                .filter(tradeId -> !requestedTradeIds.contains(tradeId))
                .collect(Collectors.toList());
        if (!tradeIdsToDelete.isEmpty()) {
            journalTradeNoteRepository.deleteByTradeIds(tradeIdsToDelete);
        }
    }

    private void validateNoDuplicateTradeIds(List<TradeNoteCommand> tradeNotes) {
        Set<Long> seen = new HashSet<>();
        for (TradeNoteCommand tradeNote : tradeNotes) {
            if (!seen.add(tradeNote.tradeId())) {
                throw new JournalException(JournalErrorCode.DUPLICATE_TRADE_ID);
            }
        }
    }

    // "로그인 사용자의 거래여야 함"과 "거래 날짜가 journalDate와 같아야 함"을 하나로 합쳐서 검증한다.
    // findTradesOn(userId, journalDate) 결과에 없는 tradeId는 남의 거래든 날짜가 다른 거래든 이 호출
    // 하나로는 구별할 수 없어서, 둘 다 TRADE_DATE_MISMATCH로 처리한다.
    private void validateTradesBelongToUserAndDate(Long userId, LocalDate journalDate, List<TradeNoteCommand> tradeNotes) {
        Set<Long> validTradeIds = tradeLedgerPort.findTradesOn(userId, journalDate).stream()
                .map(TradeInfo::tradeId)
                .collect(Collectors.toSet());
        for (TradeNoteCommand tradeNote : tradeNotes) {
            if (!validTradeIds.contains(tradeNote.tradeId())) {
                throw new JournalException(JournalErrorCode.TRADE_DATE_MISMATCH);
            }
        }
    }

    private TradeDetailResult toTradeDetailResult(TradeInfo trade,
                                                    Map<Long, SecurityInfo> securitiesBySecurityId,
                                                    Map<Long, JournalTradeNote> notesByTradeId) {
        SecurityInfo security = Optional.ofNullable(securitiesBySecurityId.get(trade.securityId()))
                .orElseThrow(() -> new JournalException(JournalErrorCode.SECURITY_NOT_FOUND));
        TradeNoteResult note = Optional.ofNullable(notesByTradeId.get(trade.tradeId()))
                .map(TradeNoteResult::from)
                .orElse(null);

        return TradeDetailResult.from(trade, security, note);
    }
}
