package com.investory.journal.presentation.controller;

import com.investory.journal.domain.services.JournalService;
import com.investory.journal.domain.services.dto.query.GetJournalByIdQuery;
import com.investory.journal.domain.services.dto.query.GetJournalDetailQuery;
import com.investory.journal.domain.services.dto.query.GetJournalEntriesQuery;
import com.investory.journal.domain.services.dto.result.CreateJournalResult;
import com.investory.journal.domain.services.dto.result.JournalDetailResult;
import com.investory.journal.domain.services.dto.result.JournalEntryResult;
import com.investory.journal.domain.services.dto.result.UpdateJournalResult;
import com.investory.journal.presentation.dto.request.CreateJournalRequest;
import com.investory.journal.presentation.dto.request.UpdateJournalRequest;
import com.investory.journal.presentation.dto.response.CreateJournalResponse;
import com.investory.journal.presentation.dto.response.JournalDetailResponse;
import com.investory.journal.presentation.dto.response.JournalEntryListResponse;
import com.investory.journal.presentation.dto.response.UpdateJournalResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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

    // TODO: JWT 인증 도입 후 Principal.userId로 교체 (auth 도메인 미구현으로 임시 고정값 사용)
    private static final Long TEMP_USER_ID = 1L;

    private final JournalService journalService;

    public JournalController(JournalService journalService) {
        this.journalService = journalService;
    }

    @GetMapping("/entries")
    public JournalEntryListResponse getEntries(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<JournalEntryResult> results = journalService.getEntries(
                new GetJournalEntriesQuery(TEMP_USER_ID, startDate, endDate));
        return JournalEntryListResponse.from(results);
    }

    @GetMapping("/entries/on/{date}")
    public JournalDetailResponse getDetail(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        JournalDetailResult result = journalService.getDetail(new GetJournalDetailQuery(TEMP_USER_ID, date));
        return JournalDetailResponse.from(result);
    }

    @GetMapping("/entries/{journalId}")
    public JournalDetailResponse getByJournalId(@PathVariable Long journalId) {
        JournalDetailResult result = journalService.getByJournalId(new GetJournalByIdQuery(TEMP_USER_ID, journalId));
        return JournalDetailResponse.from(result);
    }

    @PostMapping("/entries")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateJournalResponse create(@RequestBody CreateJournalRequest request) {
        CreateJournalResult result = journalService.save(request.toCommand(TEMP_USER_ID));
        return CreateJournalResponse.from(result);
    }

    @PutMapping("/entries/{journalId}")
    public UpdateJournalResponse update(@PathVariable Long journalId, @RequestBody UpdateJournalRequest request) {
        UpdateJournalResult result = journalService.update(request.toCommand(TEMP_USER_ID, journalId));
        return UpdateJournalResponse.from(result);
    }
}
