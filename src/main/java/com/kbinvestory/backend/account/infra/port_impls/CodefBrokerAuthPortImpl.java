package com.kbinvestory.backend.account.infra.port_impls;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kbinvestory.backend.account.domain.ports.BrokerAuthPort;
import com.kbinvestory.backend.account.domain.ports.dto.BrokerAuthInfo;
import com.kbinvestory.backend.account.infra.clients.CodefOrganizationCodes;
import com.kbinvestory.backend.account.infra.exception.AccountInfraErrorCode;
import com.kbinvestory.backend.account.infra.exception.AccountInfraException;
import io.codef.api.EasyCodef;
import io.codef.api.EasyCodefServiceType;
import io.codef.api.EasyCodefUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

@Component
public class CodefBrokerAuthPortImpl implements BrokerAuthPort {

    private static final String COUNTRY_CODE = "KR";
    private static final String BUSINESS_TYPE_SECURITIES = "ST";
    private static final String CLIENT_TYPE_INTEGRATED = "A"; // 통합: 보험, 증권
    private static final String LOGIN_TYPE_ID_PASSWORD = "1";

    private final EasyCodef easyCodef;
    private final String publicKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CodefBrokerAuthPortImpl(@Value("${codef.demo.client-id}") String clientId,
                                    @Value("${codef.demo.client-secret}") String clientSecret,
                                    @Value("${codef.demo.public-key}") String publicKey) {
        this.easyCodef = new EasyCodef();
        this.easyCodef.setClientInfoForDemo(clientId, clientSecret);
        this.easyCodef.setPublicKey(publicKey);
        this.publicKey = publicKey;
    }

    @Override
    public BrokerAuthInfo authenticate(String providerCode, String loginId, String password) {
        String organization = CodefOrganizationCodes.resolve(providerCode);

        HashMap<String, Object> account = new HashMap<>();
        account.put("countryCode", COUNTRY_CODE);
        account.put("businessType", BUSINESS_TYPE_SECURITIES);
        account.put("clientType", CLIENT_TYPE_INTEGRATED);
        account.put("organization", organization);
        account.put("loginType", LOGIN_TYPE_ID_PASSWORD);
        account.put("id", loginId);
        account.put("password", encryptPassword(password));

        HashMap<String, Object> parameterMap = new HashMap<>();
        parameterMap.put("accountList", List.of(account));

        String rawResponse = requestCreateAccount(parameterMap);
        return parseResponse(rawResponse);
    }

    private String encryptPassword(String password) {
        try {
            return EasyCodefUtil.encryptRSA(password, publicKey);
        } catch (Exception e) {
            throw new AccountInfraException(AccountInfraErrorCode.CODEF_REQUEST_FAILED, e);
        }
    }

    private String requestCreateAccount(HashMap<String, Object> parameterMap) {
        try {
            return easyCodef.createAccount(EasyCodefServiceType.DEMO, parameterMap);
        } catch (Exception e) {
            throw new AccountInfraException(AccountInfraErrorCode.CODEF_REQUEST_FAILED, e);
        }
    }

    private BrokerAuthInfo parseResponse(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode data = root.has("data") ? root.get("data") : root;
            JsonNode connectedIdNode = data.get("connectedId");

            if (connectedIdNode != null && !connectedIdNode.isNull() && !connectedIdNode.asText().isBlank()) {
                return new BrokerAuthInfo(true, connectedIdNode.asText(), null);
            }
            return new BrokerAuthInfo(false, null, extractFirstErrorMessage(data));
        } catch (Exception e) {
            throw new AccountInfraException(AccountInfraErrorCode.CODEF_RESPONSE_PARSE_FAILED, e);
        }
    }

    private String extractFirstErrorMessage(JsonNode data) {
        JsonNode errorList = data.get("errorList");
        if (errorList != null && errorList.isArray() && errorList.size() > 0) {
            JsonNode message = errorList.get(0).get("message");
            if (message != null && !message.isNull()) {
                return message.asText();
            }
        }
        return "증권사 인증에 실패했습니다.";
    }
}