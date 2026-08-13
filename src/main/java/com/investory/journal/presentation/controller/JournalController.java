package com.investory.journal.presentation.controller;

import com.investory.journal.domain.services.JournalService;
import com.investory.journal.domain.services.dto.query.GetJournalByIdQuery;
import com.investory.journal.domain.services.dto.query.GetJournalDetailQuery;
import com.investory.journal.domain.services.dto.query.GetJournalEntriesQuery;
import com.investory.journal.domain.services.dto.query.GetTradeTimelineQuery;
import com.investory.journal.domain.services.dto.result.CreateJournalResult;
import com.investory.journal.domain.services.dto.result.JournalDetailResult;
import com.investory.journal.domain.services.dto.result.JournalEntryResult;
import com.investory.journal.domain.services.dto.result.TradeTimelineResult;
import com.investory.journal.domain.services.dto.result.UpdateJournalResult;
import com.investory.journal.presentation.dto.request.CreateJournalRequest;
import com.investory.journal.presentation.dto.request.UpdateJournalRequest;
import com.investory.journal.presentation.dto.response.CreateJournalResponse;
import com.investory.journal.presentation.dto.response.JournalDetailResponse;
import com.investory.journal.presentation.dto.response.JournalEntryListResponse;
import com.investory.journal.presentation.dto.response.TradeTimelineResponse;
import com.investory.journal.presentation.dto.response.UpdateJournalResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/journal")
public class JournalController {

    private final JournalService journalService;

    public JournalController(JournalService journalService) {
        this.journalService = journalService;
    }

    @GetMapping("/entries")
    public JournalEntryListResponse getEntries(
            @AuthenticationPrincipal Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<JournalEntryResult> results = journalService.getEntries(
                new GetJournalEntriesQuery(userId, startDate, endDate));
        return JournalEntryListResponse.from(results);
    }

    @GetMapping("/entries/on/{date}")
    public JournalDetailResponse getDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        JournalDetailResult result = journalService.getDetail(new GetJournalDetailQuery(userId, date));
        return JournalDetailResponse.from(result);
    }

    @GetMapping("/entries/{journalId}")
    public JournalDetailResponse getByJournalId(@AuthenticationPrincipal Long userId, @PathVariable Long journalId) {
        JournalDetailResult result = journalService.getByJournalId(new GetJournalByIdQuery(userId, journalId));
        return JournalDetailResponse.from(result);
    }

    @PostMapping("/entries")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateJournalResponse create(@AuthenticationPrincipal Long userId, @RequestBody CreateJournalRequest request) {
        CreateJournalResult result = journalService.save(request.toCommand(userId));
        return CreateJournalResponse.from(result);
    }

    @PutMapping("/entries/{journalId}")
    public UpdateJournalResponse update(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long journalId,
            @RequestBody UpdateJournalRequest request) {
        UpdateJournalResult result = journalService.update(request.toCommand(userId, journalId));
        return UpdateJournalResponse.from(result);
    }

    @GetMapping("/trades")
    public TradeTimelineResponse getTrades(
            @AuthenticationPrincipal Long userId,
            @RequestParam Long securityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        TradeTimelineResult result = journalService.getTradeTimeline(
                new GetTradeTimelineQuery(userId, securityId, startDate, endDate, page, size));
        return TradeTimelineResponse.from(result);
    }
}
