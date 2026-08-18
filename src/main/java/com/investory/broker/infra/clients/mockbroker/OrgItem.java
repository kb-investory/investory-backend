package com.investory.broker.infra.clients.mockbroker;

import com.fasterxml.jackson.annotation.JsonProperty;

// GET /mock/system/orgs 응답의 배열 원소 하나. 이 엔드포인트는 다른 목록 응답(account_list 등)과 달리
// org_list로 감싸지 않고 최상위가 바로 JSON 배열로 내려온다 (실제 연동 후 확인됨).
public record OrgItem(
    @JsonProperty("org_code") String orgCode,
    @JsonProperty("org_name") String orgName
) {
}
