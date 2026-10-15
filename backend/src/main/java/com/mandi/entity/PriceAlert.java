package com.mandi.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter @Setter @NoArgsConstructor
@Table(name = "price_alert")
public class PriceAlert {
    public enum Direction { ABOVE, BELOW }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String state;
    @Column(nullable = false) private String district;
    @Column(nullable = false) private String commodity;
    private String market;                       // null = any market
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private Direction direction;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal threshold;
    @Column(nullable = false) private boolean active = true;
    @Column(nullable = false) private boolean triggered = false;
    @Column(name = "last_observed_price", precision = 12, scale = 2) private BigDecimal lastObservedPrice;
    @Column(name = "last_observed_market") private String lastObservedMarket;
    @Column(name = "last_observed_date") private LocalDate lastObservedDate;
    @Column(name = "last_checked_at") private Instant lastCheckedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); }
}
