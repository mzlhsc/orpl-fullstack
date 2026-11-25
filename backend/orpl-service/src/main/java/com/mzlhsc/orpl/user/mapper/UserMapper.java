package com.mzlhsc.orpl.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mzlhsc.orpl.user.domain.dto.DateCountDTO;
import com.mzlhsc.orpl.user.domain.po.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户表（C 端用户）Mapper
 * 工作台/报表模块只做统计查询，暂不需要实体类
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据时间段统计新增用户数量
     *
     * @param map 查询条件（begin/end）
     * @return 用户数量
     */
    Integer countByMap(Map<String, Object> map);

    /**
     * 按日期统计每日新增用户数量
     *
     * @param begin 开始时间（含）
     * @param end   结束时间（不含）
     * @return 每日新增数量
     */
    List<DateCountDTO> countByDate(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);

}

