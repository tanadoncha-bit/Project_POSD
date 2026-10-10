package com.example.itborrow.service;

import static org.assertj.core.api.Assertions.*;

import com.example.itborrow.config.FeePolicyProperties;
import com.example.itborrow.config.PaginationProperties;
import com.example.itborrow.domain.entity.BorrowRequest;
import com.example.itborrow.service.strategy.*;

import jakarta.validation.Validation;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;

class FeePolicyConfigurationTest {
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({FeePolicyProperties.class, PaginationProperties.class})
    static class Binding {}

    @Test
    void springBindsConfiguredFeesAndPageSizes() {
        new ApplicationContextRunner()
                .withUserConfiguration(Binding.class)
                .withPropertyValues(
                        "app.fees.standard-daily-fine=91.50",
                        "app.fees.scratch-rate=0.12",
                        "app.pagination.catalog-size=8")
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            assertThat(
                                            context.getBean(FeePolicyProperties.class)
                                                    .getStandardDailyFine())
                                    .isEqualByComparingTo("91.50");
                            assertThat(context.getBean(FeePolicyProperties.class).getScratchRate())
                                    .isEqualByComparingTo("0.12");
                            assertThat(context.getBean(PaginationProperties.class).getCatalogSize())
                                    .isEqualTo(8);
                        });
    }

    @Test
    void legacyStrategiesUseConfiguredRatesAndGracePeriods() {
        var fees = new FeePolicyProperties();
        fees.setStandardDailyFine(new BigDecimal("80"));
        fees.setStandardGraceDays(1);
        fees.setVipDailyFine(new BigDecimal("25"));
        fees.setVipGraceDays(3);
        var request = new BorrowRequest();
        request.setDueDate(LocalDate.of(2026, 1, 1));
        assertThat(new StandardFineStrategy(fees).calculate(request, LocalDate.of(2026, 1, 6)))
                .isEqualByComparingTo("320");
        assertThat(new VipFineStrategy(fees).calculate(request, LocalDate.of(2026, 1, 6)))
                .isEqualByComparingTo("50");
    }

    @Test
    void configurationRejectsNegativeFeesInvalidRatiosAndUnboundedPageSizes() {
        try (var validator = Validation.buildDefaultValidatorFactory()) {
            var fees = new FeePolicyProperties();
            fees.setStandardDailyFine(new BigDecimal("-1"));
            fees.setScratchRate(new BigDecimal("1.2"));
            assertThat(validator.getValidator().validate(fees)).hasSize(2);
            var pages = new PaginationProperties();
            pages.setCatalogSize(0);
            pages.setUserSize(1001);
            assertThat(validator.getValidator().validate(pages)).hasSize(2);
        }
    }
}
