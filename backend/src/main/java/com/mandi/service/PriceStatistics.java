package com.mandi.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** Pure calculations over REAL points only. Missing days are never filled in. */
public final class PriceStatistics {
    private PriceStatistics() {}

    public enum Trend { RISING, FALLING, STABLE, INSUFFICIENT_DATA }

    public record Point(LocalDate date, BigDecimal price) {}

    public record Result(int points, BigDecimal current, LocalDate currentDate, BigDecimal high, BigDecimal low,
                         BigDecimal average, BigDecimal changePercent, Trend trend) {}

    public static Result compute(List<Point> pts, double thresholdPercent, int minPoints) {
        if (pts == null || pts.isEmpty()) return null;
        List<Point> s = pts.stream().sorted(Comparator.comparing(Point::date)).toList();
        Point first = s.get(0), last = s.get(s.size() - 1);
        BigDecimal high = s.stream().map(Point::price).max(Comparator.naturalOrder()).orElseThrow();
        BigDecimal low = s.stream().map(Point::price).min(Comparator.naturalOrder()).orElseThrow();
        BigDecimal sum = s.stream().map(Point::price).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avg = sum.divide(BigDecimal.valueOf(s.size()), 2, RoundingMode.HALF_UP);
        BigDecimal change = null;
        if (s.size() >= 2 && first.price().signum() > 0) {
            change = last.price().subtract(first.price()).multiply(BigDecimal.valueOf(100))
                    .divide(first.price(), 1, RoundingMode.HALF_UP);
        }
        return new Result(s.size(), last.price(), last.date(), high, low, avg, change,
                trend(s, thresholdPercent, minPoints));
    }

    /** Recent-half average vs earlier-half average. A historical indicator, NOT a forecast. */
    static Trend trend(List<Point> s, double thresholdPercent, int minPoints) {
        int n = s.size();
        if (n < Math.max(minPoints, 2)) return Trend.INSUFFICIENT_DATA;
        int mid = n / 2;
        double earlier = avg(s.subList(0, mid)), recent = avg(s.subList(mid, n));
        if (earlier <= 0) return Trend.INSUFFICIENT_DATA;
        double diff = (recent - earlier) / earlier * 100.0;
        if (diff > thresholdPercent) return Trend.RISING;
        if (diff < -thresholdPercent) return Trend.FALLING;
        return Trend.STABLE;
    }

    private static double avg(List<Point> p) {
        return p.stream().mapToDouble(x -> x.price().doubleValue()).average().orElse(0);
    }
}
