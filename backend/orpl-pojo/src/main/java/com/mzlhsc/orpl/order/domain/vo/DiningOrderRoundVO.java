package com.mzlhsc.orpl.order.domain.vo;

import com.mzlhsc.orpl.order.domain.po.OrderDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "堂食当前订单的一次下单轮次")
public class DiningOrderRoundVO implements Serializable {

    @Schema(description = "展示轮次，从1开始")
    private Integer round;

    @Schema(description = "所属订单id")
    private Long orderId;

    @Schema(description = "所属订单号")
    private String orderNumber;

    @Schema(description = "下单时间")
    private LocalDateTime orderTime;

    @Schema(description = "本轮金额")
    private BigDecimal amount;

    @Schema(description = "本轮菜品明细")
    private List<OrderDetail> items = new ArrayList<>();
}
