package com.investory.market.infra.clients.kis;

import com.investory.market.domain.constant.MarketType;
import com.investory.market.domain.exception.MarketErrorCode;
import com.investory.market.domain.exception.MarketException;
import com.investory.market.domain.ports.KisMarketDataPort;
import com.investory.market.domain.ports.dto.StockInfoDto;
import com.investory.market.domain.ports.dto.StockPriceDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
public class KisMarketDataClient implements KisMarketDataPort {

    private static final Logger log = LoggerFactory.getLogger(KisMarketDataClient.class);
    private static final DateTimeFormatter KIS_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String STOCK_INFO_TR_ID = "CTPF1002R";
    private static final String PRICE_TR_ID = "FHPST01010000";

    private final RestTemplate restTemplate;

    @Value("${kis.base-url}")
    private String baseUrl;

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    // KIS 초당 호출 건수 제한에 걸리지 않도록, 여기서 마지막 호출 시각을 기준으로 최소 간격을 강제한다.
    // (배치가 종목을 수천 개 돌 때 "종목당 sleep" 대신 이 클라이언트 한 곳에서 전체 호출 속도를 통제한다)
    @Value("${kis.rate-limit-per-second:8}")
    private int rateLimitPerSecond;

    private volatile long lastCallAtMillis = 0L;

    // 토큰 발급은 분당 호출 제한이 있어서, 매 요청마다 새로 받지 않고 만료 전까지 메모리에 캐싱해서 재사용한다.
    private volatile String cachedAccessToken;
    private volatile Instant cachedTokenExpiry = Instant.EPOCH;

    public KisMarketDataClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public StockInfoDto fetchStockInfo(String stockCode) {
        HttpHeaders headers = defaultHeaders(STOCK_INFO_TR_ID);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/uapi/domestic-stock/v1/quotations/search-stock-info")
                .queryParam("PRDT_TYPE_CD", "300")
                .queryParam("PDNO", stockCode)
                .toUriString();

        KisStockInfoResponse response = exchange(url, request, KisStockInfoResponse.class);
        KisStockInfoResponse.Output output = response.getOutput();

        boolean isKospi = StringUtils.hasText(output.getScts_mket_lstg_dt());
        boolean isKosdaq = StringUtils.hasText(output.getKosdaq_mket_lstg_dt());

        MarketType marketType = isKospi ? MarketType.KOSPI : (isKosdaq ? MarketType.KOSDAQ : null);

        LocalDate listedDate = isKospi
                ? parseKisDate(output.getScts_mket_lstg_dt())
                : parseKisDate(output.getKosdaq_mket_lstg_dt());

        LocalDate delistedDate = isKospi
                ? parseKisDate(output.getScts_mket_lstg_abol_dt())
                : parseKisDate(output.getKosdaq_mket_lstg_abol_dt());

        String stockName = StringUtils.hasText(output.getPrdt_name())
                ? output.getPrdt_name()
                : output.getPrdt_abrv_name();

        return StockInfoDto.builder()
                .stockCode(stockCode)
                .stockName(stockName)
                .marketType(marketType)
                .stdIdstClsfCode(output.getStd_idst_clsf_cd())
                .stdIdstClsfName(output.getStd_idst_clsf_cd_name())
                .listedDate(listedDate)
                .delistedDate(delistedDate)
                .isActive(delistedDate == null)
                .build();
    }

    @Override
    public StockPriceDto fetchDailyPrice(String stockCode) {
        HttpHeaders headers = defaultHeaders(PRICE_TR_ID);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-price-2")
                .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                .queryParam("FID_INPUT_ISCD", stockCode)
                .toUriString();

        KisPriceResponse response = exchange(url, request, KisPriceResponse.class);
        KisPriceResponse.Output output = response.getOutput();

        return StockPriceDto.builder()
                .priceDate(LocalDate.now())
                .lowPrice(toLong(output.getStck_lwpr()))
                .highPrice(toLong(output.getStck_hgpr()))
                .openPrice(toLong(output.getStck_oprc()))
                .closePrice(toLong(output.getStck_prpr()))
                .dailyReturnRate(toBigDecimal(output.getPrdy_ctrt()))
                .tradingVolume(toLong(output.getAcml_vol()))
                .tradingValue(toLong(output.getAcml_tr_pbmn()))
                .build();
    }

    private <T> T exchange(String url, HttpEntity<Void> request, Class<T> responseType) {
        awaitRateLimit();
        try {
            ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.GET, request, responseType);
            T body = response.getBody();
            if (body == null) {
                log.error("KIS API 응답 바디가 비어있음. url={}", url);
                throw new MarketException(MarketErrorCode.KIS_API_ERROR);
            }
            return body;
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            // KIS는 4xx/5xx여도 rt_cd/msg_cd/msg1이 담긴 JSON 바디를 내려주는 경우가 많아서 그대로 로그에 남긴다.
            log.error("KIS API 호출 실패. url={}, status={}, body={}", url, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new MarketException(MarketErrorCode.KIS_API_ERROR);
        } catch (RestClientException e) {
            log.error("KIS API 호출 실패(네트워크/타임아웃 등). url={}", url, e);
            throw new MarketException(MarketErrorCode.KIS_API_ERROR);
        }
    }

