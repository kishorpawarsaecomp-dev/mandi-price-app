package com.mandi.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableCaching
@EnableScheduling
public class AppConfig implements WebMvcConfigurer {

    private final MandiProperties props;

    public AppConfig(MandiProperties props) { this.props = props; }

    /** HTTP client used ONLY by the backend to call data.gov.in, with hard timeouts. */
    @Bean
    RestClient dataGovRestClient() {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(props.api().connectTimeoutMs())).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofMillis(props.api().readTimeoutMs()));
        return RestClient.builder().requestFactory(factory).defaultHeader("Accept", "application/json").build();
    }

    /** Simple in-process cache (no Redis needed for v1). Same filters => one upstream call per TTL. */
    @Bean
    CacheManager cacheManager() {
        CaffeineCacheManager m = new CaffeineCacheManager();
        Duration prices = Duration.ofMinutes(props.cache().pricesTtlMinutes());
        Duration options = Duration.ofMinutes(props.cache().optionsTtlMinutes());
        m.registerCustomCache("prices", Caffeine.newBuilder().expireAfterWrite(prices).maximumSize(1000).build());
        m.registerCustomCache("compare", Caffeine.newBuilder().expireAfterWrite(prices).maximumSize(500).build());
        m.registerCustomCache("options", Caffeine.newBuilder().expireAfterWrite(options).maximumSize(500).build());
        return m;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(props.corsOrigins().toArray(new String[0]))
                .allowedMethods("GET", "POST", "DELETE");
    }
}
