package com.mandi.service;

import com.mandi.dto.AlertRequest;
import com.mandi.entity.MandiPrice;
import com.mandi.entity.PriceAlert;
import com.mandi.exception.BadRequestException;
import com.mandi.repository.MandiPriceRepository;
import com.mandi.repository.PriceAlertRepository;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * v1: alerts are stored and evaluated against stored history after every sync.
 * There are no user accounts and no notification delivery yet (see README) - alerts show a status in the UI.
 */
@Service
public class AlertService {
    private final PriceAlertRepository alerts;
    private final MandiPriceRepository prices;
    private final FilterValidator validator;

    public AlertService(PriceAlertRepository alerts, MandiPriceRepository prices, FilterValidator validator) {
        this.alerts = alerts;
        this.prices = prices;
        this.validator = validator;
    }

    @Transactional
    public PriceAlert create(AlertRequest r) {
        PriceAlert a = new PriceAlert();
        a.setState(validator.requireState(r.state()));
        a.setDistrict(validator.require(r.district(), "district"));
        a.setCommodity(validator.require(r.commodity(), "commodity"));
        a.setMarket(validator.optional(r.market()));
        a.setDirection(r.direction());
        a.setThreshold(r.threshold());
        evaluate(a);
        return alerts.save(a);
    }

    @Transactional(readOnly = true)
    public List<PriceAlert> list() { return alerts.findAllByOrderByCreatedAtDesc(); }

    @Transactional
    public void delete(Long id) {
        if (!alerts.existsById(id)) throw new BadRequestException("Alert not found.");
        alerts.deleteById(id);
    }

    @Transactional
    public void evaluateAll() { alerts.findByActiveTrue().forEach(this::evaluate); }

    void evaluate(PriceAlert a) {
        List<MandiPrice> latest = prices.latestDay(a.getState(), a.getDistrict(), a.getCommodity()).stream()
                .filter(p -> a.getMarket() == null || p.getMarket().equalsIgnoreCase(a.getMarket())).toList();
        a.setLastCheckedAt(Instant.now());
        if (latest.isEmpty()) { a.setTriggered(false); return; }
        Comparator<MandiPrice> byModal = Comparator.comparing(MandiPrice::getModalPrice);
        boolean above = a.getDirection() == PriceAlert.Direction.ABOVE;
        MandiPrice best = above ? Collections.max(latest, byModal) : Collections.min(latest, byModal);
        a.setLastObservedPrice(best.getModalPrice());
        a.setLastObservedMarket(best.getMarket());
        a.setLastObservedDate(best.getArrivalDate());
        int cmp = best.getModalPrice().compareTo(a.getThreshold());
        a.setTriggered(above ? cmp > 0 : cmp < 0);
    }
}
