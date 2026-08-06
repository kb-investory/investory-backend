package com.investory.market.infra.save;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 애플리케이션(root) 컨텍스트가 기동될 때 kospi_code.mst / kosdaq_code.mst 파일을
 * 읽어 stocks 테이블로 자동 import 한다.
 *
 * 이 프로젝트는 Spring Boot가 아니라 순수 Spring MVC(WAR)라서 ApplicationRunner를
 * 쓸 수 없으므로, ContextRefreshedEvent를 이용해 컨텍스트 초기화 완료 시점에 실행한다.
 * root 컨텍스트(WebApplicationInitializer의 getRootConfigClasses)와
 * 서블릿(자식) 컨텍스트가 각각 이벤트를 발행하므로, 부모가 없는(=root) 컨텍스트일 때만
 * 한 번 실행되도록 가드를 둔다.
 *
 * application.properties 설정:
 *   stock.mst.kospi-path=./data/kospi_code.mst
 *   stock.mst.kosdaq-path=./data/kosdaq_code.mst
 *   stock.mst.auto-import-on-startup=true
 */
@Component
public class StockMstInitializer implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger log = LoggerFactory.getLogger(StockMstInitializer.class);

    private final StockMstImportService stockMstImportService;

    @Value("${stock.mst.kospi-path:./data/kospi_code.mst}")
    private String kospiPath;

    @Value("${stock.mst.kosdaq-path:./data/kosdaq_code.mst}")
    private String kosdaqPath;

    @Value("${stock.mst.auto-import-on-startup:true}")
    private boolean autoImportOnStartup;

    private boolean alreadyRan = false;

    public StockMstInitializer(StockMstImportService stockMstImportService) {
        this.stockMstImportService = stockMstImportService;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 자식(서블릿) 컨텍스트에서도 이벤트가 올라오므로 root 컨텍스트에서 한 번만 실행한다.
        if (event.getApplicationContext().getParent() != null) {
            return;
        }
        if (alreadyRan) {
            return;
        }
        alreadyRan = true;

        if (!autoImportOnStartup) {
            log.info("stock.mst.auto-import-on-startup=false 설정으로 마스터 파일 자동 import를 건너뜁니다.");
            return;
        }

        importKospiIfExists();
        importKosdaqIfExists();
    }

    private void importKospiIfExists() {
        Path path = Paths.get(kospiPath);
        if (!Files.exists(path)) {
            log.warn("KOSPI 마스터 파일을 찾을 수 없어 import를 건너뜁니다. path={}", kospiPath);
            return;
        }
        try {
            int count = stockMstImportService.importKospi(path);
            log.info("KOSPI 종목 마스터 import 완료. 처리 건수={}", count);
        } catch (Exception e) {
            log.error("KOSPI 종목 마스터 import 중 오류가 발생했습니다. path={}", kospiPath, e);
        }
    }

    private void importKosdaqIfExists() {
        Path path = Paths.get(kosdaqPath);
        if (!Files.exists(path)) {
            log.warn("KOSDAQ 마스터 파일을 찾을 수 없어 import를 건너뜁니다. path={}", kosdaqPath);
            return;
        }
        try {
            int count = stockMstImportService.importKosdaq(path);
            log.info("KOSDAQ 종목 마스터 import 완료. 처리 건수={}", count);
        } catch (Exception e) {
            log.error("KOSDAQ 종목 마스터 import 중 오류가 발생했습니다. path={}", kosdaqPath, e);
        }
    }
}
