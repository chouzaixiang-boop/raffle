package org.example.raffle.repository.mysql.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.raffle.repository.mysql.po.StrategyRow;

import java.util.List;

@Mapper
public interface StrategyMapper {

    @Select("""
            select strategy_id, strategy_desc, rule_models
            from strategy
            where strategy_id = #{strategyId}
            """)
    StrategyRow findByStrategyId(@Param("strategyId") Long strategyId);

    @Select("""
            select strategy_id, strategy_desc, rule_models
            from strategy
            order by id
            """)
    List<StrategyRow> findAll();
}
