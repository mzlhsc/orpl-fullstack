package com.mzlhsc.orpl.order.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "堂食结账")
public class DiningCheckoutDTO implements Serializable {

    @NotNull
    @Schema(description = "会话id")
    private Long sessionId;

    @Schema(description = "支付方式 1微信 2支付宝 3现金")
    private Integer payMethod;

    @Schema(description = "是否强制结清未支付的AA账单")
    private Boolean settleUnpaid;

    @Schema(description = "备注")
    private String remark;
}
