package com.mzlhsc.orpl.order.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.mzlhsc.orpl.common.result.PageDTO;
import com.mzlhsc.orpl.order.domain.dto.OrdersCancelDTO;
import com.mzlhsc.orpl.order.domain.dto.OrdersPageQueryDTO;
import com.mzlhsc.orpl.order.domain.dto.OrdersRejectionDTO;
import com.mzlhsc.orpl.order.domain.dto.OrdersSubmitDTO;
import com.mzlhsc.orpl.order.domain.po.Orders;
import com.mzlhsc.orpl.order.domain.vo.OrderStatisticsVO;
import com.mzlhsc.orpl.order.domain.vo.OrderSubmitVO;
import com.mzlhsc.orpl.order.domain.vo.OrderVO;

/**
 * <p>
 * 订单表 服务类
 * </p>
 *
 * @author mzlhsc
 * @since 2026-11-09
 */
public interface IOrdersService extends IService<Orders> {

    /**
     * 订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    PageDTO<OrderVO> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);


    /**
     * 订单详情
     * @param id
     * @return
     */
    OrderVO details(Long id);


    /**
     * 接单
     * @param id
     */
    void confirm(Long id);

    /**
     * 完成订单（已接单直接完成，支付状态同步为已支付）
     * @param id
     */
    void complete(Long id);

    /**
     * 修改订单支付状态（管理端）
     * @param id 订单id
     * @param payStatus 0未支付 1已支付
     */
    void updatePayStatus(Long id, Integer payStatus);

    /**
     * 拒单
     * @param ordersRejectionDTO
     */
    void rejection(OrdersRejectionDTO ordersRejectionDTO);

    /**
     * 取消订单
     * @param ordersCancelDTO
     */
    void cancel(OrdersCancelDTO ordersCancelDTO);

    /**
     * 各个状态的订单数量统计
     * @return
     */
    OrderStatisticsVO statistics();

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);


    /**
     * 用户端订单分页查询（历史订单）
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    PageDTO<OrderVO> pageQuery4User(int page, int pageSize, Integer status);


    /**
     * 再来一单
     * @param id
     */
    void repetition(Long id);


    /**
     * 用户催单
     * @param id
     */
    void reminder(Long id);
}
