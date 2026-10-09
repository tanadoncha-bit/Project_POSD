package com.example.itborrow.config;

import jakarta.validation.constraints.*;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "app.pagination")
public class PaginationProperties {
    @Min(1)
    @Max(100)
    private int catalogSize = 12;

    public int getCatalogSize() {
        return catalogSize;
    }

    public void setCatalogSize(int value) {
        catalogSize = value;
    }

    @Min(1)
    @Max(100)
    private int requestSize = 10;

    public int getRequestSize() {
        return requestSize;
    }

    public void setRequestSize(int value) {
        requestSize = value;
    }

    @Min(1)
    @Max(100)
    private int managementSize = 10;

    public int getManagementSize() {
        return managementSize;
    }

    public void setManagementSize(int value) {
        managementSize = value;
    }

    @Min(1)
    @Max(100)
    private int userSize = 10;

    public int getUserSize() {
        return userSize;
    }

    public void setUserSize(int value) {
        userSize = value;
    }

    @Min(1)
    @Max(100)
    private int profileSize = 5;

    public int getProfileSize() {
        return profileSize;
    }

    public void setProfileSize(int value) {
        profileSize = value;
    }
}
