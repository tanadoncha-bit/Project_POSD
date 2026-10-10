package com.example.itborrow.config;

import com.example.itborrow.domain.enums.ReturnCondition;
import com.example.itborrow.domain.enums.Role;

import jakarta.validation.constraints.*;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Component
@Validated
@ConfigurationProperties(prefix = "app.fees")
public class FeePolicyProperties {
    @NotNull
    @DecimalMin("0.00")
    private BigDecimal standardDailyFine = new BigDecimal("50.00");

    public BigDecimal getStandardDailyFine() {
        return standardDailyFine;
    }

    public void setStandardDailyFine(BigDecimal value) {
        standardDailyFine = value;
    }

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal vipDailyFine = new BigDecimal("30.00");

    public BigDecimal getVipDailyFine() {
        return vipDailyFine;
    }

    public void setVipDailyFine(BigDecimal value) {
        vipDailyFine = value;
    }

    @Min(0)
    private int standardGraceDays = 0;

    public int getStandardGraceDays() {
        return standardGraceDays;
    }

    public void setStandardGraceDays(int value) {
        standardGraceDays = value;
    }

    @Min(0)
    private int vipGraceDays = 2;

    public int getVipGraceDays() {
        return vipGraceDays;
    }

    public void setVipGraceDays(int value) {
        vipGraceDays = value;
    }

    @NotNull
    @DecimalMin("0")
    @DecimalMax("1")
    private BigDecimal scratchRate = new BigDecimal("0.20");

    public BigDecimal getScratchRate() {
        return scratchRate;
    }

    public void setScratchRate(BigDecimal value) {
        scratchRate = value;
    }

    @NotNull
    @DecimalMin("0")
    @DecimalMax("1")
    private BigDecimal damageRate = new BigDecimal("0.50");

    public BigDecimal getDamageRate() {
        return damageRate;
    }

    public void setDamageRate(BigDecimal value) {
        damageRate = value;
    }

    @NotNull
    @DecimalMin("0")
    @DecimalMax("1")
    private BigDecimal lossRate = BigDecimal.ONE;

    public BigDecimal getLossRate() {
        return lossRate;
    }

    public void setLossRate(BigDecimal value) {
        lossRate = value;
    }

    public BigDecimal dailyFine(Role role) {
        return role == Role.VIP ? vipDailyFine : standardDailyFine;
    }

    public int graceDays(Role role) {
        return role == Role.VIP ? vipGraceDays : standardGraceDays;
    }

    public BigDecimal damageRate(ReturnCondition condition) {
        return switch (condition) {
            case NORMAL -> BigDecimal.ZERO;
            case MINOR_SCRATCHES -> scratchRate;
            case DAMAGED -> damageRate;
            case LOST -> lossRate;
        };
    }
}
