package com.mandi.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.mandi.service.PriceStatistics.Point;
import com.mandi.service.PriceStatistics.Trend;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PriceStatisticsTest {
    static List<Point> pts(int... prices) {
        List<Point> l = new ArrayList<>();
        for (int i = 0; i < prices.length; i++) l.add(new Point(LocalDate.of(2026, 10, 1).plusDays(i), BigDecimal.valueOf(prices[i])));
        return l;
    }

    @Test
    void computesHighLowAverageAndChange() {
        var r = PriceStatistics.compute(pts(100, 110, 120, 130), 3.0, 4);
        assertThat(r.current()).isEqualByComparingTo("130");
        assertThat(r.high()).isEqualByComparingTo("130");
        assertThat(r.low()).isEqualByComparingTo("100");
        assertThat(r.average()).isEqualByComparingTo("115.00");
        assertThat(r.changePercent()).isEqualByComparingTo("30.0");
        assertThat(r.trend()).isEqualTo(Trend.RISING);
    }

    @Test
    void fallingAndStable() {
        assertThat(PriceStatistics.compute(pts(130, 120, 110, 100), 3.0, 4).trend()).isEqualTo(Trend.FALLING);
        assertThat(PriceStatistics.compute(pts(100, 101, 100, 101), 3.0, 4).trend()).isEqualTo(Trend.STABLE);
    }

    @Test
    void tooFewPointsGivesNoTrend() {
        assertThat(PriceStatistics.compute(pts(100, 200), 3.0, 4).trend()).isEqualTo(Trend.INSUFFICIENT_DATA);
    }

    @Test
    void emptyInputGivesNull() {
        assertThat(PriceStatistics.compute(List.of(), 3.0, 4)).isNull();
    }

    @Test
    void gapsAreNotFilled() {
        // Oct 1, 2, 4, 5 : no Oct 3 point is invented
        var l = List.of(new Point(LocalDate.of(2026, 10, 1), BigDecimal.TEN), new Point(LocalDate.of(2026, 10, 2), BigDecimal.TEN),
                new Point(LocalDate.of(2026, 10, 4), BigDecimal.TEN), new Point(LocalDate.of(2026, 10, 5), BigDecimal.TEN));
        assertThat(PriceStatistics.compute(l, 3.0, 4).points()).isEqualTo(4);
    }
}
