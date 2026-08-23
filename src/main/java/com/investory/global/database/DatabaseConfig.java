package com.investory.global.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@PropertySource("classpath:application.properties")
@MapperScan(basePackages = "com.investory", annotationClass = Mapper.class)
public class DatabaseConfig {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    // 예전엔 풀 크기를 지정하지 않아 HikariCP 기본값(maximumPoolSize=10)으로 운영됐다. 1000 VU
    // 부하테스트(tendency-analysis 시나리오)에서 correctness 버그(#204 계열)를 다 걷어낸 뒤에도
    // active=10/waiting=1~2가 지속되며 p95가 24초까지 늘어지는 게 확인됐다 — 남은 병목이 순수하게
    // 이 풀 크기였다. 로컬 MySQL(mysql:8.0 기본값) max_connections는 151로 여유가 있지만, 풀
    // 크기는 max_connections 여유가 아니라 DB가 실제로 동시에 생산적으로 처리할 수 있는 양을
    // 기준으로 잡아야 하므로 그 상한까지 올리지 않는다. 1차로 30으로 올려 재측정하고, waiting이
    // 계속 찍히면 더 올리거나(다른 병목— 예: trade_matches 락 경합 — 이 먼저 걸리는지 확인 후),
    // waiting이 사라지면 그 값을 유지한다. leakDetectionThreshold는 동작을 바꾸지 않고 연결이
    // 비정상적으로 오래 잡혀 있을 때만 경고 로그를 남긴다 — 순수 관측용으로 추가.
    @Bean
    public DataSource dataSource(
            @Value("${datasource.driver-class-name}") String driverClassName,
            @Value("${datasource.url}") String url,
            @Value("${datasource.username}") String username,
            @Value("${datasource.password}") String password) {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(driverClassName);
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(30);
        config.setLeakDetectionThreshold(30_000);
        return new HikariDataSource(config);
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath:mapper/**/*.xml")
        );
        return factoryBean.getObject();
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}