package com.investory.broker.infra.clients.mockbroker;

// GET /mock/system/orgs 응답의 배열 원소 하나. 최상위가 바로 JSON 배열로 내려온다.
// /mock/system/* 네임스페이스(로그인 등)는 /v2/invest/* 계열과 달리 snake_case가 아니라
// camelCase로 내려온다 — MockLoginResponse(같은 /mock/system/* 그룹)와 동일한 관례.
public record OrgItem(
    String orgCode,
    String orgName
) {
}
