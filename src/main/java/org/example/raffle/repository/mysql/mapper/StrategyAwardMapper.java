package org.example.raffle.repository.mysql.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.raffle.repository.mysql.po.StockDeltaRow;
import org.example.raffle.repository.mysql.po.StrategyAwardRow;

import java.util.List;

@Mapper
public interface StrategyAwardMapper {

    @Select("""
            select strategy_id, award_id, award_title, rule_models, award_allocate, award_surplus, award_rate, award_index
            from strategy_award
            where strategy_id = #{strategyId}
            order by award_index
            """)
    List<StrategyAwardRow> findByStrategyId(@Param("strategyId") Long strategyId);

    @Select("""
            select strategy_id, award_id, award_title, rule_models, award_allocate, award_surplus, award_rate, award_index
            from strategy_award
            order by strategy_id, award_index
            """)
    List<StrategyAwardRow> findAll();

    @Select("""
            select strategy_id, award_id, award_title, rule_models, award_allocate, award_surplus, award_rate, award_index
            from strategy_award
            where strategy_id = #{strategyId} and award_id = #{awardId}
            """)
    StrategyAwardRow findByStrategyIdAndAwardId(@Param("strategyId") Long strategyId, @Param("awardId") Long awardId);

    @Update("""
            update strategy_award
            set award_surplus = #{surplus}
            where strategy_id = #{strategyId} and award_id = #{awardId}
            """)
    int updateSurplus(@Param("strategyId") Long strategyId, @Param("awardId") Long awardId, @Param("surplus") int surplus);

    @Update("""
            update strategy_award
            set award_surplus = least(award_surplus + #{increaseCount}, award_allocate)
            where strategy_id = #{strategyId} and award_id = #{awardId}
            """)
    int increaseSurplusWithCap(@Param("strategyId") Long strategyId,
                               @Param("awardId") Long awardId,
                               @Param("increaseCount") int increaseCount);

        @Update("""
                        <script>
                        update strategy_award
                        set award_surplus = case
                        <foreach collection='items' item='item'>
                                when strategy_id = #{item.strategyId} and award_id = #{item.awardId}
                                then greatest(award_surplus - #{item.decreaseCount}, 0)
                        </foreach>
                        else award_surplus
                        end
                        where (strategy_id, award_id) in
                        <foreach collection='items' item='item' open='(' separator=',' close=')'>
                                (#{item.strategyId}, #{item.awardId})
                        </foreach>
                        </script>
                        """)
        int batchDecreaseSurplus(@Param("items") List<StockDeltaRow> items);
}
