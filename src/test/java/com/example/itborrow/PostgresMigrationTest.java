package com.example.itborrow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.flywaydb.core.Flyway;
import static org.assertj.core.api.Assertions.*;
@EnabledIfEnvironmentVariable(named="TEST_POSTGRES_URL", matches="jdbc:postgresql://127\\.0\\.0\\.1:55439/.*")
class PostgresMigrationTest {
 @Test void freshAndExistingSchemasMigrateWithoutDeletingData() throws Exception {
  String url=System.getenv("TEST_POSTGRES_URL");
  String schema="review_"+java.util.UUID.randomUUID().toString().replace("-","");
  var flyway=Flyway.configure().dataSource(url,"review_test","").schemas(schema).locations("classpath:db/migration").load();
  assertThat(flyway.migrate().migrationsExecuted).isEqualTo(5);
  assertThat(flyway.migrate().migrationsExecuted).isZero();
  String legacy=schema+"_legacy";
  try(var c=java.sql.DriverManager.getConnection(url,"review_test","");var st=c.createStatement()) {
   st.execute("CREATE SCHEMA "+legacy);st.execute("SET search_path TO "+legacy);
   org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(c,new org.springframework.core.io.ClassPathResource("db/migration/V1__fresh_schema.sql"));
   st.execute("INSERT INTO equipment_categories(name,description) VALUES ('Retained','test')");
  }
  var upgrade=Flyway.configure().dataSource(url,"review_test","").schemas(legacy).baselineOnMigrate(true).baselineVersion("1").locations("classpath:db/migration").load();
  assertThat(upgrade.migrate().migrationsExecuted).isEqualTo(4);
  try(var c=java.sql.DriverManager.getConnection(url,"review_test","");var st=c.createStatement();var rows=st.executeQuery("SELECT count(*) FROM "+legacy+".equipment_categories WHERE name='Retained'")) { rows.next();assertThat(rows.getInt(1)).isEqualTo(1); }
 }
}
