package com.example.itborrow;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named="TEST_POSTGRES_URL", matches="jdbc:postgresql://127\\.0\\.0\\.1:55439/.*")
@SpringBootTest(properties = {
        "app.security.login-limit=1000", "app.security.registration-limit=1000", "app.mail.provider=smtp", "app.mail.brevo-api-key=", "app.public-base-url=http://localhost:8080",
        "spring.datasource.url=${TEST_POSTGRES_URL}",
        "spring.datasource.driver-class-name=org.postgresql.Driver", "spring.datasource.username=${TEST_POSTGRES_USERNAME:review_test}",
        "spring.datasource.password=${TEST_POSTGRES_PASSWORD:}",
        "spring.jpa.hibernate.ddl-auto=none", "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "spring.flyway.enabled=false", "management.health.mail.enabled=false", "app.jobs.enabled=false",
        "app.mail.from=tests@example.test", "spring.mail.host=smtp.example.test", "spring.sql.init.mode=always",
        "spring.jpa.show-sql=false", "logging.level.org.hibernate.SQL=warn",
        "borrow.overdue.initial-delay-ms=86400000", "logging.level.root=WARN", "logging.level.org.springframework=WARN",
        "debug=false"
})
@AutoConfigureMockMvc
class PostgresWorkflowIntegrationTest extends WorkflowIntegrationTest {}
