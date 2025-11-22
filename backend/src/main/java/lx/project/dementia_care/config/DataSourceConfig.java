package lx.project.dementia_care.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class DataSourceConfig {

    @Value("${DB_HOST:localhost}")
    private String dbHost;

    @Value("${DB_PORT:5432}")
    private String dbPort;

    @Value("${DB_NAME:finalproject}")
    private String dbName;

    @Value("${DB_USER:postgres}")
    private String dbUser;

    @Value("${DB_PASSWORD:rootroot}")
    private String dbPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        String jdbcUrl;
        
        // Cloud SQL Unix Socket 연결인지 확인
        if (dbHost != null && dbHost.startsWith("/cloudsql/")) {
            // Unix Socket 연결: Cloud SQL Socket Factory 사용
            // 형식: jdbc:postgresql:///DATABASE_NAME?cloudSqlInstance=PROJECT_ID:REGION:INSTANCE_NAME&socketFactory=com.google.cloud.sql.postgres.SocketFactory
            // 주의: cloudSqlInstance에는 /cloudsql/ 접두사가 없어야 하고, 프로젝트 ID만 포함되어야 함
            String instanceConnectionName = dbHost.substring("/cloudsql/".length()); // /cloudsql/ 제거
            // 예: dementia-care-project:asia-northeast3:dementia-care-db
            // Socket Factory가 자동으로 Unix Socket 경로를 찾음
            jdbcUrl = String.format(
                "jdbc:postgresql:///%s?cloudSqlInstance=%s&socketFactory=com.google.cloud.sql.postgres.SocketFactory",
                dbName,
                instanceConnectionName
            );
        } else {
            // 일반 TCP 연결
            jdbcUrl = String.format(
                "jdbc:postgresql://%s:%s/%s",
                dbHost,
                dbPort,
                dbName
            );
        }

        return DataSourceBuilder.create()
                .url(jdbcUrl)
                .username(dbUser)
                .password(dbPassword)
                .driverClassName("org.postgresql.Driver")
                .build();
    }
}

