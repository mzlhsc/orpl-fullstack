package com.mzlhsc.orpl.order.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 管理端修改订单支付状态入参。
 */
@Data
@Schema(description = "订单支付状态")
public class OrdersPaymentStatusDTO implements Serializable {

    @Schema(description = "订单id")
    private Long id;

    @Schema(description = "支付状态 0未支付 1已支付")
    private Integer payStatus;
}
