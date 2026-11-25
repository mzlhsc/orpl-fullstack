package com.mzlhsc.orpl.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mzlhsc.orpl.order.domain.dto.DateAmountDTO;
import com.mzlhsc.orpl.order.domain.dto.GoodsSalesDTO;
import com.mzlhsc.orpl.order.domain.dto.OrdersPageQueryDTO;
import com.mzlhsc.orpl.order.domain.po.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 订单表 Mapper 接口
 * </p>
 *
 * @author mzlhsc
 * @since 2026-11-09
 */
@Mapper
public interface OrdersMapper extends BaseMapper<Orders> {



    /**
     * 分页条件查询并按下单时间排序
     * 由 MyBatis-Plus 的 PaginationInnerInterceptor 负责物理分页
     * @param page 分页参数
     * @param ordersPageQueryDTO 查询条件
     */
    List<Orders> pageQuery(Page<Orders> page, @Param("dto") OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据状态统计订单数量
     * @param status
     */
    @Select("select count(id) from orders where status = #{status}")
    Integer countStatus(@Param("status") Integer status);

    /**
     * 根据时间段和状态统计订单数量（工作台/报表使用）
     * @param map 参数：begin、end、status
     */
    Integer countByMap(Map<String, Object> map);

    /**
     * 根据时间段和状态统计订单金额合计（营业额）
     * @param map 参数：begin、end、status
     */
    BigDecimal sumByMap(Map<String, Object> map);

    /**
     * 按日统计营业额（只统计已完成订单）
     * @param begin 起始时间（含）
     * @param end 截止时间（不含）
     */
    List<DateAmountDTO> sumAmountByDate(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);

    /**
     * 统计指定时间区间内的销量排名前 10
     * @param begin
     * @param end
     */
    List<GoodsSalesDTO> getSalesTop10(@Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);


}

