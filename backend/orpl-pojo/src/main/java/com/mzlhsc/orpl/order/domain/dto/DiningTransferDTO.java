package com.mzlhsc.orpl.order.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "转台/并桌")
public class DiningTransferDTO implements Serializable {

    @NotNull
    @Schema(description = "目标桌台id")
    private Long targetTableId;

    @Schema(description = "备注")
    private String remark;
}
