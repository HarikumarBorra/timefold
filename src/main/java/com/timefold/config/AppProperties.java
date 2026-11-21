package com.timefold.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String baseUrl,
        Encryption encryption,
        WhatsApp whatsapp
) {
    public record Encryption(
            int currentKeyVersion,
            String secretKeyBase64
    ) {}

    public record WhatsApp(
            boolean enabled,
            String apiBaseUrl,
            String phoneNumberId,
            String accessToken,
            String webhookVerifyToken,
            String appSecret
    ) {}
}