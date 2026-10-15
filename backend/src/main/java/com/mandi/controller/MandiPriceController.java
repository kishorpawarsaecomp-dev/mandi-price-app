package com.mandi.controller;

import com.mandi.dto.ComparisonResponse;
import com.mandi.dto.MandiPriceResponse;
import com.mandi.dto.OptionsResponse;
import com.mandi.service.FilterValidator;
import com.mandi.service.MandiPriceService;
import com.mandi.service.MarketComparisonService;
import com.mandi.service.OptionsService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mandi")
public class MandiPriceController {
    private final MandiPriceService prices;
    private final OptionsService options;
    private final MarketComparisonService comparison;
    private final FilterValidator v;

    public MandiPriceController(MandiPriceService prices, OptionsService options,
                                MarketComparisonService comparison, FilterValidator v) {
        this.prices = prices; this.options = options; this.comparison = comparison; this.v = v;
    }

    @GetMapping("/states")
    public List<String> states() { return v.states(); }

    @GetMapping("/options")
    public OptionsResponse options(@RequestParam String state, @RequestParam(required = false) String district,
                                   @RequestParam(required = false) String commodity) {
        return options.discover(v.requireState(state), v.optional(district), v.optional(commodity));
    }

    @GetMapping("/prices")
    public MandiPriceResponse prices(@RequestParam String state,
                                     @RequestParam(required = false) String district,
                                     @RequestParam(required = false) String market,
                                     @RequestParam(required = false) String commodity,
                                     @RequestParam(required = false) String variety,
                                     @RequestParam(required = false) String grade,
                                     @RequestParam(required = false) Integer limit,
                                     @RequestParam(required = false) Integer offset) {
        return prices.getPrices(v.priceQuery(state, district, market, commodity, variety, grade, limit, offset));
    }

    @GetMapping("/compare")
    public ComparisonResponse compare(@RequestParam String state, @RequestParam String district,
                                      @RequestParam String commodity,
                                      @RequestParam(required = false) String variety,
                                      @RequestParam(required = false) String grade) {
        return comparison.compare(v.requireState(state), v.require(district, "district"),
                v.require(commodity, "commodity"), v.variantPart(variety), v.variantPart(grade));
    }
}
