package com.example.itborrow;

import static org.assertj.core.api.Assertions.*;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.DriverManager;
import java.util.UUID;

@EnabledIfEnvironmentVariable(
        named = "TEST_POSTGRES_URL",
        matches = "jdbc:postgresql://127\\.0\\.0\\.1:55439/.*")
class PostgresMigrationTest {
    @Test
    void freshAndExistingSchemasMigrateWithoutDeletingData() throws Exception {
        String url = System.getenv("TEST_POSTGRES_URL");
        String username = System.getenv().getOrDefault("TEST_POSTGRES_USERNAME", "review_test");
        String password = System.getenv().getOrDefault("TEST_POSTGRES_PASSWORD", "");
        String schema = "review_" + UUID.randomUUID().toString().replace("-", "");
        var flyway =
                Flyway.configure()
                        .dataSource(url, username, password)
                        .schemas(schema)
                        .locations("classpath:db/migration")
                        .load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(11);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        String legacy = schema + "_legacy";
        try (var c = DriverManager.getConnection(url, username, password);
                var st = c.createStatement()) {
            st.execute("CREATE SCHEMA " + legacy);
            st.execute("SET search_path TO " + legacy);
            ScriptUtils.executeSqlScript(
                    c, new ClassPathResource("db/migration/V1__fresh_schema.sql"));
            st.execute(
                    "INSERT INTO equipment_categories(name,description) VALUES"
                            + " ('Retained','test')");
        }
        var upgrade =
                Flyway.configure()
                        .dataSource(url, username, password)
                        .schemas(legacy)
                        .baselineOnMigrate(true)
                        .baselineVersion("1")
                        .locations("classpath:db/migration")
                        .load();
        assertThat(upgrade.migrate().migrationsExecuted).isEqualTo(10);
        try (var c = DriverManager.getConnection(url, username, password);
                var st = c.createStatement();
                var rows =
                        st.executeQuery(
                                "SELECT count(*) FROM "
                                        + legacy
                                        + ".equipment_categories WHERE name='Retained'")) {
            rows.next();
            assertThat(rows.getInt(1)).isEqualTo(1);
        }
        try (var c = DriverManager.getConnection(url, username, password);
                var st = c.createStatement()) {
            st.execute("DROP SCHEMA " + schema + " CASCADE");
            st.execute("DROP SCHEMA " + legacy + " CASCADE");
        }
    }
}
