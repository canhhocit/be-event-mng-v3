package com.sa.event_mng.shared.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.payment.deep-link")
@Data
class PaymentDeepLinkProperties {
    private String scheme;
    private String host;
    private String path;
}

@Configuration
@ConfigurationProperties(prefix = "cloudinary")
@Data
class CloudinaryProperties {
    private String cloudName;
    private String apiKey;
    private String apiSecret;
}
