package com.mandi.repository;

import com.mandi.entity.PriceAlert;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {
    List<PriceAlert> findByActiveTrue();
    List<PriceAlert> findAllByOrderByCreatedAtDesc();
}
