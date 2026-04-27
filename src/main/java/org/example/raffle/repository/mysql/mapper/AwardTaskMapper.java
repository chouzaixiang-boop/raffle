package org.example.raffle.repository.mysql.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Options;
import org.example.raffle.repository.mysql.po.AwardTaskRow;

import java.util.List;

@Mapper
public interface AwardTaskMapper {

    @Insert("""
            insert into award_task(user_id, strategy_id, award_id, award_name, task_status, version, retry_count, fail_reason, create_time, update_time)
            values(#{userId}, #{strategyId}, #{awardId}, #{awardName}, #{taskStatus}, #{version}, #{retryCount}, #{failReason}, #{createTime}, #{updateTime})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "taskId", keyColumn = "task_id")
    int insert(AwardTaskRow row);

    @Select("""
            select task_id, user_id, strategy_id, award_id, award_name, task_status, version, retry_count, fail_reason, create_time, update_time
            from award_task
            where task_id = #{taskId}
            """)
    AwardTaskRow findById(@Param("taskId") Long taskId);

    @Select("""
            <script>
            select task_id, user_id, strategy_id, award_id, award_name, task_status, version, retry_count, fail_reason, create_time, update_time
            from award_task
            where task_status in
            <foreach collection='statuses' item='status' open='(' separator=',' close=')'>
                #{status}
            </foreach>
              and update_time &lt; date_sub(now(), interval #{staleSeconds} second)
            order by update_time asc
            limit #{limit}
            </script>
            """)
    List<AwardTaskRow> findStaleTasks(@Param("statuses") List<String> statuses,
                                      @Param("staleSeconds") int staleSeconds,
                                      @Param("limit") int limit);

    @Update("""
            update award_task
            set task_status = #{newStatus}, version = version + 1, update_time = now()
            where task_id = #{taskId}
              and task_status = #{currentStatus}
              and version = #{currentVersion}
            """)
    int updateStatus(@Param("taskId") Long taskId,
                     @Param("currentStatus") String currentStatus,
                     @Param("newStatus") String newStatus,
                     @Param("currentVersion") int currentVersion);

                @Update("""
                                                update award_task
                                                set task_status = 'FAILED',
                                                                fail_reason = #{failReason},
                                                                retry_count = retry_count + 1,
                                                                version = version + 1,
                                                                update_time = now()
                                                where task_id = #{taskId}
                                                        and task_status = 'PROCESSING'
                                                        and version = #{currentVersion}
                                                """)
                int markFailed(@Param("taskId") Long taskId,
                                                                         @Param("currentVersion") int currentVersion,
                                                                         @Param("failReason") String failReason);
}
