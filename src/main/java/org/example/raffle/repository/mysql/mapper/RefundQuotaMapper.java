package org.example.raffle.repository.mysql.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.raffle.repository.mysql.po.RefundQuotaRow;

@Mapper
public interface RefundQuotaMapper {

    @Insert("""
            insert ignore into user_refund_quota(user_id, strategy_id, used_count, max_count, version, create_time, update_time)
            values(#{userId}, #{strategyId}, 0, #{maxCount}, 0, now(), now())
            """)
    int insertIgnore(@Param("userId") Long userId,
                     @Param("strategyId") Long strategyId,
                     @Param("maxCount") int maxCount);

    @Select("""
            select user_id, strategy_id, used_count, max_count, version
            from user_refund_quota
            where user_id = #{userId} and strategy_id = #{strategyId}
            """)
    RefundQuotaRow findByUserAndStrategy(@Param("userId") Long userId,
                                         @Param("strategyId") Long strategyId);

    @Update("""
            update user_refund_quota
            set used_count = used_count + 1,
                version = version + 1,
                update_time = now()
            where user_id = #{userId}
              and strategy_id = #{strategyId}
              and version = #{currentVersion}
              and used_count < max_count
            """)
    int incrementUsedWithVersion(@Param("userId") Long userId,
                                 @Param("strategyId") Long strategyId,
                                 @Param("currentVersion") int currentVersion);
}
