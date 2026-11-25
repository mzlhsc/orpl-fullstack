package com.mzlhsc.orpl.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mzlhsc.orpl.order.domain.po.DiningSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface DiningSessionMapper extends BaseMapper<DiningSession> {

    @Select("""
            select count(*) from dining_session
            where status = 3 and checkout_time >= #{begin} and checkout_time < #{end}
            """)
    long countCheckedOut(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);

    @Select("""
            select coalesce(sum(total_amount), 0) from dining_session
            where status = 3 and checkout_time >= #{begin} and checkout_time < #{end}
            """)
    BigDecimal sumRevenue(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);

    @Select("""
            select coalesce(sum(guest_count), 0) from dining_session
            where status = 3 and checkout_time >= #{begin} and checkout_time < #{end}
            """)
    long sumGuests(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);

    @Select("""
            select date_format(checkout_time, '%Y-%m-%d') as day,
                   count(*) as session_count,
                   coalesce(sum(total_amount), 0) as revenue,
                   coalesce(sum(guest_count), 0) as guests
            from dining_session
            where status = 3 and checkout_time >= #{begin} and checkout_time < #{end}
            group by date_format(checkout_time, '%Y-%m-%d')
            order by day
            """)
    List<Map<String, Object>> dailyReport(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);
}
