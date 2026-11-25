package com.mzlhsc.orpl.order.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "AA 支付")
public class DiningAaPayDTO implements Serializable {

    @NotNull
    @Schema(description = "会话id")
    private Long sessionId;

    @Schema(description = "支付方式 1微信 2支付宝 3现金")
    private Integer payMethod;
}
