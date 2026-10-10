package com.example.itborrow.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI assetTrackOpenAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("AssetTrack IT API")
                                .description("API สำหรับระบบจัดการและติดตามการยืม–คืนอุปกรณ์ IT")
                                .version("1.0.0"));
    }
}
