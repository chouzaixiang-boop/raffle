package org.example.raffle.repository.mysql.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Options;
import org.example.raffle.repository.mysql.po.AwardReceivedRow;

@Mapper
public interface AwardReceivedMapper {

    @Insert("""
            insert into award_received(task_id, user_id, strategy_id, award_id, award_name, receive_status, receive_time, create_time)
            values(#{taskId}, #{userId}, #{strategyId}, #{awardId}, #{awardName}, #{receiveStatus}, #{receiveTime}, #{createTime})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "receivedId", keyColumn = "received_id")
    int insert(AwardReceivedRow row);

    @Select("""
            select received_id, task_id, user_id, strategy_id, award_id, award_name, receive_status, receive_time, create_time
            from award_received
            where task_id = #{taskId}
            """)
    AwardReceivedRow findByTaskId(@Param("taskId") Long taskId);
}
