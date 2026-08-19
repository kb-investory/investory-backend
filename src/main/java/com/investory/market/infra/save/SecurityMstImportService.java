package com.investory.market.infra.save;

import com.investory.market.domain.constant.MarketType;
import com.investory.market.domain.model.Security;
import com.investory.market.domain.repositories.SecurityRepository;
import com.investory.market.infra.exception.MarketInfraErrorCode;
import com.investory.market.infra.exception.MarketInfraException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * KIS에서 제공하는 kospi_code.mst / kosdaq_code.mst 파일을 읽어
 * securities 테이블에 저장(신규는 insert, 이미 있는 종목은 update)하는 서비스.
 *
 * 그대로 재사용한다 - securityCode 존재 여부로 insert/update를 분기하는 upsert 로직은
 * 이미 SecurityRepositoryImpl.save()에 구현되어 있다.
 */
@Component
public class SecurityMstImportService {

    private static final Logger log = LoggerFactory.getLogger(SecurityMstImportService.class);

    private final SecurityRepository securityRepository;

    public SecurityMstImportService(SecurityRepository securityRepository) {
        this.securityRepository = securityRepository;
    }

    @Transactional
    public int importKospi(Path filePath) {
        return importFile(filePath, MarketType.KOSPI);
    }

    @Transactional
    public int importKosdaq(Path filePath) {
        return importFile(filePath, MarketType.KOSDAQ);
    }

    private int importFile(Path filePath, MarketType marketType) {
        int savedCount = 0;
        int skippedCount = 0;

        try (BufferedReader reader = Files.newBufferedReader(filePath, Charset.forName("CP949"))) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) {
                    continue;
                }

                Security security = SecurityMstLineParser.parse(line, marketType);
                if (security == null) {
                    skippedCount++;
                    log.warn("{}: {}번째 라인 파싱 실패로 건너뜁니다.", marketType, lineNo);
                    continue;
                }

                securityRepository.save(security);
                savedCount++;
            }
        } catch (IOException e) {
            throw new MarketInfraException(MarketInfraErrorCode.STOCK_MST_IMPORT_FAILED, e);
        }

        log.info("{} 마스터 파일 import 완료. path={}, 저장={}, 스킵={}",
                marketType, filePath, savedCount, skippedCount);
        return savedCount;
    }
}
