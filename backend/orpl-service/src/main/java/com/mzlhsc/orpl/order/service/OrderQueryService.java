package com.mzlhsc.orpl.order.service;

import com.mzlhsc.orpl.order.domain.dto.DateAmountDTO;
import com.mzlhsc.orpl.order.domain.dto.GoodsSalesDTO;
import com.mzlhsc.orpl.order.domain.po.Orders;
import com.mzlhsc.orpl.order.domain.vo.OrderOverViewVO;
import com.mzlhsc.orpl.order.mapper.OrdersMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单统计查询服务。
 *
 * <p>单体架构下报表、工作台直接进程内调用该服务，不再经过 /inner HTTP 接口，
 * 也不存在 Feign 契约层。</p>
 */
@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrdersMapper ordersMapper;

    /**
     * 订单总览：各状态订单数量
     */
    public OrderOverViewVO getOverView() {
        return OrderOverViewVO.builder()
                .allOrders(count(buildQuery(null, null, null)))
                .waitingOrders(ordersMapper.countStatus(Orders.TO_BE_CONFIRMED))
                .deliveredOrders(ordersMapper.countStatus(Orders.CONFIRMED))
                .completedOrders(ordersMapper.countStatus(Orders.COMPLETED))
                .cancelledOrders(ordersMapper.countStatus(Orders.CANCELLED))
                .build();
    }

    /**
     * 按日期统计每日营业额（只统计已完成订单）
     */
    public List<DateAmountDTO> listTurnover(LocalDate begin, LocalDate end) {
        return ordersMapper.sumAmountByDate(beginTime(begin), endTime(end));
    }

    /**
     * 统计区间内某状态的订单数量，status 不传表示全部
     */
    public Long countOrders(LocalDate begin, LocalDate end, Integer status) {
        return Long.valueOf(count(buildQuery(begin, end, status)));
    }

    /**
     * 统计区间内某状态的订单金额合计
     */
    public BigDecimal sumOrders(LocalDate begin, LocalDate end, Integer status) {
        BigDecimal sum = ordersMapper.sumByMap(buildQuery(begin, end, status));
        return sum == null ? BigDecimal.ZERO : sum;
    }

    /**
     * 销量排名 top10
     */
    public List<GoodsSalesDTO> listSalesTop10(LocalDate begin, LocalDate end) {
        return ordersMapper.getSalesTop10(beginTime(begin), endTime(end));
    }

    private Integer count(Map<String, Object> query) {
        Integer count = ordersMapper.countByMap(query);
        return count == null ? 0 : count;
    }

    private Map<String, Object> buildQuery(LocalDate begin, LocalDate end, Integer status) {
        Map<String, Object> map = new HashMap<>(4);
        map.put("begin", beginTime(begin));
        map.put("end", endTime(end));
        map.put("status", status);
        return map;
    }

    private LocalDateTime beginTime(LocalDate begin) {
        return begin == null ? null : begin.atStartOfDay();
    }

    private LocalDateTime endTime(LocalDate end) {
        return end == null ? null : end.plusDays(1).atStartOfDay();
    }
}
