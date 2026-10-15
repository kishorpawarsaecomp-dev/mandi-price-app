package com.mandi.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mandi")
public record MandiProperties(Api api, List<String> states, Sync sync, Trend trend,
                              Discovery discovery, Cache cache, List<String> corsOrigins) {

    public record Api(String key, String baseUrl, String resourceId,
                      int connectTimeoutMs, int readTimeoutMs, int pageSize) {}

    public record Sync(String cron, String zone, List<String> states, int maxPagesPerState,
                       boolean runOnStartup, String adminToken) {}

    public record Trend(double thresholdPercent, int minPoints) {}

    public record Discovery(int maxRecords) {}

    public record Cache(int pricesTtlMinutes, int optionsTtlMinutes) {}
}
