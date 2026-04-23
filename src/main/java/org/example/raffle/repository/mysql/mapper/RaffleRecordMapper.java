package org.example.raffle.repository.mysql.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.raffle.repository.mysql.po.RaffleRecordRow;

import java.util.List;

@Mapper
public interface RaffleRecordMapper {

    @Insert("""
            insert into raffle_record(user_id, strategy_id, award_id, award_name, success, message, create_time)
            values(#{userId}, #{strategyId}, #{awardId}, #{awardName}, #{success}, #{message}, #{createTime})
            """)
    int insert(RaffleRecordRow row);

    @Select("""
            select user_id, strategy_id, award_id, award_name, success, message, create_time
            from raffle_record
            order by id desc
            """)
    List<RaffleRecordRow> findAll();
}
