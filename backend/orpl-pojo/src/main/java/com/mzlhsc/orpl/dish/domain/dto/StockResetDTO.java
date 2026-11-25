package com.mzlhsc.orpl.dish.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "库存重置参数")
public class StockResetDTO {
    private Long dishId;
    private Integer stock;
}
