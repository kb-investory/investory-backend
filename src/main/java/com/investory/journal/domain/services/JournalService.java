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
import com.investory.journal.domain.services.dto.query.GetJournalDetailQuery;
import com.investory.journal.domain.services.dto.query.GetJournalEntriesQuery;
import com.investory.journal.domain.services.dto.result.CreateJournalResult;
import com.investory.journal.domain.services.dto.result.JournalDetailResult;
import com.investory.journal.domain.services.dto.result.JournalEntryResult;
import com.investory.journal.domain.services.dto.result.JournalInfoResult;
import com.investory.journal.domain.services.dto.result.TradeDetailResult;
import com.investory.journal.domain.services.dto.result.TradeNoteResult;
import org.springframework.stereotype.Service;

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
        JournalInfoResult journalInfo = journal.map(j -> JournalInfoResult.from(j, now)).orElse(null);

        return new JournalDetailResult(query.date(), canCreate, journalInfo, tradeResults);
    }

    // TODO: 코드베이스 전체에 @EnableTransactionManagement가 켜져 있지 않아(global/database/DatabaseConfig.java,
    // 공유 영역이라 이번 작업 범위에서 손대지 않음) @Transactional을 붙여도 무시된다. 그래서 journal 저장과
    // journal_trade_notes 저장이 원자적이지 않다 — 아래에서 두 저장 이전에 모든 비즈니스 검증을 끝내두어
    // (미래 날짜, 중복 일지, 요청 내 중복 tradeId, tradeId 소유권/날짜 불일치) 실제로 원자성이 깨질 수 있는
    // 경우를 "검증을 통과한 데이터인데 두 번째 INSERT 시점에 DB 연결이 끊기는" 것 같은 순수 인프라 장애로
    // 최대한 좁혀뒀다. @EnableTransactionManagement가 추가되면 이 메서드 전체를 @Transactional로 감쌀 것.
    public CreateJournalResult save(CreateJournalCommand command) {
        if (command.journalDate().isAfter(LocalDate.ofInstant(Instant.now(), ZoneOffset.UTC))) {
            throw new JournalException(JournalErrorCode.FUTURE_DATE_NOT_ALLOWED);
        }
        if (journalRepository.findByUserAndDate(command.userId(), command.journalDate()).isPresent()) {
            throw new JournalException(JournalErrorCode.JOURNAL_ALREADY_EXISTS);
        }

        List<TradeNoteCommand> tradeNotes = command.tradeNotes() == null ? List.of() : command.tradeNotes();
        validateNoDuplicateTradeIds(tradeNotes);
        if (!tradeNotes.isEmpty()) {
            validateTradesBelongToUserAndDate(command.userId(), command.journalDate(), tradeNotes);
        }

        Journal journal = Journal.create(command.userId(), command.journalDate(), command.marketThought(), command.marketMood());
        Journal saved = journalRepository.save(journal);

        if (!tradeNotes.isEmpty()) {
            List<JournalTradeNote> notes = tradeNotes.stream()
                    .map(tradeNote -> JournalTradeNote.create(saved.getJournalId(), tradeNote.tradeId(), tradeNote.rationaleText()))
                    .collect(Collectors.toList());
            journalTradeNoteRepository.saveAll(notes);
        }

        return new CreateJournalResult(saved.getJournalId(), saved.getCreatedAt());
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
