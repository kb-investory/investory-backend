package com.kbinvestory.backend.account.infra.clients;

import com.kbinvestory.backend.account.infra.exception.AccountInfraErrorCode;
import com.kbinvestory.backend.account.infra.exception.AccountInfraException;

import java.util.Map;

// CODEF 증권사 기관코드(organization) 매핑. brokerage_providers.provider_code 기준
public class CodefOrganizationCodes {

    private static final Map<String, String> CODES = Map.of(
        "KB", "0218",
        "NH", "0247",
        "MIRAE", "0238",
        "SAMSUNG", "0240",
        "KIWOOM", "0264"
    );

    private CodefOrganizationCodes() {
    }

    public static String resolve(String providerCode) {
        String organization = CODES.get(providerCode);
        if (organization == null) {
            throw new AccountInfraException(AccountInfraErrorCode.CODEF_REQUEST_FAILED,
                    new IllegalStateException("CODEF 기관코드 매핑이 없는 증권사입니다: " + providerCode));
        }
        return organization;
    }
}