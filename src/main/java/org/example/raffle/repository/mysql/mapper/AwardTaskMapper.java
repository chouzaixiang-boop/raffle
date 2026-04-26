package org.example.raffle.repository.mysql.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Options;
import org.example.raffle.repository.mysql.po.AwardTaskRow;

@Mapper
public interface AwardTaskMapper {

    @Insert("""
            insert into award_task(user_id, strategy_id, award_id, award_name, task_status, version, create_time)
            values(#{userId}, #{strategyId}, #{awardId}, #{awardName}, #{taskStatus}, #{version}, #{createTime})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "taskId", keyColumn = "task_id")
    int insert(AwardTaskRow row);

    @Select("""
            select task_id, user_id, strategy_id, award_id, award_name, task_status, version, create_time
            from award_task
            where task_id = #{taskId}
            """)
    AwardTaskRow findById(@Param("taskId") Long taskId);

    @Update("""
            update award_task
            set task_status = #{newStatus}, version = version + 1
            where task_id = #{taskId}
              and task_status = #{currentStatus}
              and version = #{currentVersion}
            """)
    int updateStatus(@Param("taskId") Long taskId,
                     @Param("currentStatus") String currentStatus,
                     @Param("newStatus") String newStatus,
                     @Param("currentVersion") int currentVersion);
}
