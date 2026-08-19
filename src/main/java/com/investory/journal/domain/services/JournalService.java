package com.investory.journal.domain.services;

import com.investory.journal.domain.constant.RationaleLabelType;
import com.investory.journal.domain.exception.JournalErrorCode;
import com.investory.journal.domain.exception.JournalException;
import com.investory.journal.domain.models.Journal;
import com.investory.journal.domain.models.JournalTradeNote;
import com.investory.journal.domain.ports.MarketDataPort;
import com.investory.journal.domain.ports.RationaleLabelingPort;
import com.investory.journal.domain.ports.TradeLedgerPort;
import com.investory.journal.domain.ports.dto.SecurityInfo;
import com.investory.journal.domain.ports.dto.TradeCountInfo;
import com.investory.journal.domain.ports.dto.TradeInfo;
import com.investory.journal.domain.ports.dto.TradeTimelineInfo;
import com.investory.journal.domain.repositories.JournalRepository;
import com.investory.journal.domain.repositories.JournalTradeNoteRepository;
import com.investory.journal.domain.services.dto.command.CreateJournalCommand;
import com.investory.journal.domain.services.dto.command.TradeNoteCommand;
import com.investory.journal.domain.services.dto.query.GetJournalByIdQuery;
import com.investory.journal.domain.services.dto.query.GetJournalDetailQuery;
import com.investory.journal.domain.services.dto.query.GetJournalEntriesQuery;
import com.investory.journal.domain.services.dto.query.GetTradeTimelineQuery;
import com.investory.journal.domain.services.dto.command.UpdateJournalCommand;
import com.investory.journal.domain.services.dto.result.CreateJournalResult;
import com.investory.journal.domain.services.dto.result.JournalDetailResult;
import com.investory.journal.domain.services.dto.result.JournalEntryResult;
import com.investory.journal.domain.services.dto.result.JournalInfoResult;
import com.investory.journal.domain.services.dto.result.SecurityResult;
import com.investory.journal.domain.services.dto.result.TradeDetailResult;
import com.investory.journal.domain.services.dto.result.TradeNoteResult;
import com.investory.journal.domain.services.dto.result.TradeNoteWithJournalResult;
import com.investory.journal.domain.services.dto.result.TradeTimelineEntryResult;
import com.investory.journal.domain.services.dto.result.TradeTimelineResult;
import com.investory.journal.domain.services.dto.result.UpdateJournalResult;
import com.investory.journal.infra.exception.RationaleLabelingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class JournalService {

    private static final Logger log = LoggerFactory.getLogger(JournalService.class);

    // 미래 날짜 검증("오늘"이 언제인지)은 사용자 기준 시간대(KST)로 판단한다.
    private static final ZoneId JOURNAL_ZONE = ZoneId.of("Asia/Seoul");

    private final JournalRepository journalRepository;
    private final JournalTradeNoteRepository journalTradeNoteRepository;
    private final TradeLedgerPort tradeLedgerPort;
    private final MarketDataPort marketDataPort;
    private final RationaleLabelingPort rationaleLabelingPort;
    private final TransactionTemplate transactionTemplate;

    public JournalService(JournalRepository journalRepository,
                           JournalTradeNoteRepository journalTradeNoteRepository,
                           TradeLedgerPort tradeLedgerPort,
                           MarketDataPort marketDataPort,
                           RationaleLabelingPort rationaleLabelingPort,
                           PlatformTransactionManager transactionManager) {
        this.journalRepository = journalRepository;
        this.journalTradeNoteRepository = journalTradeNoteRepository;
        this.tradeLedgerPort = tradeLedgerPort;
        this.marketDataPort = marketDataPort;
        this.rationaleLabelingPort = rationaleLabelingPort;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    // ledger.domain.ports.JournalNotePort 구현체(JournalNotePortImpl)에서만 호출된다 — 증권사 연동
    // 해지로 거래가 삭제될 때 그 거래에 달린 근거도 함께 지운다. 별도 트랜잭션을 열지 않는다 — 호출자인
    // AccountDataCleanupService가 이미 연 트랜잭션에 합류해, 계좌 삭제 전체가 원자적으로 처리된다.
    public void deleteNotesByTradeIds(List<Long> tradeIds) {
        journalTradeNoteRepository.deleteByTradeIds(tradeIds);
    }

    // auth.domain.ports.JournalCleanupPort 구현체(JournalCleanupPortImpl)에서만 호출된다 — 계정 탈퇴 시
    // 사용자의 투자일지를 전부 지운다. 호출 시점에 이 사용자의 거래(및 journal_trade_notes)는 이미
    // 정리됐다고 가정하므로 여기서는 investment_journals만 지우면 된다.
    public void deleteAllJournals(Long userId) {
        journalRepository.deleteByUserId(userId);
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
        Journal journal = findOwnedJournal(query.journalId(), query.userId());
        return buildDetailResult(query.userId(), journal.getJournalDate(), Optional.of(journal));
    }

    // journalId로 순수 조회만 한다 — 소유권 판단은 하지 않는다.
    private Journal findJournal(Long journalId) {
        return journalRepository.findById(journalId)
                .orElseThrow(() -> new JournalException(JournalErrorCode.JOURNAL_NOT_FOUND));
    }

    // journalId로 조회한 뒤 요청한 userId 소유인지까지 확인한다. journalId 기반 진입점(조회/수정 등)은
    // 전부 이걸 통해서만 journal을 얻는다 — 존재 여부와 소유권 판단을 한 곳에 모아두기 위함.
    private Journal findOwnedJournal(Long journalId, Long userId) {
        Journal journal = findJournal(journalId);
        if (!journal.getUserId().equals(userId)) {
            throw new JournalException(JournalErrorCode.JOURNAL_NOT_FOUND);
        }
        return journal;
    }

    public TradeTimelineResult getTradeTimeline(GetTradeTimelineQuery query) {
        // 포맷성 검증(페이지/기간)을 가장 먼저 처리해 잘못된 요청이면 market/ledger 호출 없이 바로 실패시킨다.
        if (query.page() < 0 || query.size() < 1) {
            throw new JournalException(JournalErrorCode.INVALID_PAGE_PARAMS);
        }
        if (query.startDate() != null && query.endDate() != null && query.startDate().isAfter(query.endDate())) {
            throw new JournalException(JournalErrorCode.INVALID_DATE_RANGE);
        }

        SecurityInfo security = marketDataPort.findSecurities(List.of(query.securityId())).stream()
                .findFirst()
                .orElseThrow(() -> new JournalException(JournalErrorCode.SECURITY_NOT_FOUND));

        // find는 이번 페이지 분량만, count는 전체 건수만 반환한다(countTradesByDateRange/findTradesOn과 동일한 분리
        // 관례) — totalPages 계산에는 페이지에 안 담긴 전체 건수가 필요해서 별도 호출이 꼭 필요하다.
        List<TradeTimelineInfo> trades = tradeLedgerPort.findTradesBySecurity(
                query.userId(), query.securityId(), query.startDate(), query.endDate(), query.page(), query.size());
        long totalElements = tradeLedgerPort.countTradesBySecurity(
                query.userId(), query.securityId(), query.startDate(), query.endDate());

        // 근거 조회(tradeId 기준) → 그 근거들이 속한 journal 조회(journalId 기준), 2단계 배치 조회.
        // 날짜별 상세 조회와 달리 이 페이지의 거래들은 서로 다른 날짜(=서로 다른 journal)에 걸쳐 있을 수 있어
        // journalDate를 얻으려면 근거만으로는 안 되고 그 근거가 속한 journal을 한 번 더 찾아야 한다.
        Map<Long, JournalTradeNote> notesByTradeId = findNotesByTradeId(trades);
        Map<Long, LocalDate> journalDatesByJournalId = findJournalDatesByJournalId(notesByTradeId.values());

        List<TradeTimelineEntryResult> tradeResults = trades.stream()
                .map(trade -> toTradeTimelineEntryResult(trade, notesByTradeId, journalDatesByJournalId))
                .collect(Collectors.toList());

        int totalPages = (int) Math.ceil((double) totalElements / query.size());
        return new TradeTimelineResult(SecurityResult.from(security), tradeResults, query.page(), query.size(), totalElements, totalPages);
    }

    private Map<Long, JournalTradeNote> findNotesByTradeId(List<TradeTimelineInfo> trades) {
        if (trades.isEmpty()) {
            return Map.of();
        }
        List<Long> tradeIds = trades.stream().map(TradeTimelineInfo::tradeId).collect(Collectors.toList());
        return journalTradeNoteRepository.findByTradeIds(tradeIds).stream()
                .collect(Collectors.toMap(JournalTradeNote::getTradeId, note -> note));
    }

    // 근거들이 서로 다른 날짜(journal)에 걸쳐 있을 수 있어, 참조하는 journalId들을 배치 조회해 journalDate를 얻는다.
    private Map<Long, LocalDate> findJournalDatesByJournalId(Collection<JournalTradeNote> notes) {
        List<Long> journalIds = notes.stream().map(JournalTradeNote::getJournalId).distinct().collect(Collectors.toList());
        if (journalIds.isEmpty()) {
            return Map.of();
        }
        return journalRepository.findByIds(journalIds).stream()
                .collect(Collectors.toMap(Journal::getJournalId, Journal::getJournalDate));
    }

    private TradeTimelineEntryResult toTradeTimelineEntryResult(TradeTimelineInfo trade,
                                                                  Map<Long, JournalTradeNote> notesByTradeId,
                                                                  Map<Long, LocalDate> journalDatesByJournalId) {
        JournalTradeNote note = notesByTradeId.get(trade.tradeId());
        TradeNoteWithJournalResult noteResult = note == null ? null
                : TradeNoteWithJournalResult.from(note, journalDatesByJournalId.get(note.getJournalId()));
        return TradeTimelineEntryResult.from(trade, noteResult);
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
        boolean canCreate = journal.isEmpty() && !journalDate.isAfter(LocalDate.ofInstant(now, JOURNAL_ZONE));
        JournalInfoResult journalInfo = journal.map(j -> JournalInfoResult.from(j, now)).orElse(null);

        return new JournalDetailResult(journalDate, canCreate, journalInfo, tradeResults);
    }

    // 검증(트랜잭션 밖) → 라벨링(외부 LLM 호출, 트랜잭션 밖) → 영속화(트랜잭션 안) 순서로 진행한다.
    // LLM 호출처럼 느리고 실패할 수 있는 외부 호출을 DB 트랜잭션 밖으로 빼서 커넥션을 불필요하게 오래 붙잡지 않는다.
    public CreateJournalResult save(CreateJournalCommand command) {
        if (command.journalDate().isAfter(LocalDate.ofInstant(Instant.now(), JOURNAL_ZONE))) {
            throw new JournalException(JournalErrorCode.FUTURE_DATE_NOT_ALLOWED);
        }
        if (journalRepository.findByUserAndDate(command.userId(), command.journalDate()).isPresent()) {
            throw new JournalException(JournalErrorCode.JOURNAL_ALREADY_EXISTS);
        }

        List<TradeNoteCommand> tradeNotes = validateTradeNotes(command.userId(), command.journalDate(), command.tradeNotes());
        Map<Long, RationaleLabelType> labelsByTradeId = labelTradeNotes(tradeNotes);

        return transactionTemplate.execute(status -> {
            Journal journal = Journal.create(command.userId(), command.journalDate(), command.marketThought(), command.marketMood());
            Journal saved = journalRepository.save(journal);

            saveTradeNotes(saved.getJournalId(), tradeNotes, labelsByTradeId);

            return new CreateJournalResult(saved.getJournalId(), saved.getCreatedAt());
        });
    }

    // save()와 동일한 이유로 검증/라벨링은 트랜잭션 밖에서, journal 수정과 journal_trade_notes 반영(upsert+삭제)만
    // 하나의 트랜잭션으로 묶는다.
    public UpdateJournalResult update(UpdateJournalCommand command) {
        Journal journal = findOwnedJournal(command.journalId(), command.userId());

        if (!journal.isEditable(Instant.now())) {
            throw new JournalException(JournalErrorCode.JOURNAL_NOT_EDITABLE);
        }

        List<TradeNoteCommand> tradeNotes = validateTradeNotes(command.userId(), journal.getJournalDate(), command.tradeNotes());
        Map<Long, RationaleLabelType> labelsByTradeId = labelTradeNotes(tradeNotes);

        return transactionTemplate.execute(status -> {
            Journal updated = journal.update(command.marketThought(), command.marketMood());
            journalRepository.update(updated);

            deleteRemovedTradeNotes(command.journalId(), tradeNotes);
            saveTradeNotes(command.journalId(), tradeNotes, labelsByTradeId);

            return new UpdateJournalResult(updated.getJournalId(), updated.getUpdatedAt());
        });
    }

    // 근거 텍스트별로 LLM 라벨링을 시도한다. 실패해도 요청 전체를 실패시키지 않고 UNCLASSIFIED로 대체한다 —
    // 라벨링은 부가 정보이지 journal 작성의 필수 전제조건이 아니다.
    private Map<Long, RationaleLabelType> labelTradeNotes(List<TradeNoteCommand> tradeNotes) {
        Map<Long, RationaleLabelType> labelsByTradeId = new HashMap<>();
        for (TradeNoteCommand tradeNote : tradeNotes) {
            RationaleLabelType label;
            try {
                label = rationaleLabelingPort.classify(tradeNote.rationaleText());
            } catch (RationaleLabelingException e) {
                log.warn("근거 라벨링 실패 — UNCLASSIFIED로 대체합니다. tradeId={}", tradeNote.tradeId(), e);
                label = RationaleLabelType.UNCLASSIFIED;
            }
            labelsByTradeId.put(tradeNote.tradeId(), label);
        }
        return labelsByTradeId;
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

    private void saveTradeNotes(Long journalId, List<TradeNoteCommand> tradeNotes, Map<Long, RationaleLabelType> labelsByTradeId) {
        if (tradeNotes.isEmpty()) {
            return;
        }
        List<JournalTradeNote> notes = tradeNotes.stream()
                .map(tradeNote -> JournalTradeNote.create(
                        journalId, tradeNote.tradeId(), tradeNote.rationaleText(), labelsByTradeId.get(tradeNote.tradeId())))
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
