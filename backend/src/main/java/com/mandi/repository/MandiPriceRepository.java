package com.mandi.repository;

import com.mandi.entity.MandiPrice;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MandiPriceRepository extends JpaRepository<MandiPrice, Long> {

    interface VariantCount { String getVariety(); String getGrade(); Long getCnt(); }
    interface MarketCountView { String getMarket(); Long getCnt(); }
    interface CoverageView { LocalDate getFirstDate(); LocalDate getLastDate(); }

    Optional<MandiPrice> findByStateAndDistrictAndMarketAndCommodityAndVarietyAndGradeAndArrivalDate(
            String state, String district, String market, String commodity,
            String variety, String grade, LocalDate arrivalDate);

    /** Variety/grade combinations (never merged), most frequently reported first. */
    @Query("select p.variety as variety, p.grade as grade, count(p) as cnt from MandiPrice p " +
            "where p.state=:state and p.district=:district and p.commodity=:commodity " +
            "and p.arrivalDate between :from and :to group by p.variety, p.grade order by count(p) desc")
    List<VariantCount> variantCounts(@Param("state") String state, @Param("district") String district,
                                     @Param("commodity") String commodity,
                                     @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select p.market as market, count(p) as cnt from MandiPrice p " +
            "where p.state=:state and p.district=:district and p.commodity=:commodity " +
            "and p.variety=:variety and p.grade=:grade and p.arrivalDate between :from and :to " +
            "group by p.market order by count(p) desc, p.market asc")
    List<MarketCountView> marketCounts(@Param("state") String state, @Param("district") String district,
                                       @Param("commodity") String commodity, @Param("variety") String variety,
                                       @Param("grade") String grade,
                                       @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select p from MandiPrice p " +
            "where p.state=:state and p.district=:district and p.commodity=:commodity " +
            "and p.variety=:variety and p.grade=:grade and p.arrivalDate between :from and :to " +
            "and p.market in :markets order by p.arrivalDate asc, p.market asc")
    List<MandiPrice> findSeries(@Param("state") String state, @Param("district") String district,
                                @Param("commodity") String commodity, @Param("variety") String variety,
                                @Param("grade") String grade,
                                @Param("from") LocalDate from, @Param("to") LocalDate to,
                                @Param("markets") Collection<String> markets);

    /** Earliest/latest stored date for a selection (used to decide if a period is really covered). */
    @Query("select min(p.arrivalDate) as firstDate, max(p.arrivalDate) as lastDate from MandiPrice p " +
            "where p.state=:state and p.district=:district and p.commodity=:commodity " +
            "and p.variety=:variety and p.grade=:grade and p.arrivalDate between :from and :to")
    CoverageView coverage(@Param("state") String state, @Param("district") String district,
                          @Param("commodity") String commodity, @Param("variety") String variety,
                          @Param("grade") String grade,
                          @Param("from") LocalDate from, @Param("to") LocalDate to);

    /** All rows on the most recent stored day for state+district+commodity (used by alerts). */
    @Query("select p from MandiPrice p where p.state=:state and p.district=:district and p.commodity=:commodity " +
            "and p.arrivalDate = (select max(q.arrivalDate) from MandiPrice q " +
            "where q.state=:state and q.district=:district and q.commodity=:commodity)")
    List<MandiPrice> latestDay(@Param("state") String state, @Param("district") String district,
                               @Param("commodity") String commodity);
}
