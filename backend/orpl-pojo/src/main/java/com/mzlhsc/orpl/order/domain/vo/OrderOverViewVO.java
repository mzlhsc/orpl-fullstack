package com.mzlhsc.orpl.order.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 订单管理数据总览（工作台）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderOverViewVO implements Serializable {

    /** 全部订单 */
    private Integer allOrders;

    /** 已完成数量 */
    private Integer completedOrders;

    /** 已取消数量 */
    private Integer cancelledOrders;

    /** 待接单数量 */
    private Integer waitingOrders;

    /** 已接单数量（待处理） */
    private Integer deliveredOrders;
}

