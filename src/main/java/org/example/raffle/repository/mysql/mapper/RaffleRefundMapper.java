package org.example.raffle.repository.mysql.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.raffle.repository.mysql.po.RaffleRefundRow;

@Mapper
public interface RaffleRefundMapper {

    @Select("""
            select id, refund_id, task_id, user_id, strategy_id, award_id, refund_status, refund_message, create_time
            from raffle_refund
            where refund_id = #{refundId}
            """)
    RaffleRefundRow findByRefundId(@Param("refundId") String refundId);

    @Select("""
            select id, refund_id, task_id, user_id, strategy_id, award_id, refund_status, refund_message, create_time
            from raffle_refund
            where task_id = #{taskId}
            """)
    RaffleRefundRow findByTaskId(@Param("taskId") Long taskId);

    @Insert("""
            insert into raffle_refund(refund_id, task_id, user_id, strategy_id, award_id, refund_status, refund_message, create_time)
            values(#{refundId}, #{taskId}, #{userId}, #{strategyId}, #{awardId}, #{refundStatus}, #{refundMessage}, #{createTime})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(RaffleRefundRow row);
}
