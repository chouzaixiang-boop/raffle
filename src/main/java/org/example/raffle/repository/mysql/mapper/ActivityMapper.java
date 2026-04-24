package org.example.raffle.repository.mysql.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.raffle.repository.mysql.po.ActivityRow;

import java.util.List;

@Mapper
public interface ActivityMapper {

    @Select("""
            select activity_id, activity_name, activity_desc, strategy_id, page_title, page_subtitle, banner_url, theme_color, sort_no, status
            from activity
            where activity_id = #{activityId}
            """)
    ActivityRow findByActivityId(@Param("activityId") Long activityId);

    @Select("""
            select activity_id, activity_name, activity_desc, strategy_id, page_title, page_subtitle, banner_url, theme_color, sort_no, status
            from activity
            where status = 1
            order by sort_no, id
            """)
    List<ActivityRow> findAllEnabled();
}