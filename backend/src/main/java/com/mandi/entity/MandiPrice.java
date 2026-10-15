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
@Table(name = "mandi_price_history",
        uniqueConstraints = @UniqueConstraint(name = "uq_mandi_price_natural_key",
                columnNames = {"state", "district", "market", "commodity", "variety", "grade", "arrival_date"}),
        indexes = {
                @Index(name = "idx_mph_lookup", columnList = "state,district,commodity,arrival_date"),
                @Index(name = "idx_mph_series", columnList = "state,district,commodity,market,arrival_date"),
                @Index(name = "idx_mph_date", columnList = "arrival_date")})
public class MandiPrice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100) private String state;
    @Column(nullable = false, length = 150) private String district;
    @Column(nullable = false, length = 200) private String market;
    @Column(nullable = false, length = 200) private String commodity;
    @Column(nullable = false, length = 200) private String variety = "";
    @Column(nullable = false, length = 100) private String grade = "";
    @Column(name = "arrival_date", nullable = false) private LocalDate arrivalDate;
    @Column(name = "min_price", precision = 12, scale = 2) private BigDecimal minPrice;
    @Column(name = "max_price", precision = 12, scale = 2) private BigDecimal maxPrice;
    @Column(name = "modal_price", nullable = false, precision = 12, scale = 2) private BigDecimal modalPrice;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
}
