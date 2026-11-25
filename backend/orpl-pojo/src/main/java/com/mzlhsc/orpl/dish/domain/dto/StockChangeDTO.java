package com.mzlhsc.orpl.dish.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "库存变更参数")
public class StockChangeDTO implements Serializable {

    @Schema(description = "菜品id")
    private Long dishId;

    @Schema(description = "数量，必须大于0")
    private Integer number;
}
