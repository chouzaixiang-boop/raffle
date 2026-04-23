package org.example.raffle.repository.mysql.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.raffle.repository.mysql.po.RuleRow;

import java.util.List;

@Mapper
public interface RuleMapper {

    @Select("""
            select rule_model
            from strategy_rule
            where strategy_id = #{strategyId}
              and award_id is null
            order by id
            """)
    List<String> findRuleModelsByStrategyId(@Param("strategyId") Long strategyId);

    @Select("""
            select rule_value
            from strategy_rule
            where strategy_id = #{strategyId}
              and award_id is null
              and rule_model = #{ruleModel}
            limit 1
            """)
    String findRuleValueForStrategy(@Param("strategyId") Long strategyId, @Param("ruleModel") String ruleModel);

    @Select("""
            select rule_value
            from strategy_rule
            where strategy_id = #{strategyId}
              and award_id = #{awardId}
              and rule_model = #{ruleModel}
            limit 1
            """)
    String findRuleValueForAward(@Param("strategyId") Long strategyId,
                                 @Param("awardId") Long awardId,
                                 @Param("ruleModel") String ruleModel);

        @Select("""
          select strategy_id, award_id, rule_model, rule_value, rule_desc
          from strategy_rule
          order by strategy_id, ifnull(award_id, 0), id
          """)
        List<RuleRow> findAll();
}
