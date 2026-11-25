package com.mzlhsc.orpl.order.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "堂食桌台新增/修改")
public class DiningTableDTO implements Serializable {

    @Schema(description = "桌台名称，如 A01")
    private String name;

    @Schema(description = "座位数")
    private Integer seats;

    @Schema(description = "区域")
    private String area;

    @Schema(description = "是否允许预约 1是 0否")
    private Integer reservationEnabled;
}
