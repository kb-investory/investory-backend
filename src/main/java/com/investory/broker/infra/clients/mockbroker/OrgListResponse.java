package com.investory.broker.infra.clients.mockbroker;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// GET /mock/system/orgs 응답. 필드명은 목 서버의 다른 목록 응답(account_list 등)과 동일한
// snake_case 명명 관례를 따른다고 가정한 것 — 실제 엔드포인트가 열리면 확인 필요.
public record OrgListResponse(
    @JsonProperty("org_list") List<OrgItem> orgList
) {
    public record OrgItem(
        @JsonProperty("org_code") String orgCode,
        @JsonProperty("org_name") String orgName
    ) {
    }
}
