package com.example.itborrow.service;
import com.example.itborrow.config.FeePolicyProperties;
import com.example.itborrow.config.PaginationProperties;
import com.example.itborrow.domain.entity.BorrowRequest;
import com.example.itborrow.service.impl.strategy.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;
class FeePolicyConfigurationTest {
    @org.springframework.context.annotation.Configuration(proxyBeanMethods=false)
    @org.springframework.boot.context.properties.EnableConfigurationProperties({FeePolicyProperties.class,PaginationProperties.class})
    static class Binding {}
    @Test void springBindsConfiguredFeesAndPageSizes() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner().withUserConfiguration(Binding.class)
            .withPropertyValues("app.fees.standard-daily-fine=91.50","app.fees.scratch-rate=0.12","app.pagination.catalog-size=8")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getBean(FeePolicyProperties.class).getStandardDailyFine()).isEqualByComparingTo("91.50");
                assertThat(context.getBean(FeePolicyProperties.class).getScratchRate()).isEqualByComparingTo("0.12");
                assertThat(context.getBean(PaginationProperties.class).getCatalogSize()).isEqualTo(8);
            });
    }
    @Test void legacyStrategiesUseConfiguredRatesAndGracePeriods() {
        var fees=new FeePolicyProperties();fees.setStandardDailyFine(new BigDecimal("80"));fees.setStandardGraceDays(1);
        fees.setVipDailyFine(new BigDecimal("25"));fees.setVipGraceDays(3);
        var request=new BorrowRequest();request.setDueDate(LocalDate.of(2026,1,1));
        assertThat(new StandardFineStrategy(fees).calculate(request,LocalDate.of(2026,1,6))).isEqualByComparingTo("320");
        assertThat(new VipFineStrategy(fees).calculate(request,LocalDate.of(2026,1,6))).isEqualByComparingTo("50");
    }
    @Test void configurationRejectsNegativeFeesInvalidRatiosAndUnboundedPageSizes() {
        try(var validator=jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var fees=new FeePolicyProperties();fees.setStandardDailyFine(new BigDecimal("-1"));fees.setScratchRate(new BigDecimal("1.2"));
            assertThat(validator.getValidator().validate(fees)).hasSize(2);
            var pages=new PaginationProperties();pages.setCatalogSize(0);pages.setUserSize(1001);
            assertThat(validator.getValidator().validate(pages)).hasSize(2);
        }
    }
}
