package com.mzlhsc.orpl.order.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class OrdersPageQueryDTO implements Serializable {

    @Schema(description = "下单开始时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime beginTime;

    @Schema(description = "下单结束时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @Schema(description = "页码")
    private int page;

    @Schema(description = "每页记录数")
    private int pageSize;

    @Schema(description = "订单号")
    private String number;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "订单状态 1待付款 2待接单 3已接单 5已完成 6已取消")
    private Integer status;

    @Schema(description = "下单用户id")
    private Long userId;

}