    private HttpHeaders defaultHeaders(String trId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("authorization", "Bearer " + getAccessToken());
        headers.add("appkey", appKey);
        headers.add("appsecret", appSecret);
        headers.add("tr_id", trId);
        headers.add("custtype", "P");
        return headers;
    }

    // 캐싱된 토큰이 아직 유효하면 재사용하고, 없거나 만료됐으면 새로 발급받는다.
    private synchronized String getAccessToken() {
        log.info("[진단] 토큰 상태. 캐시존재={}, 아직유효={}",
                cachedAccessToken != null, Instant.now().isBefore(cachedTokenExpiry));

        if (cachedAccessToken != null && Instant.now().isBefore(cachedTokenExpiry)) {
            return cachedAccessToken;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = new HashMap<>();
        body.put("grant_type", "client_credentials");
        body.put("appkey", appKey);
        body.put("appsecret", appSecret);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<KisTokenResponse> response =
                    restTemplate.exchange(baseUrl + "/oauth2/tokenP", HttpMethod.POST, request, KisTokenResponse.class);
            KisTokenResponse tokenResponse = response.getBody();
            if (tokenResponse == null || !StringUtils.hasText(tokenResponse.getAccess_token())) {
                log.error("KIS 토큰 발급 응답에 access_token이 없음. response={}", tokenResponse);
                throw new MarketException(MarketErrorCode.KIS_API_ERROR);
            }

            // expires_in(초)보다 1분 일찍 만료 처리해서, 만료 직전 요청이 실패하는 걸 방지한다.
            int expiresInSeconds = tokenResponse.getExpires_in() != null ? tokenResponse.getExpires_in() : 3600;
            cachedAccessToken = tokenResponse.getAccess_token();
            cachedTokenExpiry = Instant.now().plusSeconds(Math.max(0, expiresInSeconds - 60*60*2));

            log.info("KIS 토큰 발급됨. expires_in={}, 만료처리시각={}", expiresInSeconds, Instant.now().plusSeconds(Math.max(0, expiresInSeconds - 60*60*2)));
            return cachedAccessToken;
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            // appkey/appsecret이 틀렸거나 미승인 상태면 KIS가 여기서 4xx + rt_cd/msg1으로 이유를 알려준다.
            log.error("KIS 토큰 발급 실패. status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new MarketException(MarketErrorCode.KIS_API_ERROR);
        } catch (RestClientException e) {
            log.error("KIS 토큰 발급 실패(네트워크/타임아웃 등)", e);
            throw new MarketException(MarketErrorCode.KIS_API_ERROR);
        }
    }

    // 설정된 초당 허용 건수(kis.rate-limit-per-second)를 넘지 않도록, 필요하면 마지막 호출 이후 남은 시간만큼만 대기한다.
    // 여기 한 곳만 통과하면 되므로, 배치가 종목을 몇 개를 돌든 호출부(서비스 계층)는 속도 조절을 신경 쓸 필요가 없다.
    private synchronized void awaitRateLimit() {
        long minIntervalMillis = 1000L / Math.max(1, rateLimitPerSecond);
        long elapsed = System.currentTimeMillis() - lastCallAtMillis;
        long waitMillis = minIntervalMillis - elapsed;

        if (waitMillis > 0) {
            try {
                Thread.sleep(waitMillis);
            } catch (InterruptedException e) {
                // 여기서 다시 interrupt()를 세팅하면 그 플래그 때문에 뒤이어 처리되는 다른 종목들의
                // awaitRateLimit()까지 전부 즉시 인터럽트되어 대기 없이 KIS를 연달아 호출하게 되고,
                // 그 결과로 초당 호출 제한에 걸려 연쇄적으로 실패하는 문제가 있었다.
                // 배치 도중의 일시적 인터럽트 하나가 남은 전체 종목의 레이트리밋을 깨서는 안 되므로 그냥 무시하고 계속 진행한다.
                log.warn("레이트리밋 대기 중 인터럽트 발생 - 무시하고 계속 진행합니다.");
            }
        }

        lastCallAtMillis = System.currentTimeMillis();
    }

    private LocalDate parseKisDate(String yyyymmdd) {
        if (!StringUtils.hasText(yyyymmdd)) {
            return null;
        }
        return LocalDate.parse(yyyymmdd, KIS_DATE_FORMAT);
    }

    private BigDecimal toBigDecimal(String value) {
        return StringUtils.hasText(value) ? new BigDecimal(value) : null;
    }

    private Long toLong(String value) {
        return StringUtils.hasText(value) ? Long.valueOf(value) : null;
    }
}
