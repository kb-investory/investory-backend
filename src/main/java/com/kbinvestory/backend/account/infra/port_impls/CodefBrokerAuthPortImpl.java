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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

@Component
public class CodefBrokerAuthPortImpl implements BrokerAuthPort {

    private static final Logger log = LoggerFactory.getLogger(CodefBrokerAuthPortImpl.class);

    private static final String COUNTRY_CODE = "KR";
    private static final String BUSINESS_TYPE_SECURITIES = "ST";
    private static final String CLIENT_TYPE_INTEGRATED = "A"; // 통합: 보험, 증권
    private static final String LOGIN_TYPE_ID_PASSWORD = "1";

    private final EasyCodef easyCodef;
    private final String publicKey;
    private final EasyCodefServiceType serviceType;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CodefBrokerAuthPortImpl(@Value("${codef.demo.client-id}") String clientId,
                                    @Value("${codef.demo.client-secret}") String clientSecret,
                                    @Value("${codef.demo.public-key}") String publicKey,
                                    @Value("${codef.service-type}") String serviceType) {
        String trimmedClientId = clientId.trim();
        String trimmedClientSecret = clientSecret.trim();
        String trimmedPublicKey = publicKey.replaceAll("\\s+", "");

        log.debug("CODEF 클라이언트 설정 로드: clientIdLength={}, publicKeyLength={}, serviceType={}",
                trimmedClientId.length(), trimmedPublicKey.length(), serviceType);

        this.easyCodef = new EasyCodef();
        this.easyCodef.setClientInfoForDemo(trimmedClientId, trimmedClientSecret);
        this.easyCodef.setPublicKey(trimmedPublicKey);
        this.publicKey = trimmedPublicKey;
        this.serviceType = EasyCodefServiceType.valueOf(serviceType.trim().toUpperCase());
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
        // CODEF 공식 예제가 해당 없는 필드도 빈 문자열로 항상 포함시켜 보내므로 동일하게 맞춤
        account.put("add_password", "");
        account.put("birthDate", "");
        account.put("loginTypeLevel", "");
        account.put("clientTypeLevel", "");
        account.put("cardNo", "");
        account.put("cardPassword", "");

        HashMap<String, Object> parameterMap = new HashMap<>();
        parameterMap.put("accountList", List.of(account));

        log.debug("CODEF 계정등록 요청: organization={}, businessType={}, clientType={}, loginType={}, idLength={}",
                organization, BUSINESS_TYPE_SECURITIES, CLIENT_TYPE_INTEGRATED, LOGIN_TYPE_ID_PASSWORD, loginId.length());

        String rawResponse = requestCreateAccount(parameterMap);
        log.debug("CODEF 계정등록 원본 응답: {}", rawResponse);
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
            return easyCodef.createAccount(serviceType, parameterMap);
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