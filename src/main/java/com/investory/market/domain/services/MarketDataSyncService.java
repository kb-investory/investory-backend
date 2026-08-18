package com.investory.market.domain.services;

import com.investory.market.domain.model.Security;
import com.investory.market.domain.model.SecurityPrice;
import com.investory.market.domain.ports.SecurityDataProviderPort;
import com.investory.market.domain.ports.dto.SecurityInfoDto;
import com.investory.market.domain.ports.dto.SecurityPriceDto;
import com.investory.market.domain.repositories.SecurityPriceRepository;
import com.investory.market.domain.repositories.SecurityRepository;
import com.investory.market.domain.services.dto.command.SyncSecurityCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MarketDataSyncService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataSyncService.class);

    // 마스터 정보(업종/상장여부 등)는 자주 안 바뀌므로, 이 기간 안에 이미 갱신됐으면 매일 배치에서는 다시 안 부른다.
    // (배치 때 매번 정보+시세 2번씩 부르면 종목 수가 많을 때 KIS 호출량이 배로 늘어나기 때문)
    private static final long STOCK_INFO_REFRESH_INTERVAL_DAYS = 0;

    // 종목 하나가 KIS 일시 오류로 실패했을 때 몇 번까지 재시도할지, 재시도 사이에 얼마나 대기할지
    private static final int MAX_ATTEMPTS_PER_STOCK = 3;
    private static final long RETRY_BACKOFF_MILLIS = 300;

    private final SecurityDataProviderPort securityDataProviderPort;
    private final SecurityRepository securityRepository;
    private final SecurityPriceRepository securityPriceRepository;

    public MarketDataSyncService(SecurityDataProviderPort securityDataProviderPort,
                                  SecurityRepository securityRepository,
                                  SecurityPriceRepository securityPriceRepository) {
        this.securityDataProviderPort = securityDataProviderPort;
        this.securityRepository = securityRepository;
        this.securityPriceRepository = securityPriceRepository;
    }

    // KIS search-stock-info를 조회해서 securities 테이블에 upsert한다.
    public Security syncStockInfo(SyncSecurityCommand command) {
        SecurityInfoDto dto = securityDataProviderPort.fetchSecurityInfo(command.securityCode());
        Security security = Security.create(dto);
        return securityRepository.save(security);
    }

    // KIS inquire-price-2를 조회해서 security_daily_prices 테이블에 저장한다.
    // securities에 해당 종목이 없으면(먼저 syncStockInfo를 안 했으면) 먼저 채우고 진행한다.
    public SecurityPrice syncDailyPrice(SyncSecurityCommand command) {
        Security security = securityRepository.findBySecurityCode(command.securityCode())
                .orElseGet(() -> syncStockInfo(command));

        SecurityPriceDto dto = securityDataProviderPort.fetchDailyPrice(command.securityCode());
        SecurityPrice securityPrice = SecurityPrice.create(security.getSecurityId(), dto);

        return securityPriceRepository.save(securityPrice);
    }

    // 종목정보 + 시세를 채운다. 마스터정보는 최근에 이미 갱신됐으면 건너뛰고 시세만 조회한다
    // (그래야 종목 수가 많아도 매일 배치의 KIS 호출량이 "시세 1번"으로 유지된다).
    public void syncStockAndPrice(SyncSecurityCommand command) {
        Optional<Security> existing = securityRepository.findBySecurityCode(command.securityCode());
        boolean needsInfoRefresh = existing.isEmpty()
                || existing.get().getUpdatedAt().isBefore(LocalDateTime.now().minusDays(STOCK_INFO_REFRESH_INTERVAL_DAYS));

        if (needsInfoRefresh) {
            syncStockInfo(command);
        }
        syncDailyPrice(command);
    }

    // 매일 배치용: 이미 등록된 종목 전체를 대상으로 그날치 정보/시세를 갱신한다.
    // 종목 하나가 실패해도(KIS 일시 오류 등) 전체 배치가 죽지 않도록 개별적으로 예외를 잡고 다음 종목으로 넘어간다.
    // 종목 사이의 호출 속도 조절(초당 몇 건까지)은 KisSecurityDataProvider가 전담한다 - 여기서는 신경 쓸 필요 없다.
    public void syncAllTrackedStocks() {
        List<String> securityCodes = securityRepository.findAllSecurityCodes();
        log.info("일일 종목 데이터 배치 시작. 대상 종목 수={}", securityCodes.size());

        int successCount = 0;
        for (String securityCode : securityCodes) {
            if (syncWithRetry(securityCode)) {
                successCount++;
            }
        }

        log.info("일일 종목 데이터 배치 종료. 성공={}/{}", successCount, securityCodes.size());
    }

    // 종목 하나를 최대 MAX_ATTEMPTS_PER_STOCK번까지 재시도한다.
    // "안 될 놈은 계속 안 되는" 경우(잘못된 종목코드 등)와 "일시적으로 삐끗한" 경우를 구분하지 않고
    // 일단 짧게 재시도해보고, 그래도 계속 실패하면 그 종목만 포기하고 다음으로 넘어간다.
    private boolean syncWithRetry(String securityCode) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS_PER_STOCK; attempt++) {
            try {
                syncStockAndPrice(new SyncSecurityCommand(securityCode));
                return true;
            } catch (Exception e) {
                boolean isLastAttempt = (attempt == MAX_ATTEMPTS_PER_STOCK);
                if (isLastAttempt) {
                    log.error("종목 배치 갱신 최종 실패(재시도 {}회 소진). securityCode={}", MAX_ATTEMPTS_PER_STOCK, securityCode, e);
                } else {
                    log.warn("종목 배치 갱신 실패, 재시도합니다. securityCode={}, attempt={}/{}, cause={}",
                            securityCode, attempt, MAX_ATTEMPTS_PER_STOCK, e.getMessage());
                    sleep(RETRY_BACKOFF_MILLIS);
                }
            }
        }
        return false;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            log.warn("재시도 대기 중 인터럽트 발생 - 무시하고 계속 진행합니다.");
        }
    }
}
