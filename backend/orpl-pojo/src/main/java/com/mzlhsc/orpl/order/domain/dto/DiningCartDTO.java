package com.mzlhsc.orpl.order.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "堂食共享购物车加菜")
public class DiningCartDTO implements Serializable {

    @NotNull
    @Schema(description = "会话id")
    private Long sessionId;

    @NotNull
    @Schema(description = "菜品id")
    private Long dishId;

    @Schema(description = "口味")
    private String dishFlavor;

    @NotNull
    @Schema(description = "数量，正数加菜，负数减菜")
    private Integer number;
}
