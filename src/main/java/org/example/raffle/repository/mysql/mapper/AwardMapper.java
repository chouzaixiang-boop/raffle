package org.example.raffle.repository.mysql.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.raffle.repository.mysql.po.AwardRow;

import java.util.List;

@Mapper
public interface AwardMapper {

    @Select("""
            select award_id, award_type, award_name, award_value, award_desc
            from award
            where award_id = #{awardId}
            """)
    AwardRow findByAwardId(@Param("awardId") Long awardId);

    @Select("""
            select award_id, award_type, award_name, award_value, award_desc
            from award
            order by id
            """)
    List<AwardRow> findAll();
}
