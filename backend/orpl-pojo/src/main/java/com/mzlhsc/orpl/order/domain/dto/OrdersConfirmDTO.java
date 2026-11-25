package com.mzlhsc.orpl.order.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 接单入参（对应接口文档 Body：{"id": 订单id}）
 */
@Data
public class OrdersConfirmDTO implements Serializable {

    @Schema(description = "订单id")
    private Long id;

}
